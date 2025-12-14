# 🔄 Estrategia de Actualización de Dependencias

## 🎯 Objetivo
Mantener las dependencias actualizadas SIN romper la app, usando testing automatizado.

---

## 📊 Clasificación de Dependencias por Riesgo

### 🟢 BAJO RIESGO (Auto-merge seguro)
**Estas dependencias NUNCA rompen la app:**

#### GitHub Actions:
- `actions/checkout`
- `actions/setup-python`
- `actions/setup-java`
- `actions/upload-artifact`
- `actions/download-artifact`

**¿Por qué son seguras?** Solo afectan el workflow CI/CD, no tu app.

**Acción:** ✅ Mergear siempre

---

### 🟡 RIESGO MEDIO (Revisar changelog)
**Pueden tener breaking changes pero son raros:**

#### Herramientas de seguridad Python:
- `bandit`
- `black`
- `ruff`
- `flake8`
- `safety`
- `pytest`

#### Firebase (generalmente retrocompatible):
- `firebase-bom`
- `firebase-crashlytics`
- `firebase-analytics`

#### Google Services:
- `play-services-ads` (AdMob)
- `user-messaging-platform`

**Acción:** 
1. Revisar changelog en el PR
2. Si no hay breaking changes → Mergear
3. El pipeline automáticamente compila y prueba

---

### 🔴 ALTO RIESGO (NUNCA auto-merge)
**Estas SÍ pueden romper tu app:**

#### Build Tools Críticos:
- ❌ `com.android.tools.build:gradle` (Android Gradle Plugin)
- ❌ `com.chaquo.python` (Chaquopy Plugin)
- ❌ `org.jetbrains.kotlin.android` (Kotlin Plugin)

#### Dependencias Core:
- ❌ `pandas` (puede romper scripts Python)
- ⚠️ Gradle wrapper version

**¿Por qué son peligrosas?**
- Chaquopy 16.x → 17.x puede cambiar cómo se compila Python
- Android Gradle Plugin 8.6 → 8.13 puede requerir cambios en build.gradle
- Kotlin 2.0 → 2.2 puede tener breaking changes en sintaxis

**Acción:**
1. ❌ NO mergear automáticamente
2. Crear una rama de testing
3. Probar localmente PRIMERO
4. Si funciona → entonces mergear

---

## 🛡️ ¿CÓMO SABER SI UN PR DE DEPENDABOT ROMPE LA APP?

### 🎯 Respuesta Corta: MIRA LOS CHECKS EN CADA PR ✅

**GitHub Actions ejecuta automáticamente el pipeline CI/CD en CADA PR de Dependabot.**

Si algún check FALLA → La actualización ROMPE algo
Si todos los checks PASAN → La actualización es SEGURA (probablemente)

---

### 📊 Sistema de Detección Automática

#### 1. **GitHub Actions ejecuta en cada PR:**

```
┌─────────────────────────────────────────────────────┐
│  PR #X: chore(deps): bump some-dependency          │
├─────────────────────────────────────────────────────┤
│  ✅ Python Security & Quality                       │
│  ✅ Android Security & Quality                      │
│  ✅ Unit Tests                                      │
│  ✅ Build Debug APK                                 │
├─────────────────────────────────────────────────────┤
│  Resultado: SAFE TO MERGE ✅                        │
└─────────────────────────────────────────────────────┘
```

Si ves esto → **Mergear es seguro**

```
┌─────────────────────────────────────────────────────┐
│  PR #X: chore(deps): bump chaquopy to 17.0.0       │
├─────────────────────────────────────────────────────┤
│  ✅ Python Security & Quality                       │
│  ❌ Android Security & Quality (FAILED)            │
│  ⏸️  Unit Tests (skipped)                          │
│  ⏸️  Build Debug APK (skipped)                     │
├─────────────────────────────────────────────────────┤
│  Resultado: DO NOT MERGE ❌                         │
└─────────────────────────────────────────────────────┘
```

Si ves esto → **NO MERGEAR, algo se rompió**

---

#### 2. **Cómo Revisar un PR de Dependabot:**

**PASO 1: Ver el estado de los checks**
```bash
Ir al PR en GitHub
┗━━ Scroll hasta "All checks have passed" o "Some checks failed"
    ┗━━ Si todos PASAN ✅ → Continuar al PASO 2
    ┗━━ Si alguno FALLA ❌ → NO MERGEAR, investigar
```

