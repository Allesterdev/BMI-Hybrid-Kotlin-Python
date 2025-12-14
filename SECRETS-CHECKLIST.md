# 🔐 Checklist de GitHub Secrets

## ✅ Secretos Ya Configurados

- [x] `KEYSTORE_FILE` - Keystore en Base64 para firma de app
- [x] `KEYSTORE_PASSWORD` - Contraseña del keystore
- [x] `KEY_ALIAS` - Alias de la clave de firma
- [x] `KEY_PASSWORD` - Contraseña de la clave
- [x] `NVD_API_KEY` - API key para OWASP Dependency Check
- [x] `GOOGLE_SERVICES_JSON` - Configuración de Firebase ✅ CONFIGURADO

## ✅ Secretos Adicionales Configurados

### 🎯 AdMob IDs de Producción (4 secretos) ✅ CONFIGURADOS

- [x] `ADMOB_APP_ID_RELEASE` ✅
- [x] `ADMOB_INTERSTITIAL_ID_RELEASE` ✅
- [x] `ADMOB_NATIVE_ADULTOS_ID_RELEASE` ✅
- [x] `ADMOB_NATIVE_MENORES_ID_RELEASE` ✅

**Estado:** Todos los IDs de AdMob ahora están **protegidos** y se inyectan desde GitHub Secrets en el build de release.

**Beneficios:**
- ✅ IDs de monetización protegidos contra clonación
- ✅ Protección contra click fraud dirigido
- ✅ Cumple con mejores prácticas DevSecOps
- ✅ Datos sensibles del negocio asegurados

---

### google-services.json ✅ RECUPERADO
**El archivo ya está presente localmente** (recuperado del historial de Git).

Si necesitas recuperarlo nuevamente en el futuro:

```bash
# Opción 1: Recuperar del historial de Git
git show 8a7ba2f:app/google-services.json > app/google-services.json

# Opción 2: Descargar desde Firebase Console
# 1. Ir a: https://console.firebase.google.com/project/imc-practico/settings/general
# 2. Scroll down → "Tus apps" → Android
# 3. Click en "google-services.json" para descargar
# 4. Mover a app/google-services.json
```

**Importante:** 
- ✅ El archivo está en `.gitignore` - NO se subirá al repo
- ✅ En CI/CD se genera automáticamente desde el secret
- ✅ Ya puedes compilar localmente sin problemas

### local.properties
**Ya está configurado correctamente** ✅

Solo necesitas:
```properties
sdk.dir=/home/tu-usuario/Android/Sdk
```

**Opcional:** Para ejecutar OWASP Dependency Check localmente más rápido:
```properties
nvdApiKey=TU_NVD_API_KEY_AQUI
```

---

## 📊 Resumen Final

| Secret | Estado | Necesario Para |
|--------|--------|----------------|
| `KEYSTORE_FILE` | ✅ | Firmar AAB |
| `KEYSTORE_PASSWORD` | ✅ | Firmar AAB |
| `KEY_ALIAS` | ✅ | Firmar AAB |
| `KEY_PASSWORD` | ✅ | Firmar AAB |
| `NVD_API_KEY` | ✅ | OWASP rápido (3min vs 60min) |
| `GOOGLE_SERVICES_JSON` | ✅ | Firebase en CI/CD |
| `ADMOB_APP_ID_RELEASE` | ✅ | Proteger IDs de monetización |
| `ADMOB_INTERSTITIAL_ID_RELEASE` | ✅ | Proteger IDs de monetización |
| `ADMOB_NATIVE_ADULTOS_ID_RELEASE` | ✅ | Proteger IDs de monetización |
| `ADMOB_NATIVE_MENORES_ID_RELEASE` | ✅ | Proteger IDs de monetización |
| `PLAY_STORE_JSON` | ⚪ | Deploy automático (OPCIONAL) |

**✅ Configurados: 10 secrets críticos**
**⚪ Opcionales: 1 secret (PLAY_STORE_JSON)**

**🎉 TODOS LOS SECRETOS CRÍTICOS ESTÁN CONFIGURADOS**

---

## ⚪ Secret Opcional: PLAY_STORE_JSON

### ¿Necesitas configurarlo?
**NO es obligatorio.** Solo si quieres deploy automático a Play Store.

### ¿Qué hace?
Sube el AAB automáticamente a Play Store (Internal Testing) cuando creas un release en GitHub.

### Si NO lo configuras:
- ✅ El AAB firmado se genera correctamente
- ✅ Puedes descargarlo de GitHub Actions → Artifacts → `signed-aab`
- ✅ Lo subes manualmente a Play Console (como siempre has hecho)
- ⚠️ El job `deploy-to-play-store` fallará (pero NO afecta el build del AAB)

### Si SÍ quieres configurarlo:
1. Ir a [Google Play Console](https://play.google.com/console)
2. Setup → API Access
3. Crear nueva Service Account (o usar existente)
4. Descargar el archivo JSON
5. Darle permisos "Release manager" o "Admin"
6. GitHub → Settings → Secrets → New repository secret
7. Name: `PLAY_STORE_JSON`
8. Value: Pegar el contenido completo del JSON
9. Click "Add secret"

### Recomendación:
**Puedes dejarlo para después.** Primero verifica que todo el pipeline funcione correctamente con los 10 secrets que ya tienes.

---

## 🚀 Verificar Configuración

```bash
# 1. Asegurar que tienes google-services.json localmente
ls -la app/google-services.json

# 2. Hacer commit de los cambios
git add .
git commit -m "security: configurar google-services.json en secrets"

# 3. Push y verificar pipeline
git push origin main

# 4. Ver en GitHub → Actions que todo funcione
```

**¿El build local no funciona sin google-services.json?** Descárgalo de Firebase Console y ponlo en `app/` (no lo commitees).

