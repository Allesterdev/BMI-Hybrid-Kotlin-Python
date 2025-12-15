# 🔒 DevSecOps Security Guide - BMI Calculator
**🌐 Language:** **🇬🇧 English** | [🇪🇸 Español](SECURITY.es.md)
---
## 📋 Implemented Security Tools
### 🐍 **Python Security & Quality**
#### 1. **Bandit** - Security Scanner
- **What it does**: Analyzes Python code for common security vulnerabilities
- **Detection examples**:
  - Use of `eval()`, `exec()`, `pickle` (code injection)
  - Hardcoded passwords
  - SQL injection
  - Use of weak cryptographic functions
  - Insecure system calls
- **Local command**: `bandit -r app/src/main/python/`
#### 2. **Black** - Code Formatter
- **What it does**: Ensures consistent Python code formatting
- **Benefit**: More readable, maintainable and professional code
- **Local command**: `black app/src/main/python/`
#### 3. **Ruff** - Fast Linter
- **What it does**: Ultra-fast linter that combines multiple tools (Flake8, isort, pyupgrade, etc.)
- **Detects**:
  - Syntax errors
  - Unused variables
  - Incorrect imports
  - Security issues (integrates Bandit rules)
  - Code smells
- **Local command**: `ruff check app/src/main/python/`
#### 4. **Flake8** - Style Guide Enforcement
- **What it does**: Verifies code follows PEP 8 (Python standard)
- **Local command**: `flake8 app/src/main/python/ --max-line-length=120`
#### 5. **Safety** - Dependency Vulnerability Scanner
- **What it does**: Checks if Python dependencies (pandas) have known CVEs
- **Database**: PyUp Safety DB with known vulnerabilities
- **Local command**: `pip freeze | safety check --stdin`
---
### 🤖 **Android/Kotlin Security**
#### 1. **Android Lint**
- **What it does**: Static analysis of Android code
- **Detects**:
  - Security issues (unnecessary permissions, component exports)
  - Hardcoded secrets
  - Deprecated APIs
  - Performance issues
  - Internationalization
- **Local command**: `./gradlew lintDebug`
#### 2. **OWASP Dependency Check**
- **What it does**: Identifies CVEs in all dependencies (Firebase, AdMob, etc.)
- **Database**: National Vulnerability Database (NVD)
- **Local command**: `./gradlew dependencyCheckAnalyze`
#### 3. **CodeQL** (GitHub Advanced Security)
- **What it does**: Deep semantic code analysis
- **Detects**:
  - SQL injections
  - Cross-site scripting (XSS)
  - Path traversal
  - Insecure cryptography usage
  - Insecure sensitive data handling
