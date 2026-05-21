# Google Pay Payments

> Process encrypted Google Pay payment tokens through the Global Payments GP-API, demonstrated in PHP, Java, and .NET.

## Critical Patterns

1. **The encrypted-mobile flow requires two SDK settings together: `mobileType = GOOGLE_PAY` and `TransactionModifier.ENCRYPTED_MOBILE`.** Setting only the modifier (without the mobile type) or only the type (without the modifier) silently sends the token down the wrong code path and the gateway rejects it with a generic decline. PHP sets both in `process-google-pay.php` (`$card->mobileType = EncyptedMobileType::GOOGLE_PAY` plus `->withModifier(TransactionModifier::ENCRYPTED_MOBILE)`); Java uses `MobilePaymentMethodType.GOOGLEPAY` (single word, no underscore) in `ProcessPaymentServlet.doPost()`; .NET uses `EncyptedMobileType.GOOGLE_PAY` in `ConfigurePaymentEndpoint()`. The enum name `EncyptedMobileType` is misspelled in the SDK — do not "fix" it.

2. **PHP exposes endpoints with `.php` extensions; Java and .NET use clean paths.** PHP runs under `php -S` with no router, so the live URLs are `/config.php` and `/process-google-pay.php` (the actual filenames). Java's `@WebServlet(urlPatterns = {"/process-google-pay", "/config"})` and .NET's `app.MapGet("/config", ...)` / `app.MapPost("/process-google-pay", ...)` use the clean form. Any frontend or test harness must branch on language, or PHP needs a rewrite rule added.

3. **Google Pay requires a real browser and a Google account to mint a token.** The `/process-google-pay` endpoint cannot be exercised end-to-end with curl alone — the encrypted token is produced client-side by the Google Pay payment sheet, which only renders in a browser with HTTPS (localhost is exempt) and a signed-in Google account. Use the bundled `index.html` to drive a real payment; curl-only testing is limited to `/config` and to malformed-input rejection paths.

4. **`docker-compose.yml` and root `package.json` are stale.** Compose declares `nodejs`, `python`, and `go` services, but those directories do not exist in this repo (only `php/`, `java/`, `dotnet/`). The root `package.json` is leftover starter-template scaffolding pointing at a non-existent `nodejs/server.js`. Use `./run.sh` per language; do not rely on `docker-compose up` until compose is fixed.

## Repository Structure

### PHP (built-in server + Global Payments SDK)
- [`php/process-google-pay.php`](php/process-google-pay.php) — single-file endpoint; `configureSdk()`, `sanitizeCurrency()`, `sanitizeAmount()`, and the inline charge call using `CreditCardData->charge()->withCurrency()->withModifier()->execute()`
- [`php/config.php`](php/config.php) — GET endpoint returning merchant and Google Pay config; defaults `countryCode`/`currencyCode` to `GB`/`GBP` when env vars are absent
- [`php/index.html`](php/index.html) — PHP-local copy of the Google Pay form
- [`php/composer.json`](php/composer.json) — `globalpayments/php-sdk` ^13.1, `vlucas/phpdotenv` ^5.5

### Java (Jakarta EE servlet + Global Payments SDK)
- [`java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java`](java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java) — single servlet handling both routes; `init()` configures `GpApiConfig`, `doGet()` serves `/config`, `doPost()` runs the Google Pay charge
- [`java/src/main/webapp/index.html`](java/src/main/webapp/index.html) — Java-local copy of the form (served by Tomcat)
- [`java/src/main/webapp/WEB-INF/web.xml`](java/src/main/webapp/WEB-INF/web.xml) — declares `index.html` as the welcome file
- [`java/pom.xml`](java/pom.xml) — `globalpayments-sdk` 14.2.20, Jakarta Servlet 5.0, runs via `cargo-maven3-plugin` on embedded Tomcat 10 at port 8000

### .NET (ASP.NET Core minimal API + Global Payments SDK)
- [`dotnet/Program.cs`](dotnet/Program.cs) — `Main()`, `ConfigureGlobalPaymentsSDK()`, `ConfigureEndpoints()` (registers `/config`), `ConfigurePaymentEndpoint()` (registers `/process-google-pay`)
- [`dotnet/wwwroot/`](dotnet/wwwroot/) — static file root served by `UseDefaultFiles()` / `UseStaticFiles()`
- [`dotnet/dotnet.csproj`](dotnet/dotnet.csproj) — `GlobalPayments.Api` 9.0.16, `DotEnv.Net` 3.2.1, net9.0

### Shared / repo root
- [`index.html`](index.html) — root copy of the payment form (each language also has its own copy; keep them in sync)
- [`README.md`](README.md) — public-facing readme
- [`docker-compose.yml`](docker-compose.yml) — stale: references nodejs/python/go services that do not exist
- [`package.json`](package.json) — stale starter-template leftover; not used by any implementation

## API Surface

| Method | Path (Java / .NET) | Path (PHP) | Purpose |
|--------|--------------------|------------|---------|
| GET | `/config` | `/config.php` | Returns merchant info + Google Pay button/environment config |
| POST | `/process-google-pay` | `/process-google-pay.php` | Charges an encrypted Google Pay token via GP-API |

All three implementations return the same JSON response shape. The path divergence is the only API-surface difference.

## Environment Variables

