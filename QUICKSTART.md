# 🚀 Guía Rápida de Inicio - Pipeline CI/CD

## 📋 Pasos para Implementar

### 1️⃣ **Preparar el Keystore de Firma**

Si no tienes un keystore, créalo:
```bash
keytool -genkey -v -keystore imcpractico.jks -keyalg RSA -keysize 2048 -validity 10000 -alias imcpractico
```

Luego conviértelo a Base64:
```bash
# Linux/Mac
base64 -w 0 imcpractico.jks > keystore-base64.txt

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("imcpractico.jks")) > keystore-base64.txt
```

### 2️⃣ **Configurar Service Account de Google Play**

1. Ir a [Google Play Console](https://play.google.com/console)
2. **Configuración → Acceso a la API**
3. Crear proyecto de servicio (seguir enlace a Google Cloud)
4. Crear cuenta de servicio con rol "Service Account User"
5. Generar clave JSON
6. Volver a Play Console y otorgar permisos de "Admin" a la cuenta

### 3️⃣ **Guardar Secretos en GitHub**

**IMPORTANTE: Hacer esto con el repositorio PRIVADO primero**

Ve a: **Settings → Secrets and variables → Actions → New repository secret**

Crea estos 5 secretos:
```
KEYSTORE_FILE        → Contenido del archivo keystore-base64.txt
KEYSTORE_PASSWORD    → La contraseña que usaste al crear el keystore
KEY_ALIAS            → El alias que usaste (ej: "imcpractico")
KEY_PASSWORD         → La contraseña de la key (puede ser igual que KEYSTORE_PASSWORD)
PLAY_STORE_JSON      → Contenido completo del JSON del Service Account
```

### 4️⃣ **Proteger la Rama Main**

**Settings → Branches → Add branch protection rule:**

- Branch name pattern: `main`
- ✅ Require a pull request before merging
- ✅ Require status checks to pass
- ✅ Do not allow bypassing

### 5️⃣ **Activar Seguridad**

**Settings → Code security and analysis:**

- ✅ Dependency graph
- ✅ Dependabot alerts
- ✅ Dependabot security updates

### 6️⃣ **Probar el Pipeline**

```bash
# Crear commit de prueba
git add .
git commit -m "chore: implementar pipeline CI/CD DevSecOps"
git push origin main
```

Ve a **Actions** en GitHub y verifica que el pipeline se ejecute correctamente.

### 7️⃣ **Hacer el Repositorio Público** (cuando esté listo)

**Settings → General → Danger Zone → Change visibility → Make public**

---

## 🎮 Cómo Usar el Pipeline

### Desarrollo Normal (Push a main)
```bash
git add .
git commit -m "feat: nueva funcionalidad"
git push origin main
```
→ Se ejecutan **todos los checks de seguridad** automáticamente
→ **NO** se genera AAB ni se sube a Play Store

### Crear Release (Generar AAB y Subir a Play Store)
```bash
# Opción 1: Desde GitHub UI
1. Ir a "Releases" → "Create a new release"
2. Crear tag (ej: v1.0.7)
3. Generar release notes
4. Publish release

# Opción 2: Desde línea de comandos
git tag v1.0.7
git push origin v1.0.7
```
→ Se ejecutan **todos los checks**
→ Se **genera y firma el AAB**
→ Se **sube a Play Store Internal Testing**
→ **TÚ decides** manualmente cuándo promover a producción

### Ejecución Manual
```bash
# En GitHub: Actions → CI/CD Pipeline → Run workflow
```
→ Control total, ejecutas cuando quieras

---

## 🧪 Ejecutar Checks Localmente (Antes de Push)

```bash
# Instalar dependencias de desarrollo
pip install -r requirements-dev.txt

# Ejecutar TODOS los checks automáticamente
./run-security-checks.sh

# O ejecutar checks individuales:
black --check app/src/main/python/          # Formato
ruff check app/src/main/python/             # Linting
bandit -r app/src/main/python/              # Seguridad Python
./gradlew lintDebug                         # Seguridad Android
./gradlew testDebugUnitTest                 # Tests
```

---

## 📊 Entender los Resultados

### ✅ Todos los Checks Pasan
- Código seguro y listo
- Puedes hacer merge/release con confianza

### ⚠️ Warnings (continue-on-error)
- Revisa los reportes descargables
- Algunos warnings son informativos, no críticos
- Decide si necesitas corregir antes de continuar

### ❌ Checks Fallan
- **Revisa el log** en la pestaña Actions
- **Descarga los reportes** (artifacts)
- **Corrige los problemas** localmente
- **Push de nuevo**

---

## 🔒 Seguridad del Repositorio Público

### ✅ Seguro
- Secretos en GitHub Actions (nunca se exponen)
- IDs de AdMob en código (son públicos por naturaleza)
- Código fuente visible (es tu portfolio)

### ⚠️ Revisar Antes de Hacer Público
- ❌ **NO commitear** archivos `.jks` o `.keystore`
- ❌ **NO commitear** `google-services.json` si contiene claves privadas
- ❌ **NO commitear** contraseñas o tokens en código
- ✅ Usar `.gitignore` para excluir archivos sensibles

### 🛡️ Impedir Contribuciones

**Settings → General → Features:**
- ❌ Issues
- ❌ Pull requests
- ❌ Wiki
- ❌ Discussions

Así el repo es de solo lectura para otros.

---

## 🎯 Flujo de Trabajo Recomendado

```
1. Desarrollar funcionalidad localmente
   ↓
2. Ejecutar ./run-security-checks.sh
   ↓
3. Si pasa → Commit y Push
   ↓
4. Pipeline automático ejecuta checks
   ↓
5. Si todo pasa → Crear Release
   ↓
6. Pipeline genera AAB y sube a Internal Testing
   ↓
7. Probar en Internal Testing
   ↓
8. Promover manualmente a Producción desde Play Console
```

---

## ❓ FAQ

**Q: ¿El pipeline sube automáticamente a producción?**
A: NO. Sube a **Internal Testing**. TÚ promueves manualmente a producción desde Play Console.

**Q: ¿Puedo desactivar el deploy automático?**
A: SÍ. Cambia el workflow para usar solo `workflow_dispatch` (manual).

**Q: ¿Los secretos están seguros en repo público?**
A: SÍ. GitHub nunca expone secretos en logs ni permite acceso a ellos desde forks.

**Q: ¿Qué pasa si un check falla?**
A: El pipeline se detiene. No se genera AAB ni se sube nada. Debes corregir los errores.

**Q: ¿Puedo usar esto en otros proyectos Android?**
A: SÍ. Solo ajusta los paths y nombres de paquete en los workflows.

---

## 📞 Soporte

Si algo falla:
1. Revisa los logs en **Actions → CI/CD Pipeline → Run details**
2. Descarga los **artifacts** (reportes) para análisis detallado
3. Verifica que todos los secretos estén configurados correctamente
4. Asegúrate de que el Service Account tenga permisos en Play Console

---

**🎉 ¡Listo para implementar DevSecOps en tu proyecto!**

