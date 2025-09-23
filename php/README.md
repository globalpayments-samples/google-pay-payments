# PHP Google Pay Integration

This example demonstrates Google Pay payment processing using PHP and the Global Payments GP-API.

## Requirements

- PHP 7.4 or later
- Composer
- Global Payments GP-API account with Google Pay processing enabled
- Google Pay merchant account

## Project Structure

- `process-google-pay.php` - Google Pay payment processing script
- `config.php` - Configuration endpoint for Google Pay settings
- `index.html` - Client-side payment form with Google Pay integration
- `composer.json` - Project dependencies
- `.env.sample` - Template for environment variables
- `run.sh` - Convenience script to run the application

## Setup

1. Clone this repository
2. Copy `.env.sample` to `.env`
3. Update `.env` with your Global Payments GP-API credentials:
   ```bash
   GP_API_APP_ID=your_app_id_here
   GP_API_APP_KEY=your_app_key_here
   ENVIRONMENT=TEST
   MERCHANT_ID=your_merchant_id
   MERCHANT_NAME="Your Merchant Name"

   # Google Pay Configuration
   GOOGLE_PAY_MERCHANT_ID=12345678901234567890
   GOOGLE_PAY_COUNTRY_CODE=US
   GOOGLE_PAY_CURRENCY_CODE=USD
   GOOGLE_PAY_BUTTON_COLOR=black
   ```
4. Install dependencies:
   ```bash
   composer install
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   php -S localhost:8000
   ```

## Implementation Details

### Application Structure
The application uses a simple PHP structure:
- Static HTML form with Google Pay button integration
- Separate PHP scripts for configuration and payment processing
- Composer for dependency management
- Environment-based configuration

### GP-API SDK Configuration
Global Payments GP-API SDK configuration using environment variables:
- Loads credentials from .env file
- Sets up GP-API environment (TEST/PRODUCTION)
- Configures channel for card-not-present transactions

### Google Pay Processing
Google Pay payment processing flow:
1. Client loads Google Pay configuration from `/config`
2. User initiates Google Pay and receives encrypted token
3. Client submits Google Pay token to `/process-google-pay`
4. Server validates and processes encrypted mobile token
5. Returns success/error response with transaction details

### Error Handling
Implements comprehensive error handling:
- Validates Google Pay token format
- Catches and processes GP-API exceptions
- Returns structured JSON error responses
- Handles edge cases gracefully

## API Endpoints

### GET /config
Returns Google Pay configuration for client-side initialization.

Response:
```json
{
  "success": true,
  "data": {
    "merchantInfo": {
      "merchantName": "Your Merchant Name",
      "merchantId": "your_merchant_id"
    },
    "googlePayConfig": {
      "googleMerchantId": "12345678901234567890",
      "environment": "TEST",
      "countryCode": "US",
      "currencyCode": "USD",
      "buttonColor": "black"
    }
  }
}
```

### POST /process-google-pay
Processes Google Pay payments using encrypted mobile tokens.

Request Parameters:
- `token` (string, required) - Encrypted Google Pay token
- `amount` (string, required) - Payment amount
- `currency` (string, optional) - Currency code (USD, EUR, GBP)

Request Example:
```json
{
  "token": "{encrypted_google_pay_token}",
  "amount": "10.00",
  "currency": "USD"
}
```

Response (Success):
```json
{
  "success": true,
  "message": "Payment successful! Transaction ID: TXN_123456",
  "data": {
    "transactionId": "TXN_123456",
    "amount": "10.00",
    "currency": "USD",
    "status": "SUCCESS",
    "responseCode": "00",
    "authCode": "AUTH123",
    "timestamp": "2023-01-01T12:00:00+00:00"
  }
}
```

Response (Error):
```json
{
  "success": false,
  "message": "Payment processing failed",
  "error": {
    "code": "API_ERROR",
    "details": "Detailed error message"
  }
}
```

## Security Considerations

This example demonstrates basic implementation. For production use, consider:
- Implementing additional input validation
- Adding request rate limiting
- Including security headers
- Implementing proper logging
- Adding payment fraud prevention measures
- Using HTTPS in production
- Implementing CSRF protection
- Configuring proper session handling
- Setting appropriate PHP security directives