**PASO 2: Verificar QUÉ se compiló**
```bash
Click en "Details" del check "Android Security & Quality"
┗━━ Ver logs del step "🧹 Android Lint"
    ┗━━ Si dice "BUILD SUCCESSFUL" ✅ → La app compila
    ┗━━ Si dice "BUILD FAILED" ❌ → La dependencia rompió algo
```

**PASO 3: Verificar tests**
```bash
Click en "Details" del check "Unit Tests"
┗━━ Ver logs del step "🧪 Run Unit Tests"
    ┗━━ Si todos pasan ✅ → Tests OK
    ┗━━ Si alguno falla ❌ → La dependencia rompió funcionalidad
```

---

### 📊 TABLA DE DECISIONES RÁPIDAS

| Estado de Checks | Riesgo | Acción Recomendada |
|------------------|--------|-------------------|
| ✅ ✅ ✅ Todos PASS | 🟢 Bajo | **MERGEAR** inmediatamente |
| ✅ ✅ ⚠️ 1-2 warnings | 🟡 Medio | Revisar warnings, probablemente **MERGEAR** |
| ✅ ❌ ✅ Lint FAILED | 🟡 Medio | Ver qué lint falló, puede ser **MERGEAR** si son warnings menores |
| ❌ Build FAILED | 🔴 Alto | **NO MERGEAR** - La app no compila |
| ❌ Tests FAILED | 🔴 Alto | **NO MERGEAR** - Funcionalidad rota |
| ⏸️ Checks no ejecutados | 🔴 Alto | **NO MERGEAR** - Esperar a que terminen los checks |

---

### 🔍 EJEMPLOS DE ERRORES COMUNES Y QUÉ SIGNIFICAN

#### ❌ Error de Compilación (NO MERGEAR)
```
> Task :app:compileDebugKotlin FAILED
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:compileDebugKotlin'.
> Compilation error. See log for more details
```
**Significado:** La nueva versión tiene breaking changes que rompen tu código Kotlin
**Acción:** NO mergear, cerrar el PR

---

#### ❌ Error de Dependencia (NO MERGEAR)
```
Could not resolve com.chaquo.python:gradle:17.0.0
Required by:
    project :app
> Could not find com.chaquo.python:gradle:17.0.0
```
**Significado:** La nueva versión no existe o hay problemas de compatibilidad
**Acción:** NO mergear, cerrar el PR

---

#### ❌ Error de Tests (NO MERGEAR)
```
> Task :app:testDebugUnitTest
CalculadoraIMCTest > testCalculoIMCCorrecto FAILED
    java.lang.AssertionError: expected:<24.5> but was:<0.0>
```
**Significado:** La actualización cambió el comportamiento de alguna función
**Acción:** NO mergear, investigar o cerrar el PR

---

#### ⚠️ Warnings Aceptables (PUEDE SER SEGURO)
```
> Task :app:lintDebug
Warning: The 'backgroundColor' attribute is deprecated.
Use 'android:backgroundTint' instead.

Lint found 3 warnings (0 errors)
```
**Significado:** Warnings de deprecación, no rompen la app
**Acción:** Puedes mergear, arreglar los warnings después

---

## 🛡️ Estrategia de Testing Automatizado (3 Niveles)

#### **Nivel 1: Pre-merge Checks Automáticos (EN CADA PR)**
```yaml
# YA CONFIGURADO en tu pipeline ✅
# Se ejecuta automáticamente en TODOS los PRs (incluidos Dependabot)

on:
  pull_request:
    branches: [ main ]

jobs:
  python-security-scan:    # ✅ Verifica código Python
  android-security-scan:   # ✅ Compila la app (detecta errores)
  unit-tests:              # ✅ Ejecuta tests (detecta bugs)
```

**¿Qué detecta?**
- ✅ Errores de compilación (si Chaquopy/Gradle rompe)
- ✅ Tests que fallan (si pandas/firebase rompe funcionalidad)
- ✅ Warnings de lint (problemas potenciales)
- ✅ Vulnerabilidades de seguridad

**¿Cuándo se ejecuta?**
- 🤖 Automáticamente en CADA push a CADA PR
- 🤖 Dependabot crea el PR → GitHub Actions lo prueba
- 👀 Tú solo miras si los checks pasan

---

#### **Nivel 2: Build Test Completo (Ya está en el pipeline)**
```yaml
# El job "android-security-scan" ya incluye:
- ./gradlew lintDebug           # ✅ Compila y verifica
- ./gradlew dependencyCheckAnalyze  # ✅ Escanea vulnerabilidades
```

**Esto ya detecta si la app compila con la nueva dependencia**

---

