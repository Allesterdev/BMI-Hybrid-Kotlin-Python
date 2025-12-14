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

## 🛡️ Estrategia de Testing Automatizado

### Nivel 1: Pre-merge Checks (Automático)
```yaml
# Ya lo tienes configurado en tu pipeline:
- Python Security Scan ✅
- Android Security Scan ✅
- Unit Tests ✅
- Build Debug ✅
```

### Nivel 2: Build Test (En cada PR)
```yaml
# Añadir al workflow para verificar builds:
- Build Debug APK
- Build Release AAB
- Run Instrumentation Tests (si los tienes)
```

### Nivel 3: Manual Testing (Para cambios críticos)
```bash
# Antes de mergear PRs de ALTO RIESGO:
1. git checkout pr/9  # Ejemplo: Chaquopy update
2. ./gradlew clean assembleDebug
3. Instalar en dispositivo real
4. Probar funcionalidad Python (cálculo IMC)
5. Si funciona → mergear
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

