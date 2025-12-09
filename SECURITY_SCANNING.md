# Secret Scanning Setup

This repository uses [Gitleaks](https://github.com/gitleaks/gitleaks) to prevent secrets from being committed.

## How It Works

A pre-commit hook automatically scans your staged changes for:
- API keys (Firebase, GCP, Gemini, etc.)
- Private keys and certificates
- Service account credentials
- Tokens and passwords
- Any high-entropy strings that look like secrets

## What Happens When You Commit

```bash
git commit -m "your message"
```

1. The pre-commit hook runs automatically
2. Gitleaks scans your staged files
3. If secrets are found, the commit is blocked
4. If no secrets are found, the commit proceeds

## If a Secret is Detected

```
❌ Secret scanning failed!
🔒 Potential secrets detected in your commit.
```

**What to do:**

1. **Remove the secret** from your code (recommended)
2. **Use environment variables** instead
3. **Add to `.gitleaks.toml` allowlist** if it's a false positive
4. **Use `--no-verify`** to bypass (NOT RECOMMENDED)

## Configuration

The `.gitleaks.toml` file contains:
- Custom rules for Firebase, GCP, and other secrets
- Allowlist for false positives
- Entropy thresholds for detection

## Manual Scan

Scan the entire repository:
```bash
gitleaks detect --no-git
```

Scan staged files only:
```bash
gitleaks protect --staged
```

## Best Practices

✅ **DO:**
- Use environment variables for secrets
- Store secrets in GCP Secret Manager
- Use `.env.example` for documentation
- Keep `.env` in `.gitignore`

❌ **DON'T:**
- Commit `.env` files
- Hardcode API keys in code
- Use `--no-verify` unless absolutely necessary
- Store credentials in version control

## Need Help?

See the [Gitleaks documentation](https://github.com/gitleaks/gitleaks) for more information.

---

# OWASP ZAP Security Scanning

This repository includes automated security scanning using [OWASP ZAP](https://www.zaproxy.org/) (Zed Attack Proxy) in the CI/CD pipeline.

## How It Works

OWASP ZAP runs automatically on every **main branch** deployment:

1. Application is deployed to Cloud Run
2. ZAP baseline scan runs against the deployed service
3. Security reports are generated and uploaded to Cloud Storage
4. HTML, Markdown, and JSON reports are available for review

## What ZAP Scans For

The baseline scan performs passive security checks including:

- **Cross-Site Scripting (XSS)** vulnerabilities
- **SQL Injection** potential
- **Insecure Headers** (missing security headers)
- **Cookie Security** issues
- **Information Disclosure** risks
- **Outdated Components** with known vulnerabilities

## Viewing Scan Reports

After each main branch build, reports are uploaded to:

```
gs://${PROJECT_ID}-security-reports/recipe-storage-service/zap/${TIMESTAMP}/
```

Three report formats are available:
- **HTML**: `zap-report.html` - Visual report (publicly accessible)
- **Markdown**: `zap-report.md` - Text-based summary
- **JSON**: `zap-report.json` - Machine-readable format

### Access Reports

Check the Cloud Build logs for the direct link to the HTML report, or access via:

```bash
# List recent scans
gsutil ls gs://${PROJECT_ID}-security-reports/recipe-storage-service/zap/

# Download a report by replacing TIMESTAMP with a real value from the `gsutil ls` command above.
gsutil cp gs://${PROJECT_ID}-security-reports/recipe-storage-service/zap/TIMESTAMP/zap-report.html .
```

## Scan Configuration

The ZAP baseline scan is configured with:
- **Target**: Full service URL (root endpoint)
- **Mode**: Passive scan only (safe for production)
- **Timeout**: 5 minutes maximum
- **Exit Strategy**: Fails build on HIGH risk findings

## Understanding Results

ZAP findings are categorized by risk level:

| Risk Level | Severity | Action Required |
|------------|----------|-----------------|
| 🔴 High | Critical | Fix immediately |
| 🟠 Medium | Important | Address in next sprint |
| 🟡 Low | Minor | Consider fixing |
| ℹ️ Informational | FYI | Review for best practices |

## Running ZAP Locally

Test security scanning locally before pushing:

```bash
# Using Docker
docker run -t ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://localhost:8080 \
  -r zap-report.html \
  -l HIGH -I

# View the report
open zap-report.html
```

## CI/CD Integration

ZAP scanning is integrated into `cloudbuild.yaml`:

- **Step**: `security-scan-zap`
- **Runs On**: Main branch only, after deployment
- **Reports To**: Cloud Storage bucket
- **Duration**: ~2-5 minutes

## Best Practices

✅ **DO:**
- Review ZAP reports after each deployment
- Address High and Medium findings promptly
- Use security headers (CSP, HSTS, X-Frame-Options)
- Keep dependencies updated
- Enable HTTPS everywhere

❌ **DON'T:**
- Ignore High-risk findings
- Disable security scans
- Expose sensitive endpoints without authentication
- Use deprecated security configurations

## Troubleshooting

### Scan Fails or Times Out

- Check if the service is healthy: `curl https://your-service-url`
- Increase timeout in `cloudbuild.yaml` if needed
- Review ZAP logs in Cloud Build output

### False Positives

Create a ZAP configuration file to suppress false positives:

```bash
# Add to .zap/rules.tsv in repository
# Format: rule-id,state,threshold
10096,IGNORE,OFF  # Example: ignore timestamp disclosure
```

## Additional Resources

- [OWASP ZAP Documentation](https://www.zaproxy.org/docs/)
- [ZAP Baseline Scan](https://www.zaproxy.org/docs/docker/baseline-scan/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

