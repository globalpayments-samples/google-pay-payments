using GlobalPayments.Api;
using GlobalPayments.Api.Entities;
using GlobalPayments.Api.Entities.Enums;
using GlobalPayments.Api.PaymentMethods;
using GlobalPayments.Api.ServiceConfigs;
using dotenv.net;
using System.Text.Json;

namespace GooglePayPaymentSample;

/// <summary>
/// Google Pay Payment Processing Application
///
/// This application demonstrates Google Pay payment processing using the Global Payments GP-API.
/// It handles encrypted mobile payment tokens from Google Pay and processes them
/// securely through the GP-API.
/// </summary>
public class Program
{
    public static void Main(string[] args)
    {
        // Load environment variables from .env file
        DotEnv.Load();

        var builder = WebApplication.CreateBuilder(args);
        
        var app = builder.Build();

        // Configure static file serving for the payment form
        app.UseDefaultFiles();
        app.UseStaticFiles();
        
        // Configure the SDK on startup
        ConfigureGlobalPaymentsSDK();

        ConfigureEndpoints(app);
        
        var port = System.Environment.GetEnvironmentVariable("PORT") ?? "8000";
        app.Urls.Add($"http://0.0.0.0:{port}");
        
        app.Run();
    }

    /// <summary>
    /// Configures the Global Payments GP-API SDK with necessary credentials and settings.
    /// This must be called before processing any payments.
    /// </summary>
    private static void ConfigureGlobalPaymentsSDK()
    {
        var environment = System.Environment.GetEnvironmentVariable("ENVIRONMENT");

        ServicesContainer.ConfigureService(new GpApiConfig
        {
            AppId = System.Environment.GetEnvironmentVariable("GP_API_APP_ID"),
            AppKey = System.Environment.GetEnvironmentVariable("GP_API_APP_KEY"),
            Environment = "PRODUCTION".Equals(environment) ? GlobalPayments.Api.Entities.Environment.PRODUCTION : GlobalPayments.Api.Entities.Environment.TEST,
            Channel = Channel.CardNotPresent
        });
    }

    /// <summary>
    /// Configures the application's HTTP endpoints for payment processing.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigureEndpoints(WebApplication app)
    {
        // Configure HTTP endpoints
        app.MapGet("/config", () => {
            var merchantName = System.Environment.GetEnvironmentVariable("MERCHANT_NAME") ?? "Test Merchant";
            var merchantId = System.Environment.GetEnvironmentVariable("MERCHANT_ID");
            var googleMerchantId = System.Environment.GetEnvironmentVariable("GOOGLE_PAY_MERCHANT_ID") ?? "12345678901234567890";
            var environment = System.Environment.GetEnvironmentVariable("ENVIRONMENT") ?? "TEST";
            var countryCode = System.Environment.GetEnvironmentVariable("GOOGLE_PAY_COUNTRY_CODE") ?? "US";
            var currencyCode = System.Environment.GetEnvironmentVariable("GOOGLE_PAY_CURRENCY_CODE") ?? "USD";
            var buttonColor = System.Environment.GetEnvironmentVariable("GOOGLE_PAY_BUTTON_COLOR") ?? "black";

            return Results.Ok(new
            {
                success = true,
                data = new {
                    merchantInfo = new {
                        merchantName = merchantName,
                        merchantId = merchantId
                    },
                    googlePayConfig = new {
                        googleMerchantId = googleMerchantId,
                        environment = "PRODUCTION".Equals(environment) ? "PRODUCTION" : "TEST",
                        countryCode = countryCode,
                        currencyCode = currencyCode,
                        buttonColor = buttonColor
                    }
                }
            });
        });

        ConfigurePaymentEndpoint(app);
    }

    /// <summary>
    /// Sanitizes postal code input by removing invalid characters.
    /// </summary>
    /// <param name="postalCode">The postal code to sanitize. Can be null.</param>
    /// <returns>
    /// A sanitized postal code containing only alphanumeric characters and hyphens,
    /// limited to 10 characters. Returns empty string if input is null or empty.
    /// </returns>
    private static string SanitizePostalCode(string postalCode)
    {
        if (string.IsNullOrEmpty(postalCode)) return string.Empty;
        
        // Remove any characters that aren't alphanumeric or hyphen
        var sanitized = new string(postalCode.Where(c => char.IsLetterOrDigit(c) || c == '-').ToArray());
        
        // Limit length to 10 characters
        return sanitized.Length > 10 ? sanitized[..10] : sanitized;
    }

