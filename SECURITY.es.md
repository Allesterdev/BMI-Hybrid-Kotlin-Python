# 🔒 Guía de Seguridad DevSecOps - Calculadora IMC

**🌐 Idioma:** **🇪🇸 Español** | [🇬🇧 English](SECURITY.md)

---

## 📋 Herramientas de Seguridad Implementadas

### 🐍 **Python Security & Quality**

#### 1. **Bandit** - Security Scanner
- **Qué hace**: Analiza el código Python en busca de vulnerabilidades de seguridad comunes
- **Ejemplos de detección**:
  - Uso de `eval()`, `exec()`, `pickle` (code injection)
  - Contraseñas hardcodeadas
  - SQL injection
  - Uso de funciones criptográficas débiles
  - Llamadas de sistema inseguras
- **Comando local**: `bandit -r app/src/main/python/`

#### 2. **Black** - Code Formatter
- **Qué hace**: Asegura formato consistente del código Python
- **Beneficio**: Código más legible, mantenible y profesional
- **Comando local**: `black app/src/main/python/`

#### 3. **Ruff** - Fast Linter
- **Qué hace**: Linter ultrarrápido que combina múltiples herramientas (Flake8, isort, pyupgrade, etc.)
- **Detección de**:
  - Errores de sintaxis
  - Variables no usadas
  - Imports incorrectos
  - Problemas de seguridad (integra reglas de Bandit)
  - Code smells
- **Comando local**: `ruff check app/src/main/python/`

#### 4. **Flake8** - Style Guide Enforcement
- **Qué hace**: Verifica que el código siga PEP 8 (estándar Python)
- **Comando local**: `flake8 app/src/main/python/ --max-line-length=120`

#### 5. **Safety** - Dependency Vulnerability Scanner
- **Qué hace**: Verifica si las dependencias Python (pandas) tienen CVEs conocidos
- **Base de datos**: PyUp Safety DB con vulnerabilidades conocidas
- **Comando local**: `pip freeze | safety check --stdin`

---

### 🤖 **Android/Kotlin Security**

#### 1. **Android Lint**
- **Qué hace**: Análisis estático del código Android
- **Detección de**:
  - Problemas de seguridad (permisos innecesarios, exportación de componentes)
  - Hardcoded secrets
  - API deprecadas
  - Problemas de rendimiento
  - Internacionalización
- **Comando local**: `./gradlew lintDebug`

#### 2. **OWASP Dependency Check**
- **Qué hace**: Identifica CVEs en todas las dependencias (Firebase, AdMob, etc.)
- **Base de datos**: National Vulnerability Database (NVD)
- **Comando local**: `./gradlew dependencyCheckAnalyze`

#### 3. **CodeQL** (GitHub Advanced Security)
- **Qué hace**: Análisis semántico profundo del código
- **Detecta**:
  - Inyecciones SQL
  - Cross-site scripting (XSS)
  - Path traversal
  - Uso inseguro de criptografía
  - Manejo inseguro de datos sensibles
- **Ejecución**: Automática en cada push
- **Estado**: ⏸️ **Temporalmente Desactivado**

> **¿Por qué desactivado?** El análisis Java/Kotlin de CodeQL requiere configuración de build compleja que es incompatible con la integración Python-Android de Chaquopy. El proceso de autobuild no puede rastrear correctamente la compilación híbrida. Las herramientas de seguridad actuales (Bandit, Android Lint, OWASP) proporcionan ~90% de cobertura de lo que CodeQL detectaría. Puede reactivarse en el futuro con mejoras en la configuración manual del build.

#### 4. **Dependabot**
- **Qué hace**: Actualización automática de dependencias con parches de seguridad
- **Crea PRs** automáticos cuando hay vulnerabilidades
- **Ecosistemas cubiertos**: Gradle, Python, GitHub Actions

---

## 🚀 Pipeline CI/CD Completo

### Flujo de Trabajo

