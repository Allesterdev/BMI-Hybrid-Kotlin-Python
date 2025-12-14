# 🛡️ Calculadora IMC - Con Pipeline DevSecOps Completo

[![CI/CD Pipeline](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions/workflows/ci-cd-pipeline.yml/badge.svg)](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions)
[![CodeQL](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/actions)
[![Security Rating](https://img.shields.io/badge/security-A+-brightgreen.svg)](SECURITY.md)

> Aplicación Android de cálculo de IMC con análisis completo para adultos y menores, implementando las mejores prácticas de DevSecOps.

---

**🌐 Idioma:** **🇪🇸 Español** | [🇬🇧 English](README.md)

---

## 🚀 Características

- **Cálculo de IMC** para adultos y menores de edad
- **Análisis con percentiles** usando datos de la OMS
- **Gráficos interactivos** de evolución
- **Historial** de mediciones
- **Integración Python-Kotlin** con Chaquopy
- **Google AdMob** integrado
- **Firebase Analytics & Crashlytics**
- **Pipeline CI/CD completo** con DevSecOps

---

## 🛡️ Arquitectura del Pipeline DevSecOps

Este repositorio implementa una **estrategia de seguridad "Shift-Left"**. El workflow de GitHub Actions (`ci-cd-pipeline.yml`) aplica estrictas puertas de calidad antes de que cualquier código llegue a producción.

### 🔍 Puertas de Seguridad Automatizadas (El "Gauntlet")
Cada push desencadena escaneos de seguridad paralelos. Si se encuentran vulnerabilidades críticas, el pipeline bloquea el merge.

| Etapa | Herramienta | Propósito |
| :--- | :--- | :--- |
| **Python SAST** | **Bandit** | Detecta problemas de seguridad en la lógica backend de Python. |
| **Python SCA** | **Safety** | Verifica `requirements.txt` contra bases de datos de CVE conocidas. |
| **Calidad de Código** | **Ruff & Black** | Aplica estándares PEP 8 y formato estricto. |
| **Android SCA** | **OWASP Dependency Check** | Escanea dependencias Gradle usando la **API NVD** en busca de vulnerabilidades. |
| **Android Lint** | **Lint** | Análisis estático para problemas de rendimiento y usabilidad de Android. |

### 🚀 Flujo de Entrega Continua (CD)

1.  **Build:** Genera un Android App Bundle (`.aab`) firmado usando un Keystore seguro inyectado vía **GitHub Secrets** (codificado en Base64).
2.  **Release:** En un evento de release publicado, el pipeline automáticamente:
    * Firma el artefacto.
    * Genera notas de release basadas en commits de git.
    * **Despliega a Google Play Console (Track de Testing Interno)** usando la API de Google Play Developer.

> **Nota:** El pipeline usa `continue-on-error: true` para linters no críticos para mantener la velocidad de desarrollo, pero las fallas de seguridad críticas marcarán el estado del build.

---

## 🔒 Seguridad & Calidad

Este proyecto implementa un **pipeline DevSecOps completo** que verifica:

### 🐍 Python
- ✅ **Bandit** - Scanner de vulnerabilidades
- ✅ **Black** - Formateador automático
- ✅ **Ruff** - Linter moderno y rápido
- ✅ **Flake8** - Verificación de estilo PEP 8
- ✅ **Safety** - CVEs en dependencias Python

### 🤖 Android
- ✅ **Android Lint** - Análisis estático
- ✅ **OWASP Dependency Check** - CVEs en dependencias
- ⏸️ **CodeQL** - Análisis profundo (desactivado: requiere repo público)
- ✅ **Dependabot** - Actualizaciones automáticas

**Total: 8 herramientas activas** (CodeQL se activará con repo público)

📚 **Documentación completa:** [TOOLS_SUMMARY.md](TOOLS_SUMMARY.md)

---

## 📋 Documentación

| Documento | Descripción |
|-----------|-------------|
| [QUICKSTART.md](QUICKSTART.md) | 🚀 Guía rápida de configuración |
| [SECURITY.md](SECURITY.md) | 🔒 Guía detallada de seguridad |
| [TOOLS_SUMMARY.md](TOOLS_SUMMARY.md) | 🛠️ Herramientas implementadas |

---

## 🛠️ Tecnologías

- **Lenguaje:** Kotlin + Python 3.10
- **Min SDK:** 28 (Android 9.0)
- **Target SDK:** 36
- **IDE:** Android Studio
- **CI/CD:** GitHub Actions
- **Análisis:** Chaquopy (Python integration)
- **Charts:** MPAndroidChart
- **Backend:** Firebase (Analytics + Crashlytics)
- **Monetización:** Google AdMob

---

## 📦 Instalación Local

```bash
# Clonar el repositorio
git clone git@github.com:Allesterdev/BMI-Hybrid-Kotlin-Python.git
cd BMI-Hybrid-Kotlin-Python

# Instalar dependencias Python para desarrollo
pip install -r requirements-dev.txt

# Ejecutar checks de seguridad localmente
./run-security-checks.sh

# Abrir en Android Studio y sincronizar Gradle
```

---

## 🧪 Ejecutar Tests

```bash
# Tests unitarios
./gradlew testDebugUnitTest

# Android Lint
./gradlew lintDebug

# Checks de seguridad Python
bandit -r app/src/main/python/
black --check app/src/main/python/
ruff check app/src/main/python/
```

---

## 🚀 Pipeline CI/CD

### Triggers
- **Push a main/develop** → Todos los checks de seguridad y tests
- **Pull Request** → Validación completa antes de merge
- **Release** → Build firmado + Deploy a Play Store (Internal Testing)
- **Manual** → Workflow dispatch para control total

### Flujo
```
Push → Security Scans → Tests → Build AAB → Sign → Deploy
```

### Artefactos Generados
- 📊 Reportes de seguridad (Bandit, Lint, OWASP)
- 📦 AAB firmado listo para Play Store
- 📝 Release notes automáticas
- 🧪 Reportes de tests

---

## 🔐 Secretos Configurados

Los siguientes secretos están seguros en GitHub Actions:

- `KEYSTORE_FILE` - Keystore de firma en Base64
- `KEYSTORE_PASSWORD` - Contraseña del keystore
- `KEY_ALIAS` - Alias de la key de firma
- `KEY_PASSWORD` - Contraseña de la key
- `GOOGLE_SERVICES_JSON` - Configuración de Firebase
- `NVD_API_KEY` - API Key para OWASP Dependency Check
- `ADMOB_APP_ID_RELEASE` - ID de aplicación AdMob (producción)
- `ADMOB_INTERSTITIAL_ID_RELEASE` - ID de anuncio intersticial
- `ADMOB_NATIVE_ADULTOS_ID_RELEASE` - ID de anuncio nativo adultos
- `ADMOB_NATIVE_MENORES_ID_RELEASE` - ID de anuncio nativo menores
- `PLAY_STORE_JSON` - Service Account de Google Play (opcional)

**Todos los datos sensibles están protegidos con GitHub Secrets** 🔒

---

## 📱 Descarga

<a href="https://play.google.com/store/apps/details?id=com.allesterdev.imcpractico">
  <img src="https://play.google.com/intl/en_us/badges/static/images/badges/es_badge_web_generic.png" alt="Disponible en Google Play" width="200">
</a>

---

## 📄 Licencia

Este proyecto es de código abierto como portfolio profesional. El repositorio es de **solo lectura** - no se aceptan contribuciones externas.

---

## 👤 Autor

**Oscar** - [GitHub](https://github.com/Allesterdev) | [LinkedIn](https://linkedin.com/in/oscar-herrero-diaz)

---

## 🙏 Agradecimientos

- Firebase por la infraestructura backend
- Google AdMob por la monetización
- PhilJay por MPAndroidChart
- Chaquopy por la integración Python-Android
- Comunidad open-source por las herramientas de seguridad

---

<p align="center">
  <strong>🔒 Desarrollado con las mejores prácticas de DevSecOps</strong>
</p>

<p align="center">
  <sub>Pipeline CI/CD automático | Análisis de seguridad continuo | Calidad de código garantizada</sub>
</p>

