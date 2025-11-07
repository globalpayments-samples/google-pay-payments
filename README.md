# Google Pay Payments Integration

This project demonstrates Google Pay payment processing integration with the Global Payments GP-API across multiple programming languages. Each implementation shows how to handle encrypted mobile payment tokens from Google Pay and process them securely through the GP-API.

## Available Implementations

- [.NET Core](./dotnet/) - ([Preview](https://githubbox.com/globalpayments-samples/google-pay-payments/tree/main/dotnet)) - ASP.NET Core web application
- [Java](./java/) - ([Preview](https://githubbox.com/globalpayments-samples/google-pay-payments/tree/main/java)) - Jakarta EE servlet-based web application
- [PHP](./php/) - ([Preview](https://githubbox.com/globalpayments-samples/google-pay-payments/tree/main/php)) - PHP web application

## Google Pay Features

- **Google Pay Token Processing** - Handle encrypted mobile payment tokens
- **GP-API Integration** - Secure processing through Global Payments GP-API
- **Comprehensive Validation** - Input validation and error handling
- **Production Ready** - Complete implementation with security best practices

## Implementation Features

Each implementation includes:

1. **Google Pay Configuration**
   - Merchant configuration endpoint
   - Google Pay button settings
   - Environment-specific configuration
   - Currency and country code management

2. **Payment Processing**
   - GET `/config` - Google Pay merchant configuration
   - POST `/process-google-pay` - Google Pay token processing
   - Encrypted mobile token handling
   - Multi-currency transaction support

3. **Security & Validation**
   - Token format validation
   - Amount and currency validation
   - Comprehensive error handling
   - Transaction status verification

## Quick Start

1. **Choose your language** - Navigate to any implementation directory (dotnet, java, php)
2. **Set up credentials** - Copy `.env.sample` to `.env` and add your Global Payments GP-API credentials
3. **Configure Google Pay** - Update Google Pay merchant settings in `.env` file
4. **Run the server** - Execute `./run.sh` to install dependencies and start the server
5. **Test payments** - Use the web interface to test Google Pay transactions

## Google Pay Integration Requirements

To use this integration, you'll need:

- **Global Payments GP-API Account** - With Google Pay processing enabled
- **Google Pay Merchant Account** - Registered with Google Pay
- **SSL Certificate** - Required for Google Pay in production
- **Development Environment** - For your chosen language (.NET, Java, or PHP)

## Prerequisites

- Global Payments GP-API account with credentials:
  - `GP_API_APP_ID` - Your application ID
  - `GP_API_APP_KEY` - Your application key
- Google Pay merchant configuration:
  - Google Merchant ID for Google Pay
  - Merchant name and display settings
- Development environment for your chosen language
- Package manager (dotnet, maven, composer)

## Configuration Guide

### Environment Variables

Each implementation uses the following environment variables:

```bash
# Global Payments GP-API Configuration
GP_API_APP_ID=your_app_id_here
GP_API_APP_KEY=your_app_key_here
ENVIRONMENT=TEST  # or PRODUCTION
MERCHANT_ID=your_merchant_id
MERCHANT_NAME="Your Merchant Name"

# Google Pay Configuration
GOOGLE_PAY_MERCHANT_ID=12345678901234567890
GOOGLE_PAY_COUNTRY_CODE=US
GOOGLE_PAY_CURRENCY_CODE=USD
GOOGLE_PAY_BUTTON_COLOR=black
```

### Google Pay Setup

1. Register for a Google Pay merchant account
2. Configure your merchant settings in the Google Pay console
3. Add your domain to the allowed origins list
4. Update the environment variables with your merchant information

### Testing

- Use Google Pay test cards for development
- Ensure your development environment uses HTTPS for Google Pay
- Test with multiple currencies if supporting international payments

## API Endpoints

### GET /config
Returns Google Pay configuration for client-side initialization:

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
Processes Google Pay payment tokens:

**Request:**
```json
{
  "token": "{google_pay_encrypted_token}",
  "amount": "10.00",
  "currency": "USD"
}
```

**Success Response:**
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

## Security Considerations

This implementation includes production-ready security features:
- Input validation and sanitization
- Encrypted token processing
- Comprehensive error handling
- Transaction verification
- Secure credential management
- Rate limiting recommendations
- HTTPS enforcement for production