```
┌─────────────────────────────────────────────────────────────┐
│  1. PUSH/PR a main o develop                                │
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
    │  Solo si es Release/Manual      │
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
    │  Promoción a Producción: MANUAL │
    └─────────────────────────────────┘
```

---

## 🔑 Arquitectura de Secretos en GitHub

El pipeline CI/CD utiliza GitHub Secrets para inyectar credenciales sensibles de forma segura sin hardcodearlas en el repositorio.

### Credenciales de Firma:
```bash
KEYSTORE_FILE          # Keystore Android codificado en Base64 (.jks)
KEYSTORE_PASSWORD      # Contraseña del keystore
KEY_ALIAS              # Alias de la clave de firma
KEY_PASSWORD           # Contraseña de la clave
GOOGLE_SERVICES_JSON   # Configuración de Firebase (Base64)
NVD_API_KEY            # API Key de National Vulnerability Database
```

### Integración AdMob:
```bash
ADMOB_APP_ID_RELEASE                 # Identificador de aplicación para producción
ADMOB_INTERSTITIAL_ID_RELEASE        # ID de unidad de anuncio intersticial
ADMOB_NATIVE_ADULTOS_ID_RELEASE      # ID de unidad de anuncio nativo (sección adultos)
ADMOB_NATIVE_MENORES_ID_RELEASE      # ID de unidad de anuncio nativo (sección menores)
```

### Despliegue en Play Store:
```bash
PLAY_STORE_JSON        # Credenciales de Service Account de Google Cloud (JSON)
```

---

## 📝 Detalles de Implementación Técnica

### Gestión del Keystore
La firma de la aplicación Android requiere un keystore codificado en Base64 inyectado vía GitHub Secrets:

```bash
# Codificación en Linux/Mac
base64 -w 0 keystore.jks > keystore-base64.txt

# Codificación en Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks")) > keystore-base64.txt
```

El keystore codificado se almacena en el secret `KEYSTORE_FILE` y se decodifica durante el proceso de build.

### Integración con Google Play API
El despliegue automatizado utiliza un Service Account de Google Cloud con la siguiente configuración:
- **Rol**: Service Account User
- **Permisos**: Acceso de Admin para el track de Internal Testing
- **Formato**: Archivo JSON almacenado en el secret `PLAY_STORE_JSON`

El pipeline utiliza la Google Play Publisher API v3 para subir los AABs firmados al track de Internal Testing.

---

## 🛡️ Arquitectura de Seguridad del Repositorio

### Protección de Ramas
El repositorio implementa reglas estrictas de protección de ramas:
- Pull requests requeridos para todos los merges
- Los status checks deben pasar: `python-security-scan`, `android-security-scan`, `unit-tests`
- Resolución de conversaciones requerida
- Sin bypass permitido (incluyendo al propietario del repositorio)
- Restricciones de push aplicadas

### Modelo de Contribución
Este es un proyecto de portfolio de solo lectura:
- Issues, PRs, Wiki y Discussions deshabilitados
- No se aceptan contribuciones externas
- Estrategia de merge: Solo rebase

### Gestión Automatizada de Dependencias
Dependabot monitorea y actualiza dependencias en:
- **Gradle** (dependencias Android)
- **Pip** (dependencias Python)
- **GitHub Actions** (dependencias de workflows)

---

## 🧪 Desarrollo Local y Testing

### Checks de Seguridad Python
```bash
# Instalar dependencias de desarrollo
pip install -r requirements-dev.txt

# Ejecutar escaneos de seguridad
bandit -r app/src/main/python/
black --check app/src/main/python/
ruff check app/src/main/python/
flake8 app/src/main/python/ --max-line-length=120
pip freeze | safety check --stdin
```

### Checks de Seguridad Android
```bash
# Análisis estático
./gradlew lintDebug

# Escaneo de vulnerabilidades en dependencias
./gradlew dependencyCheckAnalyze

# Ver reportes generados
open app/build/reports/lint-results-debug.html
open build/reports/dependency-check-report.html
```