#### **Nivel 3: Testing Manual (Solo para dependencias CRÍTICAS)**
```bash
# SOLO si quieres estar 100% seguro antes de mergear
# (Por ejemplo, para Chaquopy o Gradle)

# 1. Checkout el PR localmente
gh pr checkout 9  # O: git fetch origin pull/9/head:pr-9 && git checkout pr-9

# 2. Compilar
./gradlew clean assembleDebug

# 3. Instalar en dispositivo real
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Probar manualmente:
# - Calcular IMC
# - Ver gráficos
# - Verificar que pandas funciona
# - Probar AdMob

# 5. Si todo funciona → Mergear el PR
```

---

### 🎯 FLUJO COMPLETO DE DECISIÓN

```
┌──────────────────────────────────────┐
│  Dependabot crea PR #X               │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  GitHub Actions ejecuta checks       │
│  automáticamente                     │
└──────────────┬───────────────────────┘
               │
               ▼
         ¿Todos los checks
          pasan? ✅
               │
       ┌───────┴───────┐
       │               │
      SÍ              NO
       │               │
       ▼               ▼
┌─────────────┐  ┌──────────────┐
│ ¿Riesgo?    │  │ NO MERGEAR   │
└──────┬──────┘  │ Investigar   │
       │         │ por qué falló│
   ┌───┴────┐    └──────────────┘
   │        │
 BAJO    MEDIO/ALTO
   │        │
   ▼        ▼
MERGEAR  PROBAR
 AHORA   MANUAL
```

---

### 📋 EJEMPLO REAL: Verificar PR #4 (Firebase BOM)

```bash
1. Ir a: https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/pull/4

2. Scroll hasta ver "Checks" (abajo del título)

3. Verificar estado:
   ✅ CI/CD Pipeline - DevSecOps / Python Security & Quality
   ✅ CI/CD Pipeline - DevSecOps / Android Security & Quality
   ✅ CI/CD Pipeline - DevSecOps / Unit Tests
   
4. Si TODOS tienen ✅ → SAFE TO MERGE

5. Si ALGUNO tiene ❌:
   - Click en "Details"
   - Ver qué step falló
   - Leer el error en los logs
   - Decidir: ¿Es crítico? ¿Se puede arreglar?
```

---

### ⚠️ SEÑALES DE PELIGRO EN LOS CHECKS

**🚨 Señales de que la actualización ROMPE la app:**

```
❌ BUILD FAILED
❌ Compilation error in...
❌ Task :app:compileDebugKotlin FAILED
❌ Could not resolve dependency
❌ Tests failed: X failing
❌ java.lang.NoSuchMethodError
❌ python.chaquopy.error.PyException
```

**⚠️ Señales que PUEDEN ser aceptables:**

```
⚠️ Lint found X issues (warnings, no errors)
⚠️ Deprecated API used (warning)
⚠️ Some dependency has known vulnerabilities (depende del severity)
```

---

### 💡 PRO TIP: Configurar Notificaciones

```bash
# Opción 1: GitHub notificará por email si un check falla

# Opción 2: Ver rápidamente todos los PRs con checks fallidos
Ir a: https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/pulls
Filtrar por: "status:failure"
```

---

## 🚀 Plan de Acción Inmediato

### PASO 1: Mergear PRs SEGUROS (ahora mismo)

**PRs para mergear SIN miedo:**
- #2 → actions/checkout
- #1 → actions/setup-python
- #5 → actions/setup-java
- #7 → actions/upload-artifact
- #10 → actions/download-artifact

**Cómo:**
```bash
# Desde GitHub web UI, en cada PR:
1. Verificar que los checks pasen ✅
2. Click "Merge pull request"
3. Click "Confirm merge"
```

---

### PASO 2: Mergear PRs de RIESGO MEDIO (después de revisar)

**Orden recomendado:**

1. **#14 → Safety** (herramienta de seguridad)
2. **#11 → Flake8** (herramienta de linting)
3. **#13 → Pytest** (herramienta de testing)
4. **#4 → Firebase BOM** (34.2.0 → 34.7.0)
5. **#19 → Firebase Crashlytics** (20.0.1 → 20.0.3)
6. **#18 → Play Services Ads** (24.5.0 → 24.9.0)

**Cómo verificar:**
```bash
# Antes de mergear, verificar en el PR:
1. Ir al PR en GitHub
2. Ver el tab "Checks" 
3. Si todos pasan ✅ → Mergear
```

---

### PASO 3: DETENER PRs de ALTO RIESGO (NO mergear todavía)

**PRs que debes CERRAR o POSTPONER por ahora:**

