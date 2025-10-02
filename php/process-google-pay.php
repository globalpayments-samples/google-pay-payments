<?php

declare(strict_types=1);

/**
 * Google Pay Payment Processing Script
 *
 * This script demonstrates Google Pay payment processing using the Global Payments GP-API.
 * It handles encrypted mobile payment tokens from Google Pay and processes them
 * securely through the GP-API.
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   GlobalPayments_Sample
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */

require_once 'vendor/autoload.php';

use Dotenv\Dotenv;
use GlobalPayments\Api\Entities\Enums\EncyptedMobileType;
use GlobalPayments\Api\Entities\Enums\Environment;
use GlobalPayments\Api\Entities\Enums\Channel;
use GlobalPayments\Api\Entities\Enums\TransactionModifier;
use GlobalPayments\Api\Entities\Exceptions\ApiException;
use GlobalPayments\Api\PaymentMethods\CreditCardData;
use GlobalPayments\Api\ServiceConfigs\Gateways\GpApiConfig;
use GlobalPayments\Api\ServicesContainer;
use GlobalPayments\Api\Entities\Enums\PaymentDataSourceType;
use GlobalPayments\Api\Utils\Logging\Logger;
use GlobalPayments\Api\Utils\Logging\SampleRequestLogger;

ini_set('display_errors', '0');

/**
 * Configure the GP-API SDK
 *
 * Sets up the Global Payments GP-API SDK with necessary credentials and settings
 * loaded from environment variables.
 *
 * @return void
 */
function configureSdk(): void
{
    $dotenv = Dotenv::createImmutable(__DIR__);
    $dotenv->load();

    $config = new GpApiConfig();
    $config->appId = $_ENV['GP_API_APP_ID'];
    $config->appKey = $_ENV['GP_API_APP_KEY'];
    $config->environment = $_ENV['ENVIRONMENT'] === 'PRODUCTION' ? Environment::PRODUCTION : Environment::TEST;
    $config->channel = Channel::CardNotPresent;
    
    // Add logging if enabled
    if (isset($_ENV['ENABLE_LOGGING']) && $_ENV['ENABLE_LOGGING'] === 'true') {
        $config->requestLogger = new SampleRequestLogger(new Logger("logs"));
    }

    ServicesContainer::configureService($config);
}

/**
 * Sanitize and validate currency code
 *
 * @param string|null $currency The currency code to validate
 *
 * @return string Valid currency code (defaults to GBP)
 */
function sanitizeCurrency(?string $currency): string
{
    $allowedCurrencies = ['USD', 'EUR', 'GBP'];
    $currency = strtoupper($currency ?? 'GBP');
    
    return in_array($currency, $allowedCurrencies, true) ? $currency : 'GBP';
}

/**
 * Validate and sanitize amount
 *
 * @param string|float|null $amount The amount to validate
 *
 * @return string Valid amount string
 * @throws ApiException If amount is invalid
 */
function sanitizeAmount($amount): string
{
    if ($amount === null) {
        throw new ApiException('Amount is required');
    }

    $amount = floatval($amount);
    
    if ($amount <= 0) {
        throw new ApiException('Amount must be greater than 0');
    }
    
    if ($amount > 999999.99) {
        throw new ApiException('Amount exceeds maximum limit');
    }

    return number_format($amount, 2, '.', '');
}

// Set response content type
header('Content-Type: application/json');

// Initialize SDK configuration
try {
    configureSdk();
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Configuration error',
        'error' => [
            'code' => 'CONFIG_ERROR',
            'details' => 'Unable to initialize payment processor'
        ]
    ]);
    exit;
}

try {
    $requestData = json_decode(file_get_contents('php://input'));

    // Validate required POST parameters
    if (!isset($requestData->token)) {
        throw new ApiException('Google Pay token is required');
    }

    if (!isset($requestData->amount)) {
        throw new ApiException('Amount is required');
    }

    // Process and validate input parameters
    /**
     * The Google Pay token. If you have any input filtering on $_POST, you will need
     * to use htmlspecialchars_decode, otherwise this will not be a valid JSON anymore
     * and the API will throw an error
     */
    $googlePayToken = htmlspecialchars_decode($requestData->token);

    /**
     * Replace the '\\' with '\' so the encoded characters won't be decoded.
     * If your code adds extra backslashes ('\') the the string, you will need to manipulate it
     * in order to look like the one from test/Integration/Gateways/GpApiConnector/GpApiDigitalWalletTest.php
     */
    $googlePayToken = str_replace('\\\\', '\\', $googlePayToken);

    $amount = sanitizeAmount($requestData->amount);
    $currency = sanitizeCurrency($requestData->currency ?? 'USD');

    // Validate Google Pay token format
    $tokenData = json_decode($googlePayToken, true);
    $tokenData = $googlePayToken;
    if (json_last_error() !== JSON_ERROR_NONE) {
        throw new ApiException('Invalid Google Pay token format');
    }

    // Initialize credit card with Google Pay token
    $card = new CreditCardData();
    $card->token = $googlePayToken;
    $card->mobileType = EncyptedMobileType::GOOGLE_PAY;

    // Process the payment transaction
    $transaction = $card->charge($amount)
        ->withCurrency($currency)
        ->withModifier(TransactionModifier::ENCRYPTED_MOBILE)
        ->execute();

    // Verify transaction was successful
    if ($transaction->responseCode !== '00' && $transaction->responseCode !== 'SUCCESS') {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Payment was declined',
            'error' => [
                'code' => 'PAYMENT_DECLINED',
                'details' => $transaction->responseMessage ?? 'Payment declined by processor'
            ]
        ]);
        exit;
    }

    // Return successful response
    echo json_encode([
        'success' => true,
        'message' => sprintf(
            'Payment successful! Transaction ID: %s',
            $transaction->transactionId
        ),
        'data' => [
            'transactionId' => $transaction->transactionId,
            'amount' => $amount,
            'currency' => $currency,
            'status' => $transaction->responseMessage ?? 'SUCCESS',
            'responseCode' => $transaction->responseCode,
            'authCode' => $transaction->authorizationCode ?? null,
            'timestamp' => date('c')
        ]
    ]);

} catch (ApiException $e) {
    // Handle payment processing errors
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Payment processing failed',
        'error' => [
            'code' => 'API_ERROR',
            'details' => $e->getMessage()
        ]
    ]);
} catch (Exception $e) {
    // Handle unexpected errors
    error_log('Google Pay processing error: ' . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'An unexpected error occurred',
        'error' => [
            'code' => 'SYSTEM_ERROR',
            'details' => 'Please try again later'
        ]
    ]);
}