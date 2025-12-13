# ✅ Checklist de Implementación DevSecOps

## 📋 Pre-Implementación

- [ ] **Leer toda la documentación**
  - [ ] QUICKSTART.md
  - [ ] SECURITY.md
  - [ ] TOOLS_SUMMARY.md

## 🔐 Configuración de Secretos

### Keystore de Firma
- [ ] Generar o localizar keystore (.jks)
- [ ] Convertir keystore a Base64
- [ ] Guardar `KEYSTORE_FILE` en GitHub Secrets
- [ ] Guardar `KEYSTORE_PASSWORD` en GitHub Secrets
- [ ] Guardar `KEY_ALIAS` en GitHub Secrets
- [ ] Guardar `KEY_PASSWORD` en GitHub Secrets
- [ ] **VERIFICAR:** Keystore NO está en el repositorio (.gitignore)

### Google Play Service Account
- [ ] Crear proyecto en Google Cloud Console
- [ ] Crear Service Account con permisos adecuados
- [ ] Descargar JSON del Service Account
- [ ] Configurar permisos en Google Play Console
- [ ] Guardar `PLAY_STORE_JSON` en GitHub Secrets
- [ ] **VERIFICAR:** JSON NO está en el repositorio (.gitignore)

## 🛡️ Configuración de Seguridad del Repo

### Protección de Ramas
- [ ] Activar branch protection para `main`
- [ ] Requiere PR antes de merge
- [ ] Requiere status checks:
  - [ ] python-security-scan
  - [ ] android-security-scan
  - [ ] unit-tests
- [ ] No permitir bypass (ni siquiera admin)
- [ ] Restringir push solo a tu usuario

### Características del Repositorio
- [ ] Desactivar Issues (si no quieres contribuciones)
- [ ] Desactivar Pull Requests (si no quieres contribuciones)
- [ ] Desactivar Wiki
- [ ] Desactivar Discussions

### Code Security
- [ ] Activar Dependency graph
- [ ] Activar Dependabot alerts
- [ ] Activar Dependabot security updates
- [ ] Activar Grouped security updates
- [ ] Activar Code scanning (CodeQL) si disponible

## 🧪 Verificación Local

### Instalación de Herramientas
- [ ] Instalar Python 3.10
- [ ] Instalar dependencias: `pip install -r requirements-dev.txt`
- [ ] Verificar que `./gradlew` funciona
- [ ] Dar permisos al script: `chmod +x run-security-checks.sh`

### Ejecutar Checks Localmente
- [ ] `black --check app/src/main/python/` ✅
- [ ] `ruff check app/src/main/python/` ✅
- [ ] `bandit -r app/src/main/python/` ✅
- [ ] `flake8 app/src/main/python/` ✅
- [ ] `./gradlew lintDebug` ✅
- [ ] `./gradlew testDebugUnitTest` ✅
- [ ] **O ejecutar todo:** `./run-security-checks.sh` ✅

## 📤 Primer Push

### Preparar Repositorio
- [ ] **Repositorio creado como PRIVADO**
- [ ] Git remote configurado
- [ ] `.gitignore` actualizado
- [ ] Verificar que NO hay archivos sensibles:
  - [ ] NO hay .jks o .keystore
  - [ ] NO hay *-base64.txt
  - [ ] NO hay service-account*.json
  - [ ] google-services.json está en .gitignore si es necesario

### Commit Inicial
```bash
git add .
git commit -m "feat: implementar pipeline DevSecOps completo"
git push origin main
```

- [ ] Push realizado exitosamente
- [ ] Ir a **Actions** en GitHub
- [ ] Verificar que el pipeline se ejecuta
- [ ] **ESPERAR** a que todos los jobs terminen

## ✅ Verificación del Pipeline

### Jobs que deben pasar
- [ ] ✅ Python Security Scan (Bandit, Black, Ruff, Flake8, Safety)
- [ ] ✅ Android Security Scan (Lint, OWASP Dependency Check)
- [ ] ✅ Unit Tests
- [ ] ✅ Notify Results

### Si algo falla
- [ ] Descargar artifacts (reportes)
- [ ] Revisar logs del job que falló
- [ ] Corregir errores localmente
- [ ] Ejecutar `./run-security-checks.sh` de nuevo
- [ ] Push de nuevo

## 🚀 Primer Release

### Preparación
- [ ] Todos los checks pasan ✅
- [ ] Versión actualizada en `app/build.gradle`:
  - [ ] `versionCode` incrementado
  - [ ] `versionName` actualizado