- ❌ #20 → Gradle 8.6.0 → 8.13.2 **(PELIGROSO)**
- ❌ #9/#6 → Chaquopy 16.1.0 → 17.0.0 **(PELIGROSO)**
- ❌ #17/#16 → Kotlin 2.0.0 → 2.2.21 **(PELIGROSO)**
- ⚠️ #12 → pandas 2.2.3 → 2.3.3 **(REVISAR)**
- ❌ #3 → Android Application 8.11.1 → 8.13.2 **(PELIGROSO)**
- ⚠️ #8 → androidx.activity 1.9.3 → 1.12.1 **(PUEDE SER SEGURO)**

**Acción para estos:**
```bash
# En GitHub, en cada PR peligroso:
1. Añadir comentario: "Postponed for manual testing"
2. Click "Close pull request" (sin mergear)
3. Dependabot NO los volverá a crear (porque ya existen)
```

---

## 🔧 Cómo Testear Actualizaciones PELIGROSAS Localmente

### Ejemplo: Actualizar Chaquopy 16.1.0 → 17.0.0

```bash
# 1. Crear rama de testing
git checkout -b test/chaquopy-17

# 2. Actualizar manualmente en build.gradle
# Cambiar: id ("com.chaquo.python") version "16.1.0"
# Por:     id ("com.chaquo.python") version "17.0.0"

# 3. Sincronizar y compilar
./gradlew clean
./gradlew assembleDebug

# 4. Si compila ✅, probar en dispositivo:
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. Probar funcionalidad Python:
# - Calcular IMC
# - Ver gráficos
# - Verificar que pandas funciona

# 6. Si funciona ✅:
git add app/build.gradle
git commit -m "chore: upgrade chaquopy to 17.0.0 [tested]"
git push origin test/chaquopy-17

# 7. Crear PR manual
# 8. Cerrar PR de Dependabot #9
```

---

## 📋 Checklist de Actualización Segura

### Para CUALQUIER dependencia crítica:

- [ ] ¿Compila sin errores? `./gradlew assembleDebug`
- [ ] ¿Pasan los tests? `./gradlew testDebugUnitTest`
- [ ] ¿La app instala? `adb install`
- [ ] ¿Funciona el cálculo IMC? (Python + pandas)
- [ ] ¿Se muestran los gráficos? (MPAndroidChart)
- [ ] ¿Funcionan los anuncios? (AdMob)
- [ ] ¿Firebase registra eventos? (Analytics)

Si alguno falla → NO ACTUALIZAR todavía

---

## 🎯 Regla de Oro

> **"Si no es una vulnerabilidad crítica de seguridad,**
> **NO actualices dependencias core durante desarrollo activo"**

Mantén:
- Gradle Plugin estable
- Chaquopy estable
- Kotlin estable

Actualiza:
- Firebase (generalmente seguro)
- Herramientas de seguridad
- GitHub Actions
- AdMob (Google lo mantiene retrocompatible)

---

## 📊 Configuración Recomendada de Dependabot

Voy a crear un archivo para configurar Dependabot y que NO cree PRs de dependencias peligrosas automáticamente.

**Archivo:** `.github/dependabot.yml`

```yaml
version: 2
updates:
  # Gradle dependencies
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    
    # Ignorar actualizaciones de dependencias core peligrosas
    ignore:
      - dependency-name: "com.android.tools.build:gradle"
        update-types: ["version-update:semver-major", "version-update:semver-minor"]
      - dependency-name: "com.chaquo.python"
        update-types: ["version-update:semver-major"]
      - dependency-name: "org.jetbrains.kotlin.android"
        update-types: ["version-update:semver-major"]
      - dependency-name: "com.android.application"
        update-types: ["version-update:semver-major", "version-update:semver-minor"]

  # Python dependencies
  - package-ecosystem: "pip"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 5
    
    # Ignorar actualizaciones mayores de pandas
    ignore:
      - dependency-name: "pandas"
        update-types: ["version-update:semver-major"]

  # GitHub Actions
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
    # GitHub Actions son seguros, permitir todas
```

---

## 💡 Resumen de Tu Situación

**Tienes 20 PRs de Dependabot:**

- ✅ **5 PRs seguros** (GitHub Actions) → Mergear ahora
- 🟡 **6 PRs de riesgo medio** (Firebase, Safety, etc.) → Mergear después de revisar
- ❌ **9 PRs peligrosos** (Gradle, Chaquopy, Kotlin) → CERRAR/POSTPONER

**Total tiempo:** ~15-20 minutos para mergear los seguros

---

¿Quieres que configure el archivo `dependabot.yml` para evitar estos PRs peligrosos en el futuro?