```bash
GP_API_APP_ID=your_app_id          # GP-API application ID
GP_API_APP_KEY=your_app_key        # GP-API application key
ENVIRONMENT=TEST                   # TEST or PRODUCTION (drives Environment.TEST/PRODUCTION in SDK)
MERCHANT_ID=gpapiqa1               # Surfaced via /config to the frontend
MERCHANT_NAME="Test Merchant"      # Surfaced via /config to the frontend
ENABLE_LOGGING=false               # PHP only: enables SampleRequestLogger when "true"

GOOGLE_PAY_MERCHANT_ID=12345678901234567890   # Google-issued merchant ID for the payment sheet
GOOGLE_PAY_COUNTRY_CODE=US                    # ISO 3166-1 alpha-2 (.env.sample default; PHP code defaults to GB if missing)
GOOGLE_PAY_CURRENCY_CODE=USD                  # ISO 4217 (.env.sample default; PHP code defaults to GBP if missing)
GOOGLE_PAY_BUTTON_COLOR=black                 # black or white
PORT=8000                                     # Optional; all three languages default to 8000
```

Each language directory has its own `.env.sample` — copy to `.env` and fill in credentials. The three samples are kept in sync; changes to one should apply to all.

## Test Cards

Use these GP-API sandbox cards when the Google Pay payment sheet asks for a card to tokenize. The actual auth path is gateway-mediated, so the card numbers below are what the gateway expects regardless of which test card Google Pay surfaces in the sheet.

| Brand | Number | CVV | Expiry |
|-------|--------|-----|--------|
| Visa | 4263970000005262 | 123 | Any future date |
| Mastercard | 5425230000004415 | 123 | Any future date |

For Google Pay's own client-side test tokens, see [Google Pay sample tokens](https://developers.google.com/pay/api/web/guides/resources/sample-tokens). Get sandbox GP-API credentials at [developer.globalpayments.com](https://developer.globalpayments.com).

## Architecture Summary

**Payment flow:** Browser loads `index.html` → Google Pay payment sheet returns an encrypted token → `POST /process-google-pay` with `{token, amount, currency}` → server sets `mobileType = GOOGLE_PAY` and `TransactionModifier.ENCRYPTED_MOBILE` on `CreditCardData`, calls `.charge().withCurrency().withModifier().execute()` → returns transaction ID and auth code

**Config flow:** Browser fetches `/config` (or `/config.php`) on load → server reads `MERCHANT_*` and `GOOGLE_PAY_*` env vars → frontend initializes the Google Pay button with the returned merchant info

## Security Notes

These demos have no authentication on either endpoint, ship sandbox `GP_API_APP_ID` / `GP_API_APP_KEY` placeholders directly in `.env.sample` for convenience, and log raw error messages to stdout. Production deployments need auth middleware on `/process-google-pay`, secrets management instead of `.env` files, HTTPS, and the redaction of SDK error details from client responses.

## How to Run

```bash
cd php && ./run.sh       # PHP — :8000 (php -S 0.0.0.0:8000)
cd java && ./run.sh      # Java — :8000 (mvn cargo:run, embedded Tomcat 10)
cd dotnet && ./run.sh    # .NET — :8000 (dotnet run)
```

`docker-compose up` is **not** a working path here — compose references nodejs/python/go services that do not exist in this repo. Use `./run.sh` per language.

Google Pay requires a real browser to render the payment sheet. After starting any server, open `http://localhost:8000/` in Chrome (with a signed-in Google account) to exercise the full flow.

## How to Verify

```bash
# Config endpoint — Java / .NET
curl http://localhost:8000/config
# Expected: {"success":true,"data":{"merchantInfo":{"merchantName":"Test Merchant","merchantId":"gpapiqa1"},"googlePayConfig":{"googleMerchantId":"12345678901234567890","environment":"TEST","countryCode":"US","currencyCode":"USD","buttonColor":"black"}}}

# Config endpoint — PHP
curl http://localhost:8000/config.php
# Expected: same shape as above

# Input-validation paths on /process-google-pay (PHP uses /process-google-pay.php)
curl -X POST http://localhost:8000/process-google-pay \
  -H "Content-Type: application/json" \
  -d '{}'
# Expected: 400 with {"success":false,"message":"Google Pay token is required",...}
```

The happy path on `/process-google-pay` cannot be tested with curl — the request body requires a freshly minted encrypted Google Pay token, which only the Google Pay payment sheet (in a browser, with a Google account) can produce. Drive the full flow through `index.html`.

## Making Changes

All three language implementations expose identical behavior (modulo the `.php` URL suffix in PHP). A change to one must be applied to all — each language in a separate commit. Three copies of `index.html` live in this repo (`index.html`, `php/index.html`, `java/src/main/webapp/index.html`); keep them in sync. `docker-compose.yml` and the root `package.json` are stale leftovers — do not add new implementations to compose without first cleaning the dead service definitions, and do not assume the root `package.json` is wired to anything.

This repo intentionally ships only PHP, Java, and .NET — Node.js, Python, and Go are not present. Do not add a new language implementation without explicit instruction.

## SDK Versions

- **PHP**: `globalpayments/php-sdk` ^13.1
- **Java**: `globalpayments-sdk` (com.heartlandpaymentsystems) 14.2.20
- **.NET**: `GlobalPayments.Api` 9.0.16