- [ ] Commit de la versión:
  ```bash
  git add app/build.gradle
  git commit -m "chore: bump version to X.X.X"
  git push origin main
  ```

### Crear Release
- [ ] Ir a Releases → "Create a new release"
- [ ] Crear tag (ej: v1.0.7)
- [ ] Título: "Release v1.0.7"
- [ ] Descripción con cambios
- [ ] Publish release

### Verificar Build
- [ ] Ir a Actions → CI/CD Pipeline
- [ ] Verificar que se ejecuta el workflow del release
- [ ] Jobs adicionales deben ejecutarse:
  - [ ] ✅ Build Release (genera AAB firmado)
  - [ ] ✅ Deploy to Play Store (sube a Internal Testing)

### Descargar Artifacts
- [ ] Descargar `signed-aab` (el AAB firmado)
- [ ] Descargar `release-notes` 
- [ ] Descargar reportes de seguridad

## 🎮 Verificar en Play Console

- [ ] Ir a [Google Play Console](https://play.google.com/console)
- [ ] Seleccionar tu app
- [ ] Ir a Release → Testing → Internal testing
- [ ] Verificar que hay una nueva versión
- [ ] Descargar y probar en dispositivo de prueba
- [ ] Si todo está bien, promover a Producción (manualmente)

## 🌐 Hacer Repositorio Público

### Solo cuando TODO lo anterior esté listo
- [ ] Todos los secretos configurados ✅
- [ ] Pipeline funcionando perfectamente ✅
- [ ] Al menos 1 release exitoso ✅
- [ ] Protecciones de rama activas ✅

### Hacer público
- [ ] Settings → General → Danger Zone
- [ ] "Change visibility" → "Make public"
- [ ] Confirmar con contraseña
- [ ] **VERIFICAR:** Los secretos NO son visibles (nunca lo son)

### Post-Publicación
- [ ] Actualizar badges en README.md con URLs reales
- [ ] Actualizar enlaces de GitHub/LinkedIn
- [ ] Compartir en portfolio
- [ ] (Opcional) Archivar el repo si no quieres más cambios

## 📊 Mantenimiento Continuo

### Semanal
- [ ] Revisar Dependabot PRs
- [ ] Verificar CodeQL Security Alerts
- [ ] Revisar logs de Crashlytics

### Por Release
- [ ] Incrementar versionCode y versionName
- [ ] Crear tag y release en GitHub
- [ ] Verificar que el AAB se genera y firma correctamente
- [ ] Probar en Internal Testing
- [ ] Promover a producción manualmente

### Dependabot PRs
- [ ] Revisar el changelog de la dependencia
- [ ] Verificar que los tests pasan
- [ ] Mergear si todo está bien
- [ ] Dependabot mantendrá las dependencias actualizadas automáticamente

## 🆘 Troubleshooting

### Pipeline falla en Python Security Scan
- Ejecutar localmente: `./run-security-checks.sh`
- Corregir errores de formato con: `black app/src/main/python/`
- Corregir linting con las sugerencias de Ruff

### Pipeline falla en Android Security Scan
- Ejecutar localmente: `./gradlew lintDebug`
- Abrir reporte HTML: `app/build/reports/lint-results-debug.html`
- Corregir problemas indicados

### Build Release falla
- Verificar que todos los secretos estén configurados
- Verificar que el keystore sea válido
- Verificar que las contraseñas sean correctas

### Deploy a Play Store falla
- Verificar que el Service Account tenga permisos
- Verificar que el JSON sea válido
- Verificar que el packageName sea correcto
- Verificar que ya exista una versión en Play Console

## ✅ Lista de Verificación Final

Antes de considerar el proyecto completo:

- [ ] ✅ Pipeline CI/CD funcionando
- [ ] ✅ Todos los checks de seguridad pasan
- [ ] ✅ AAB se genera y firma correctamente
- [ ] ✅ Deploy a Play Store funciona
- [ ] ✅ Repositorio público (si lo deseas)
- [ ] ✅ Protecciones configuradas
- [ ] ✅ Documentación completa
- [ ] ✅ README con badges actualizados
- [ ] ✅ No hay secretos expuestos

---

## 🎉 ¡PROYECTO DEVSECOPLS COMPLETO!

Tu aplicación ahora tiene:
- 🔒 Seguridad de nivel enterprise
- 🚀 CI/CD automatizado
- 📊 Monitoreo continuo de vulnerabilidades
- ✅ Calidad de código garantizada
- 🎯 Listo para portfolio profesional

---

**Fecha de implementación:** _____________
**Última verificación:** _____________
**Estado:** 🟢 Operacional | 🟡 En progreso | 🔴 Pendiente