- **Execution**: Automatic on every push
- **Status**: ✅ Active (public repository)
#### 4. **Dependabot**
- **What it does**: Automatic dependency updates with security patches
- **Creates PRs** automatically when vulnerabilities exist
- **Covered ecosystems**: Gradle, Python, GitHub Actions
---
## 🚀 Complete CI/CD Pipeline
### Workflow
```
┌─────────────────────────────────────────────────────────────┐
│  1. PUSH/PR to main or develop                              │
└──────────────────┬──────────────────────────────────────────┘
                   │
    ┌──────────────┴──────────────┐
    │                             │
    ▼                             ▼
┌─────────────────┐    ┌─────────────────────┐
│ Python Security │    │ Android Security    │
│ & Quality       │    │ Scan                │
│                 │    │                     │
│ • Bandit        │    │ • Android Lint      │
│ • Black         │    │ • OWASP Dep Check   │
│ • Ruff          │    │                     │
│ • Flake8        │    │                     │
│ • Safety        │    │                     │
└────────┬────────┘    └──────────┬──────────┘
         │                        │
         └────────┬───────────────┘
                  │
                  ▼
         ┌────────────────┐
         │  Unit Tests    │
         │                │
         │ • Kotlin Tests │
         │ • Python Tests │
         └────────┬───────┘
                  │
                  ▼
    ┌─────────────────────────────────┐
    │  Only if Release/Manual         │
    └──────────────┬──────────────────┘
                   │
                   ▼
         ┌─────────────────┐
         │  Build & Sign   │
         │  Release AAB    │
         │                 │
         │ • bundleRelease │
         │ • Sign with Key │
         └────────┬────────┘
                  │
                  ▼
    ┌─────────────────────────────────┐
    │  Deploy to Play Store           │
    │  (Internal Testing)             │
    │                                 │
    │  Promote to Production: MANUAL  │
    └─────────────────────────────────┘
```
---
## 🔑 GitHub Secrets Architecture
The CI/CD pipeline uses GitHub Secrets to securely inject sensitive credentials without hardcoding them in the repository.
### For APK/AAB Signing:
```bash
KEYSTORE_FILE          # Base64 of .jks file (see instructions below)
KEYSTORE_PASSWORD      # Keystore password
KEY_ALIAS              # Signing key alias
KEY_PASSWORD           # Key password
GOOGLE_SERVICES_JSON   # Firebase configuration file
NVD_API_KEY            # NVD API Key for OWASP Dependency Check
```
### For AdMob Configuration:
```bash
ADMOB_APP_ID_RELEASE                 # AdMob application ID (production)
ADMOB_INTERSTITIAL_ID_RELEASE        # Interstitial ad ID
ADMOB_NATIVE_ADULTOS_ID_RELEASE      # Native ad ID for adults
ADMOB_NATIVE_MENORES_ID_RELEASE      # Native ad ID for children
```
### For Play Store Deploy:
```bash
PLAY_STORE_JSON        # Service Account JSON (see instructions below)
```
---
## 📝 Configuration Instructions
### 1. Convert Keystore to Base64
```bash
# On Linux/Mac
base64 -w 0 your-keystore.jks > keystore-base64.txt
# On Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks")) > keystore-base64.txt
```
Copy the content of `keystore-base64.txt` → GitHub Secret `KEYSTORE_FILE`
### 2. Create Google Play Service Account
1. Go to [Google Play Console](https://play.google.com/console)
2. **Settings → API Access → Create new service project**
3. Follow the link to Google Cloud Console
4. Create a service account with **Service Account User** permissions
5. Download the service account JSON
6. In Play Console, grant permissions to the service account:
   - **Admin (for releases to internal testing)**
7. Copy the JSON content → GitHub Secret `PLAY_STORE_JSON`
---
## 🛡️ Repository Security Configuration
### 1. Protect main branch
**Settings → Branches → Add branch protection rule:**
```
Branch name pattern: main
☑ Require a pull request before merging
☑ Require status checks to pass before merging
  - python-security-scan
  - android-security-scan
  - unit-tests
☑ Require conversation resolution before merging
☑ Do not allow bypassing the above settings (not even you)
☑ Restrict who can push to matching branches
  - Add only your user
```
### 2. Disable external contributions
**Settings → General → Features:**
```
☐ Issues
☐ Projects
☐ Wiki
☐ Discussions
```
**Settings → General → Pull Requests:**
```
☐ Allow merge commits
☐ Allow squash merging
☑ Allow rebase merging (only for you)
```
### 3. Activate Dependabot Alerts
**Settings → Security → Code security and analysis:**
```
☑ Dependency graph
☑ Dependabot alerts
☑ Dependabot security updates
☑ Grouped security updates
```
### 4. Activate CodeQL (if you have GitHub Advanced Security)
**Settings → Security → Code security and analysis:**
```
☑ Code scanning
☑ CodeQL analysis
```
---
## 🧪 Run Checks Locally
### Python Security Check
```bash
# Install tools
pip install -r requirements-dev.txt
# Run all checks
bandit -r app/src/main/python/
black --check app/src/main/python/
ruff check app/src/main/python/
flake8 app/src/main/python/ --max-line-length=120
pip freeze | safety check --stdin
```
### Android Security Check
```bash
# Lint
./gradlew lintDebug
# Dependency Check
./gradlew dependencyCheckAnalyze
# View reports
open app/build/reports/lint-results-debug.html
open build/reports/dependency-check-report.html
```
---
## 🎯 Security Levels
### 🟢 **Level 1: Pre-commit (Local)**
- Black auto-format
- Ruff quick check
- Basic unit tests
### 🟡 **Level 2: CI Pipeline (every push)**
- All security scanners
- Complete lint
- Complete tests
- Detailed reports
### 🔴 **Level 3: Release (only releases)**
- Signed build
- Signature verification
- Upload to Play Store (internal testing)
- **Promote to Production: MANUAL from Play Console**
---
## 📊 Generated Reports
After each pipeline run, the following artifacts are generated and available for download:
1. **bandit-security-report.json** - Python vulnerabilities
2. **android-lint-report.html** - Android issues
3. **test-reports/** - Test results
4. **signed-aab** - Signed AAB ready for Play Store
5. **release-notes.txt** - Version notes
---
## 🔐 GitHub Secrets and Sensitive Configuration
### Secrets Management
This project uses **GitHub Secrets** to protect sensitive data in the CI/CD pipeline:
- 🔑 Android app signing credentials
- 🔑 API keys for external services
- 🔑 Play Store deployment credentials
- 🔑 API keys for security analysis tools
**Important:**
- ✅ All secrets are configured in GitHub Actions
- ✅ GitHub **NEVER** exposes secrets in public logs
- ✅ Secrets are **NOT** accessible in PRs from forks
- ✅ This is a **read-only** project - external contributions not accepted
### Protected Files
The following files **MUST NOT be committed** and are in `.gitignore`:
```
google-services.json          # Firebase configuration
*.jks, *.keystore            # Android signing keys
local.properties             # SDK paths and local configuration
keystore.properties          # Signing credentials
*base64*.txt                 # Encoded keystores
service-account*.json        # Play Store credentials
```
### For Local Development
When cloning this project for reference (read-only):
1. Secrets are not accessible (protected by GitHub)
2. Security checks can be run locally without secrets
3. Signed release builds are not possible (requires private keystores)
4. The `debug` build variant can be used for local development without signing
---
## ⚠️ Important Notes
### About Public Repository
- ✅ Secrets are 100% secure in public repos
- ✅ GitHub NEVER exposes secrets in logs
- ✅ Secrets are NOT accessible in PRs from forks
- ✅ This project is **read-only** - contributions not accepted
### About Deployment
- 🎮 **Full control**: YOU decide when to release to production
- 🔄 Pipeline uploads to **Internal Testing** automatically (optional)
- 👤 From Play Console you manually promote to Production
- ⏸️ You can pause the pipeline with `workflow_dispatch` (manual trigger)
---
## 🚦 Pipeline States
| State | Meaning |
|--------|------------|
| ✅ **All checks passed** | Secure code, ready for merge/release |
| ⚠️ **Some checks failed** | Review reports, possible vulnerabilities |
| ❌ **Build failed** | Compilation or test error |
| 🚀 **Deployed** | Uploaded to Play Store (internal testing) |
---
## 📚 Additional Resources
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [Android Security Best Practices](https://developer.android.com/privacy-and-security/security-tips)
- [Python Security Best Practices](https://bandit.readthedocs.io/)
- [Google Play Security Guidelines](https://support.google.com/googleplay/android-developer/answer/9888379)
---
