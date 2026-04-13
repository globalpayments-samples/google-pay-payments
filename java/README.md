# Java Google Pay Integration

This example demonstrates Google Pay payment processing using Jakarta EE and the Global Payments GP-API.

## Requirements

- Java 11 or later
- Maven
- Global Payments GP-API account with Google Pay processing enabled
- Google Pay merchant account

## Project Structure

- `src/main/java/com/globalpayments/example/ProcessPaymentServlet.java` - Main servlet handling Google Pay processing
- `src/main/webapp/index.html` - Client-side payment form with Google Pay integration
- `src/main/webapp/WEB-INF/web.xml` - Web application configuration
- `.env.sample` - Template for environment variables
- `pom.xml` - Project dependencies and build configuration
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
   mvn clean install
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   mvn jetty:run
   ```

## Implementation Details

### Servlet Configuration
The application uses Jakarta EE servlets to:
- Handle Google Pay configuration requests
- Process Google Pay payment tokens
- Serve static content and payment forms

### GP-API SDK Configuration
Global Payments GP-API SDK configuration is handled in the servlet's init method:
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
- Returns appropriate HTTP status codes and JSON responses
- Provides meaningful error messages

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
    "timestamp": "2023-01-01T12:00:00Z"
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
- Configuring secure session management

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `mvn` command not found | Install Maven 3.6+. Run `mvn -v` to check version |
| Build fails | Ensure Java 11+ is installed. Run `java -version` to check |
| Port already in use | Stop other services on port 8000, or modify `pom.xml` cargo config |
| `.env` not loading | Verify `.env` file exists in the language directory (not project root) |

---

## Resources

- [Parent Project README](../README.md)
- [Global Payments Developer Portal](https://developer.globalpayments.com/)
- [API Reference](https://developer.globalpayments.com/api/references-overview)
- [Java SDK](https://github.com/globalpayments/java-sdk)
- [Test Cards](https://developer.globalpayments.com/resources/test-cards)
