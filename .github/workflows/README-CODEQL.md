# CodeQL Analysis - Desactivado Temporalmente

## ⚠️ Estado: Desactivado

Este workflow está desactivado porque **CodeQL requiere**:

1. **Repositorio público**, O
2. **GitHub Advanced Security** (solo disponible en planes GitHub Enterprise)

## 📋 Para Activar CodeQL:

### Opción 1: Hacer el Repositorio Público
```bash
# 1. Ir a: Settings → General → Danger Zone
# 2. Change visibility → Make public
# 3. Renombrar el archivo:
mv .github/workflows/codeql-analysis.yml.disabled .github/workflows/codeql-analysis.yml
git add .github/workflows/
git commit -m "feat: activar CodeQL después de hacer repo público"
git push
```

### Opción 2: GitHub Advanced Security (Empresas)
Si tienes acceso a GitHub Enterprise:
```bash
# 1. Habilitar Advanced Security en Settings → Code security
# 2. Renombrar el archivo como en Opción 1
```

## ✅ Herramientas de Seguridad Activas

Mientras tanto, estas herramientas SÍ están activas:

### 🐍 Python:
- ✅ Bandit (security scanner)
- ✅ Black (formatter)
- ✅ Ruff (linter + security)
- ✅ Flake8 (style)
- ✅ Safety (CVE scanner)

### 🤖 Android:
- ✅ Android Lint (security + quality)
- ✅ Dependabot (dependency updates)

### Total: 7 herramientas activas

## 📊 Cobertura Sin CodeQL

Bandit + Ruff ya cubren la mayoría de vulnerabilidades que CodeQL detectaría en Python.
Android Lint cubre muchas vulnerabilidades en Java/Kotlin.

**CodeQL añadiría:** Análisis de flujo de datos más profundo, pero no es crítico para el portfolio.

## 🎯 Cuándo Activarlo

Activa CodeQL cuando:
- ✅ Hagas el repositorio público (gratis)
- ✅ Obtengas GitHub Enterprise (corporativo)

Hasta entonces, las 7 herramientas activas son suficientes para demostrar DevSecOps.

