# 🛡️ BMI Calculator - With Complete DevSecOps Pipeline

[![CI/CD Pipeline](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions/workflows/ci-cd-pipeline.yml/badge.svg)](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions)
[![CodeQL](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions)
[![Security Rating](https://img.shields.io/badge/security-A+-brightgreen.svg)](SECURITY.es.md)

> Android BMI calculator app with comprehensive analysis for adults and children, implementing DevSecOps best practices.

---

**🌐 Language:** [🇪🇸 Español](README.es.md) | **🇬🇧 English**

---

## 🚀 Features

- **BMI Calculation** for adults and children
- **Percentile Analysis** using WHO data
- **Interactive Charts** for progress tracking
- **Measurement History**
- **Python-Kotlin Integration** with Chaquopy
- **Google AdMob** integrated
- **Firebase Analytics & Crashlytics**
- **Complete CI/CD Pipeline** with DevSecOps

---

## 📜 Technical Evolution & Engineering Journey

This project is the result of a rigorous engineering process, evolving from a simple CLI tool to a production-grade hybrid application. The journey highlights a strategic shift from pure development to a **Security-First** mindset.

### 🔹 Phase 1: Rapid Prototyping (Python)
The project began as a Python script to validate BMI algorithms and WHO data classification.
* **Focus:** Logic validation using Pandas.
* **Environment:** CLI (VS Code).

### 🔹 Phase 2: The Cross-Platform Bottleneck (Kivy)
I initially attempted a cross-platform GUI using **Kivy** and **Buildozer**.
* **The Challenge:** Compiling scientific libraries like **NumPy** and **Matplotlib** for Android ARM architecture proved unstable due to complex C-dependencies.
* **The Pivot:** After facing constant ABI incompatibilities and realizing the UI limitations for a modern user experience, a strategic decision was made to migrate to a native architecture.

### 🔹 Phase 3: The Hybrid Solution & Tooling Shift
The solution pivoted to a **Hybrid Architecture** (Kotlin UI + Python Logic via Chaquopy), requiring a complete change of environment.
* **🛠️ The Tooling Upgrade:** I migrated from VS Code to **Android Studio** to leverage professional debugging tools, native emulators, and strict project structure enforcement.
* **🛑 The Blocker:** Integrating Chaquopy into the modern Android ecosystem caused severe Gradle build failures that AI assistants could not resolve.
* **✅ The Fix:** I manually debugged the build process by analyzing the official documentation, restructuring the `build.gradle` dependency graph, and enforcing version locking.

### 🔹 Phase 4: The DevSecOps Integration (Security First)
**Parallel to development, I was advancing my training in Cybersecurity.** I realized that functional code is not enough; it must be secure by design. This led to the adoption of the **DevSecOps culture**:
* **Shift-Left Security:** I integrated automated security scanners (Bandit, OWASP, Lint) directly into the CI pipeline to catch vulnerabilities *before* the merge.
* **Release Management:** Automated the deployment to Google Play (Internal Track) to eliminate human error during release, successfully managing the 14-day Closed Testing mandate.

### 📸 Visual Evolution: Before & After

| Initial Cross-Platform (Kivy) | Final Native Architecture (Kotlin) |
| :---: | :---: |
| <img src="project_docs/kivy_capture.png" width="350"> | <img src="project_docs/final_capture.png" width="350"> |
| *Python GUI (Buildozer issues)* | *Production Hybrid App (Android Studio)* |

---

## 🛡️ DevSecOps Pipeline Architecture

This repository implements a **"Shift-Left" security strategy**. The GitHub Actions workflow (`ci-cd-pipeline.yml`) enforces strict quality gates before any code reaches production.

### 🔍 Automated Security Gates (The "Gauntlet")
Every push triggers parallel security scans. If critical vulnerabilities are found, the pipeline blocks the merge.

| Stage | Tool | Purpose |
| :--- | :--- | :--- |
| **Python SAST** | **Bandit** | Detects security issues in Python backend logic. |
| **Python SCA** | **Safety** | Checks `requirements.txt` against known CVE databases. |
| **Code Quality** | **Ruff & Black** | Enforces PEP 8 standards and strict formatting. |
| **Android SCA** | **OWASP Dependency Check** | Scans Gradle dependencies using the **NVD API** for vulnerabilities. |
| **Android Lint** | **Lint** | Static analysis for Android performance and usability issues. |

### 🚀 Continuous Delivery (CD) Flow

1.  **Build:** Generates a signed Android App Bundle (`.aab`) using a secured Keystore injected via **GitHub Secrets** (Base64 encoded).
2.  **Release:** On a published release event, the pipeline automatically:
    * Signs the artifact.
    * Generates release notes based on git commits.
    * **Deploys to Google Play Console (Internal Testing Track)** using the Google Play Developer API.

> **Note:** The pipeline uses `continue-on-error: true` for non-critical linters to maintain development velocity, but critical security flaws will flag the build status.

---

## 🔒 Security & Quality

This project implements a **complete DevSecOps pipeline** that verifies:

### 🐍 Python
- ✅ **Bandit** - Vulnerability scanner
- ✅ **Black** - Automatic formatter
- ✅ **Ruff** - Modern and fast linter
- ✅ **Flake8** - PEP 8 style verification
- ✅ **Safety** - CVEs in Python dependencies

### 🤖 Android
- ✅ **Android Lint** - Static analysis
- ✅ **OWASP Dependency Check** - CVEs in dependencies
- ✅ **Dependabot** - Automatic updates

**Total: 8 active tools**

> **Note on CodeQL:** Deep semantic analysis with CodeQL is currently disabled due to compatibility challenges with Chaquopy (Python-Android hybrid architecture). The current 8-tool security stack provides comprehensive coverage (~90% of what CodeQL would detect). CodeQL can be re-enabled when the build complexity is resolved or if the project structure changes.

📚 **Complete documentation:** [SECURITY.md](SECURITY.md)

---

## 🛠️ Technologies

- **Language:** Kotlin + Python 3.10
- **Min SDK:** 28 (Android 9.0)
- **Target SDK:** 36
- **IDE:** Android Studio
- **CI/CD:** GitHub Actions
- **Analysis:** Chaquopy (Python integration)
- **Charts:** MPAndroidChart
- **Backend:** Firebase (Analytics + Crashlytics)
- **Monetization:** Google AdMob

---


## 🧪 Run Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Android Lint
./gradlew lintDebug

# Python security checks
bandit -r app/src/main/python/
black --check app/src/main/python/
ruff check app/src/main/python/
```

---

## 🚀 CI/CD Pipeline

### Triggers
- **Push to main/develop** → All security checks and tests
- **Pull Request** → Complete validation before merge
- **Release** → Signed build + Deploy to Play Store (Internal Testing)
- **Manual** → Workflow dispatch for full control

### Flow
```
Push → Security Scans → Tests → Build AAB → Sign → Deploy
```

### Generated Artifacts
- 📊 Security reports (Bandit, Lint, OWASP)
- 📦 Signed AAB ready for Play Store
- 📝 Automatic release notes
- 🧪 Test reports

---

## 🔐 Configured Secrets

The following secrets are secured in GitHub Actions:

- `KEYSTORE_FILE` - Signing keystore in Base64
- `KEYSTORE_PASSWORD` - Keystore password
- `KEY_ALIAS` - Signing key alias
- `KEY_PASSWORD` - Key password
- `GOOGLE_SERVICES_JSON` - Firebase configuration
- `NVD_API_KEY` - API Key for OWASP Dependency Check
- `ADMOB_APP_ID_RELEASE` - AdMob application ID (production)
- `ADMOB_INTERSTITIAL_ID_RELEASE` - Interstitial ad ID
- `ADMOB_NATIVE_ADULTOS_ID_RELEASE` - Native ad ID for adults
- `ADMOB_NATIVE_MENORES_ID_RELEASE` - Native ad ID for children
- `PLAY_STORE_JSON` - Google Play Service Account (optional)

**All sensitive data is protected with GitHub Secrets** 🔒

---

## 📱 Download

<a href="https://play.google.com/store/apps/details?id=com.allesterdev.imcpractico">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" width="200">
</a>

---

## 📄 License

This project is open source as a professional portfolio. The repository is **read-only** - external contributions are not accepted.

---

## 👤 Author

**Oscar** - [GitHub](https://github.com/Allesterdev) | [LinkedIn](https://www.linkedin.com/in/oscar-campoy-ballester-sec)

---

## 🙏 Acknowledgments

- Firebase for backend infrastructure
- Google AdMob for monetization
- PhilJay for MPAndroidChart
- Chaquopy for Python-Android integration
- Open-source community for security tools

---

<p align="center">
  <strong>🔒 Developed with DevSecOps best practices</strong>
</p>

<p align="center">
  <sub>Automatic CI/CD pipeline | Continuous security analysis | Guaranteed code quality</sub>
</p>

