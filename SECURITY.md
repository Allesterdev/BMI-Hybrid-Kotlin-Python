# 🔒 Guía de Seguridad DevSecOps - Calculadora IMC

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

## 🔑 Secretos Requeridos en GitHub

Configura estos secretos en: **Settings → Secrets and variables → Actions → New repository secret**

### Para Firma de APK/AAB:
```bash
KEYSTORE_FILE          # Base64 del archivo .jks (ver instrucciones abajo)
KEYSTORE_PASSWORD      # Contraseña del keystore
KEY_ALIAS              # Alias de la key de firma
KEY_PASSWORD           # Contraseña de la key
```

### Para Deploy a Play Store:
```bash
PLAY_STORE_JSON        # JSON del Service Account (ver instrucciones abajo)
```

---

## 📝 Instrucciones de Configuración

### 1. Convertir Keystore a Base64

```bash
# En Linux/Mac
base64 -w 0 tu-keystore.jks > keystore-base64.txt

# En Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("tu-keystore.jks")) > keystore-base64.txt
```

Copia el contenido de `keystore-base64.txt` → GitHub Secret `KEYSTORE_FILE`

### 2. Crear Service Account de Google Play

1. Ve a [Google Play Console](https://play.google.com/console)
2. **Configuración → Acceso a la API → Crear nuevo proyecto de servicio**
3. Sigue el enlace a Google Cloud Console
4. Crea una cuenta de servicio con permisos de **Service Account User**
5. Descarga el JSON de la cuenta de servicio
6. En Play Console, otorga permisos a la cuenta de servicio:
   - **Admin (para lanzamientos a internal testing)**
7. Copia el contenido del JSON → GitHub Secret `PLAY_STORE_JSON`

---

## 🛡️ Configuración de Seguridad del Repositorio

### 1. Proteger la rama main

**Settings → Branches → Add branch protection rule:**

```
Branch name pattern: main

☑ Require a pull request before merging
☑ Require status checks to pass before merging
  - python-security-scan
  - android-security-scan
  - unit-tests
☑ Require conversation resolution before merging
☑ Do not allow bypassing the above settings (ni siquiera tú)
☑ Restrict who can push to matching branches
  - Añade solo tu usuario
```

### 2. Desactivar contribuciones externas

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
☑ Allow rebase merging (solo para ti)
```

### 3. Activar Dependabot Alerts

**Settings → Security → Code security and analysis:**
```
☑ Dependency graph
☑ Dependabot alerts
☑ Dependabot security updates
☑ Grouped security updates
```

### 4. Activar CodeQL (si tienes GitHub Advanced Security)

**Settings → Security → Code security and analysis:**
```
☑ Code scanning
☑ CodeQL analysis
```

---

## 🧪 Ejecutar Checks Localmente

### Python Security Check
```bash
# Instalar herramientas
pip install -r requirements-dev.txt

# Ejecutar todos los checks
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

# Ver reportes
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

Después de cada ejecución del pipeline, puedes descargar:

1. **bandit-security-report.json** - Vulnerabilidades Python
2. **android-lint-report.html** - Problemas Android
3. **test-reports/** - Resultados de tests
4. **signed-aab** - AAB firmado listo para Play Store
5. **release-notes.txt** - Notas de la versión

---

## ⚠️ Notas Importantes

### Sobre el Repositorio Público
- ✅ Los secretos están 100% seguros en repos públicos
- ✅ GitHub NUNCA expone secretos en logs
- ✅ Los secretos NO son accesibles en PRs de forks
- ⚠️ **NO** commiteés `google-services.json` si contiene info sensible
- ⚠️ **NO** commiteés archivos `.jks` o `.keystore`

### Sobre los AdMob IDs
- ❓ Los IDs de AdMob están hardcodeados en `build.gradle`
- 💡 **Recomendación**: Moverlos a GitHub Secrets para mayor seguridad
- 💡 Usar `buildConfigField` con valores de secrets en CI

### Sobre el Despliegue
- 🎮 **Control total**: TÚ decides cuándo lanzar a producción
- 🔄 El pipeline sube a **Internal Testing** automáticamente (opcional)
- 👤 Desde Play Console promueves manualmente a Producción
- ⏸️ Puedes pausar el pipeline con `workflow_dispatch` (manual trigger)

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

**¿Preguntas?** El pipeline está diseñado para ser seguro por defecto y darte control total sobre los releases. 🎯

