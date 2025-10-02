package com.globalpayments.example;

import com.global.api.ServicesContainer;
import com.global.api.entities.Transaction;
import com.global.api.entities.enums.Channel;
import com.global.api.entities.enums.MobilePaymentMethodType;
import com.global.api.entities.enums.Environment;
import com.global.api.entities.enums.TransactionModifier;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.ConfigurationException;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.serviceConfigs.GpApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Google Pay Payment Processing Servlet
 *
 * This servlet demonstrates Google Pay payment processing using the Global Payments GP-API.
 * It handles encrypted mobile payment tokens from Google Pay and processes them
 * securely through the GP-API.
 *
 * Endpoints:
 * - GET /config: Returns merchant configuration for Google Pay
 * - POST /process-google-pay: Processes Google Pay payments using encrypted tokens
 *
 * @author Global Payments
 * @version 1.0
 */

@WebServlet(urlPatterns = {"/process-google-pay", "/config"})
public class ProcessPaymentServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private final Dotenv dotenv = Dotenv.load();
    
    /**
     * Initializes the servlet and configures the Global Payments GP-API SDK.
     * This must be called before processing any payments.
     *
     * @throws ServletException if there's an error initializing the servlet
     */
    @Override
    public void init() throws ServletException {
        try {
            // Configure the Global Payments GP-API SDK with credentials and settings
            GpApiConfig config = new GpApiConfig();
            config.setAppId(dotenv.get("GP_API_APP_ID"));
            config.setAppKey(dotenv.get("GP_API_APP_KEY"));

            String environment = dotenv.get("ENVIRONMENT");
            config.setEnvironment("PRODUCTION".equals(environment) ? Environment.PRODUCTION : Environment.TEST);
            config.setChannel(Channel.CardNotPresent);

            ServicesContainer.configureService(config);
        } catch (ConfigurationException e) {
            // Log configuration errors and propagate as ServletException
            throw new ServletException("Failed to configure Global Payments GP-API SDK", e);
        }
    }

    /**
     * Handles GET requests to /config endpoint.
     * Returns merchant configuration for Google Pay.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getServletPath().equals("/config")) {
            response.setContentType("application/json");

            String merchantName = dotenv.get("MERCHANT_NAME", "Test Merchant");
            String merchantId = dotenv.get("MERCHANT_ID");
            String googleMerchantId = dotenv.get("GOOGLE_PAY_MERCHANT_ID", "12345678901234567890");
            String environment = dotenv.get("ENVIRONMENT", "TEST");
            String countryCode = dotenv.get("GOOGLE_PAY_COUNTRY_CODE", "US");
            String currencyCode = dotenv.get("GOOGLE_PAY_CURRENCY_CODE", "USD");
            String buttonColor = dotenv.get("GOOGLE_PAY_BUTTON_COLOR", "black");

            String jsonResponse = String.format(
                "{\"success\":true,\"data\":{\"merchantInfo\":{\"merchantName\":\"%s\",\"merchantId\":%s},\"googlePayConfig\":{\"googleMerchantId\":\"%s\",\"environment\":\"%s\",\"countryCode\":\"%s\",\"currencyCode\":\"%s\",\"buttonColor\":\"%s\"}}}",
                merchantName,
                merchantId != null ? "\"" + merchantId + "\"" : "null",
                googleMerchantId,
                "PRODUCTION".equals(environment) ? "PRODUCTION" : "TEST",
                countryCode,
                currencyCode,
                buttonColor
            );
            response.getWriter().write(jsonResponse);
        }
    }

    /**
     * Handles POST requests to /process-google-pay endpoint.
     * Processes Google Pay payments using encrypted mobile tokens.
     *
     * @param request The HTTP request containing payment details
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        try {
            // Read JSON request body
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                requestBody.append(line);
            }

            // Parse JSON request
            ObjectMapper mapper = new ObjectMapper();
            JsonNode requestJson = mapper.readTree(requestBody.toString());

            // Validate required parameters
            if (!requestJson.has("token")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Google Pay token is required\",\"error\":{\"code\":\"MISSING_TOKEN\",\"details\":\"Google Pay token is required for processing\"}}"
                );
                return;
            }

            if (!requestJson.has("amount")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Amount is required\",\"error\":{\"code\":\"MISSING_AMOUNT\",\"details\":\"Amount is required for processing\"}}"
                );
                return;
            }

            // Extract and validate Google Pay token
            String googlePayToken = requestJson.get("token").asText();

            // Validate and parse amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(requestJson.get("amount").asText());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(
                        "{\"success\":false,\"message\":\"Invalid amount\",\"error\":{\"code\":\"INVALID_AMOUNT\",\"details\":\"Amount must be a positive number\"}}"
                    );
                    return;
                }
                if (amount.compareTo(new BigDecimal("999999.99")) > 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(
                        "{\"success\":false,\"message\":\"Amount exceeds maximum limit\",\"error\":{\"code\":\"AMOUNT_TOO_LARGE\",\"details\":\"Amount exceeds maximum limit of 999999.99\"}}"
                    );
                    return;
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid amount format\",\"error\":{\"code\":\"INVALID_AMOUNT\",\"details\":\"Amount must be a valid number\"}}"
                );
                return;
            }

            // Validate currency
            String currency = requestJson.has("currency") ? requestJson.get("currency").asText().toUpperCase() : "USD";
            if (!"USD".equals(currency) && !"EUR".equals(currency) && !"GBP".equals(currency)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid currency\",\"error\":{\"code\":\"INVALID_CURRENCY\",\"details\":\"Currency must be one of: USD, EUR, GBP\"}}"
                );
                return;
            }

            // Validate Google Pay token format
            try {
                mapper.readTree(googlePayToken);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid Google Pay token format\",\"error\":{\"code\":\"INVALID_TOKEN_FORMAT\",\"details\":\"Google Pay token must be valid JSON\"}}"
                );
                return;
            }

            // Initialize credit card with Google Pay token
            CreditCardData card = new CreditCardData();
            card.setToken(googlePayToken);
            card.setMobileType(MobilePaymentMethodType.GOOGLEPAY);

            // Process the payment transaction
            Transaction transaction = card.charge(amount)
                    .withCurrency(currency)
                    .withModifier(TransactionModifier.EncryptedMobile)
                    .execute();

            // Verify transaction was successful
            if (!"00".equals(transaction.getResponseCode()) && !"SUCCESS".equals(transaction.getResponseCode())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String errorResponse = String.format(
                    "{\"success\":false,\"message\":\"Payment was declined\",\"error\":{\"code\":\"PAYMENT_DECLINED\",\"details\":\"%s\"}}",
                    transaction.getResponseMessage() != null ? transaction.getResponseMessage() : "Payment declined by processor"
                );
                response.getWriter().write(errorResponse);
                return;
            }

            // Return successful response
            String timestamp = java.time.Instant.now().toString();
            String successResponse = String.format(
                "{\"success\":true,\"message\":\"Payment successful! Transaction ID: %s\",\"data\":{\"transactionId\":\"%s\",\"amount\":\"%s\",\"currency\":\"%s\",\"status\":\"%s\",\"responseCode\":\"%s\",\"authCode\":%s,\"timestamp\":\"%s\"}}",
                transaction.getTransactionId(),
                transaction.getTransactionId(),
                amount.toPlainString(),
                currency,
                transaction.getResponseMessage() != null ? transaction.getResponseMessage() : "SUCCESS",
                transaction.getResponseCode(),
                transaction.getAuthorizationCode() != null ? "\"" + transaction.getAuthorizationCode() + "\"" : "null",
                timestamp
            );
            response.getWriter().write(successResponse);

        } catch (ApiException e) {
            // Handle payment processing errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String errorResponse = String.format(
                "{\"success\":false,\"message\":\"Payment processing failed\",\"error\":{\"code\":\"API_ERROR\",\"details\":\"%s\"}}",
                e.getMessage()
            );
            response.getWriter().write(errorResponse);
        } catch (Exception e) {
            // Handle unexpected errors
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                "{\"success\":false,\"message\":\"An unexpected error occurred\",\"error\":{\"code\":\"SYSTEM_ERROR\",\"details\":\"Please try again later\"}}"
            );
        }
    }
}