---

## 🎯 Niveles de Seguridad

### 🟢 **Nivel 1: Pre-commit (Local)**
- Black auto-format
- Ruff quick check
- Tests unitarios básicos

### 🟡 **Nivel 2: CI Pipeline (cada push)**
- Todos los scanners de seguridad
- Lint completo
- Tests completos
- Reportes detallados

### 🔴 **Nivel 3: Release (solo releases)**
- Build firmado
- Verificación de firma
- Upload a Play Store (internal testing)
- **Promoción a Producción: MANUAL desde Play Console**

---

## 📊 Reportes Generados

Después de cada ejecución del pipeline, se generan los siguientes artefactos descargables:

1. **bandit-security-report.json** - Vulnerabilidades Python
2. **android-lint-report.html** - Problemas Android
3. **test-reports/** - Resultados de tests
4. **signed-aab** - AAB firmado listo para Play Store
5. **release-notes.txt** - Notas de la versión

---

## 🔐 GitHub Secrets y Configuración Sensible

### Gestión de Secretos

Este proyecto utiliza **GitHub Secrets** para proteger datos sensibles en el pipeline CI/CD:
- 🔑 Credenciales de firma de aplicaciones Android
- 🔑 Claves API para servicios externos
- 🔑 Credenciales de despliegue a Play Store
- 🔑 API keys para herramientas de análisis de seguridad

**Importante:**
- ✅ Todos los secretos están configurados en GitHub Actions
- ✅ GitHub **NUNCA** expone secretos en logs públicos
- ✅ Los secretos **NO** son accesibles en PRs de forks
- ✅ Este es un proyecto de **solo lectura** - no se aceptan contribuciones externas

### Archivos Protegidos

Los siguientes archivos **NO deben commitearse** y están en `.gitignore`:
```
google-services.json          # Firebase configuration
*.jks, *.keystore            # Android signing keys
local.properties             # SDK paths y configuración local
keystore.properties          # Credenciales de firma
*base64*.txt                 # Keystores encoded
service-account*.json        # Play Store credentials
```

### Para Desarrollo Local

Al clonar este proyecto para referencia (solo lectura):
1. Los secretos no son accesibles (protegidos por GitHub)
2. Los checks de seguridad pueden ejecutarse localmente sin secretos
3. Los builds de release firmados no son posibles (requiere keystores privados)
4. La variante de build `debug` puede usarse para desarrollo local sin signing

---

## ⚠️ Notas Importantes

### Sobre el Repositorio Público
- ✅ Los secretos están 100% seguros en repos públicos
- ✅ GitHub NUNCA expone secretos en logs
- ✅ Los secretos NO son accesibles en PRs de forks
- ✅ Este proyecto es de **solo lectura** - no se aceptan contribuciones

### Sobre el Despliegue
- 🎮 **Control manual**: La promoción a producción se realiza manualmente desde Play Console
- 🔄 El pipeline sube automáticamente al track de **Internal Testing**
- 👤 La promoción a producción requiere aprobación manual desde Play Console
- ⏸️ El pipeline puede ejecutarse manualmente mediante `workflow_dispatch`

---

## 🚦 Estados del Pipeline

| Estado | Significado |
|--------|------------|
| ✅ **All checks passed** | Código seguro, listo para merge/release |
| ⚠️ **Some checks failed** | Revisar reportes, posibles vulnerabilidades |
| ❌ **Build failed** | Error en compilación o tests |
| 🚀 **Deployed** | Subido a Play Store (internal testing) |

---

## 📚 Recursos Adicionales

- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- [Android Security Best Practices](https://developer.android.com/privacy-and-security/security-tips)
- [Python Security Best Practices](https://bandit.readthedocs.io/)
- [Google Play Security Guidelines](https://support.google.com/googleplay/android-developer/answer/9888379)

---