    /// <summary>
    /// Configures the Google Pay payment processing endpoint.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigurePaymentEndpoint(WebApplication app)
    {
        app.MapPost("/process-google-pay", async (HttpContext context) =>
        {
            try
            {
                // Read and parse JSON request body
                using var reader = new StreamReader(context.Request.Body);
                var requestBody = await reader.ReadToEndAsync();

                using var jsonDoc = JsonDocument.Parse(requestBody);
                var root = jsonDoc.RootElement;

                // Validate required parameters
                if (!root.TryGetProperty("token", out var tokenElement))
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Google Pay token is required",
                        error = new {
                            code = "MISSING_TOKEN",
                            details = "Google Pay token is required for processing"
                        }
                    });
                }

                if (!root.TryGetProperty("amount", out var amountElement))
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Amount is required",
                        error = new {
                            code = "MISSING_AMOUNT",
                            details = "Amount is required for processing"
                        }
                    });
                }

                // Extract and validate Google Pay token
                var googlePayToken = tokenElement.GetString();
                if (string.IsNullOrEmpty(googlePayToken))
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Invalid Google Pay token",
                        error = new {
                            code = "INVALID_TOKEN",
                            details = "Google Pay token cannot be empty"
                        }
                    });
                }

                // Validate and parse amount
                if (!decimal.TryParse(amountElement.GetString(), out var amount) || amount <= 0)
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Invalid amount",
                        error = new {
                            code = "INVALID_AMOUNT",
                            details = "Amount must be a positive number"
                        }
                    });
                }

                if (amount > 999999.99m)
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Amount exceeds maximum limit",
                        error = new {
                            code = "AMOUNT_TOO_LARGE",
                            details = "Amount exceeds maximum limit of 999999.99"
                        }
                    });
                }

                // Validate currency
                var currency = "USD";
                if (root.TryGetProperty("currency", out var currencyElement))
                {
                    currency = currencyElement.GetString()?.ToUpper() ?? "USD";
                }

                string[] allowedCurrencies = { "USD", "EUR", "GBP" };
                if (!allowedCurrencies.Contains(currency))
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Invalid currency",
                        error = new {
                            code = "INVALID_CURRENCY",
                            details = "Currency must be one of: USD, EUR, GBP"
                        }
                    });
                }

                // Validate Google Pay token format
                try
                {
                    JsonDocument.Parse(googlePayToken);
                }
                catch (JsonException)
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Invalid Google Pay token format",
                        error = new {
                            code = "INVALID_TOKEN_FORMAT",
                            details = "Google Pay token must be valid JSON"
                        }
                    });
                }

                // Initialize credit card with Google Pay token
                var card = new CreditCardData
                {
                    Token = googlePayToken,
                    MobileType = EncyptedMobileType.GOOGLE_PAY
                };

                // Process the payment transaction
                var transaction = card.Charge(amount)
                    .WithCurrency(currency)
                    .WithModifier(TransactionModifier.EncryptedMobile)
                    .Execute();

                // Verify transaction was successful
                if (transaction.ResponseCode != "00" && transaction.ResponseCode != "SUCCESS")
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Payment was declined",
                        error = new {
                            code = "PAYMENT_DECLINED",
                            details = transaction.ResponseMessage ?? "Payment declined by processor"
                        }
                    });
                }

                // Return successful response
                return Results.Ok(new
                {
                    success = true,
                    message = $"Payment successful! Transaction ID: {transaction.TransactionId}",
                    data = new {
                        transactionId = transaction.TransactionId,
                        amount = amount.ToString("F2"),
                        currency = currency,
                        status = transaction.ResponseMessage ?? "SUCCESS",
                        responseCode = transaction.ResponseCode,
                        authCode = transaction.AuthorizationCode,
                        timestamp = DateTime.UtcNow.ToString("o")
                    }
                });
            }
            catch (ApiException ex)
            {
                // Handle payment processing errors
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "API_ERROR",
                        details = ex.Message
                    }
                });
            }
            catch (Exception ex)
            {
                // Handle unexpected errors
                Console.WriteLine($"Google Pay processing error: {ex.Message}");
                return Results.Problem("An unexpected error occurred"
                    // new
                    // {
                    //     success = false,
                    //     message = "An unexpected error occurred",
                    //     error = new {
                    //         code = "SYSTEM_ERROR",
                    //         details = "Please try again later"
                    //     }
                    // }
                );
            }
        });
    }
}
