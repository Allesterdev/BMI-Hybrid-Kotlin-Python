#!/bin/bash

# 🔒 Script para gestionar PRs de Dependabot de forma segura
# Este script ayuda a cerrar PRs peligrosos y mergear los seguros

set -e

echo "╔═══════════════════════════════════════════════════════════════════════╗"
echo "║  🔒 Gestor de PRs de Dependabot - Actualización Segura               ║"
echo "╚═══════════════════════════════════════════════════════════════════════╝"
echo ""

# ============================================
# CONFIGURACIÓN
# ============================================
REPO_OWNER="Allesterdev"
REPO_NAME="BMI-Hybrid-Kotlin-Python"

# ============================================
# PRs SEGUROS PARA AUTO-MERGE
# ============================================
SAFE_PRS=(
    "2"   # actions/checkout 4→6
    "1"   # actions/setup-python 5→6
    "5"   # actions/setup-java 4→5
    "7"   # actions/upload-artifact 4→6
    "10"  # actions/download-artifact 4→7
)

# ============================================
# PRs DE RIESGO MEDIO (revisar manualmente)
# ============================================
MEDIUM_RISK_PRS=(
    "14"  # safety 3.2.11→3.7.0
    "11"  # flake8 7.1.1→7.3.0
    "13"  # pytest 8.3.4→9.0.2
    "15"  # pytest-mock 3.14.0→3.15.1
    "4"   # firebase-bom 34.2.0→34.7.0
    "19"  # firebase-crashlytics 20.0.1→20.0.3
    "18"  # play-services-ads 24.5.0→24.9.0
    "8"   # androidx.activity 1.9.3→1.12.1
)

# ============================================
# PRs PELIGROSOS (NO mergear automáticamente)
# ============================================
DANGEROUS_PRS=(
    "20"  # gradle 8.6.0→8.13.2 (PELIGROSO)
    "9"   # chaquopy gradle 16.1.0→17.0.0 (PELIGROSO)
    "6"   # chaquopy 16.1.0→17.0.0 (PELIGROSO)
    "17"  # kotlin.android 2.2.0→2.2.21 (PELIGROSO)
    "16"  # kotlin-gradle-plugin 2.0.0→2.2.21 (PELIGROSO)
    "3"   # android.application 8.11.1→8.13.2 (PELIGROSO)
    "12"  # pandas 2.2.3→2.3.3 (REVISAR)
)

# ============================================
# FUNCIONES
# ============================================

print_header() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "$1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# ============================================
# MAIN
# ============================================

print_header "📊 RESUMEN DE PRs DE DEPENDABOT"

echo "✅ PRs SEGUROS (GitHub Actions): ${#SAFE_PRS[@]}"
echo "🟡 PRs de RIESGO MEDIO: ${#MEDIUM_RISK_PRS[@]}"
echo "❌ PRs PELIGROSOS: ${#DANGEROUS_PRS[@]}"
echo ""

# ============================================
# OPCIÓN 1: LISTAR TODOS
# ============================================

print_header "1️⃣  PRs SEGUROS PARA MERGEAR"
echo "Estos PRs actualizan GitHub Actions y son 100% seguros:"
echo ""
for pr in "${SAFE_PRS[@]}"; do
    echo "  ✅ PR #$pr"
done
echo ""
echo "💡 Acción recomendada: Mergear todos desde GitHub UI"
echo "   https://github.com/$REPO_OWNER/$REPO_NAME/pulls"

# ============================================
print_header "2️⃣  PRs de RIESGO MEDIO"
echo "Revisar changelog, si no hay breaking changes → mergear:"
echo ""
for pr in "${MEDIUM_RISK_PRS[@]}"; do
    echo "  🟡 PR #$pr"
done
echo ""
echo "💡 Acción recomendada: Revisar uno por uno, mergear si checks pasan"

# ============================================
print_header "3️⃣  PRs PELIGROSOS (NO MERGEAR TODAVÍA)"
echo "Estos pueden romper la app. Requieren testing manual:"
echo ""
for pr in "${DANGEROUS_PRS[@]}"; do
    echo "  ❌ PR #$pr"
done
echo ""
echo "💡 Acción recomendada: CERRAR estos PRs (sin mergear)"
echo "   O testear manualmente en rama separada antes de mergear"

# ============================================
print_header "🚀 SIGUIENTE PASO"

cat << 'EOF'
OPCIÓN A: Mergear PRs seguros manualmente
   1. Ir a: https://github.com/Allesterdev/BMI-Hybrid-Kotlin-Python/pulls
   2. Para cada PR seguro (#2, #1, #5, #7, #10):
      - Verificar que checks pasen ✅
      - Click "Merge pull request"
      - Click "Confirm merge"

OPCIÓN B: Cerrar PRs peligrosos
   1. Ir a cada PR peligroso
   2. Añadir comentario: "Postponed for manual testing - see DEPENDENCY-UPDATE-STRATEGY.md"
   3. Click "Close pull request" (SIN mergear)

OPCIÓN C: Configurar Dependabot para evitar PRs peligrosos en el futuro
   1. Ya creamos .github/dependabot.yml
   2. Hacer commit y push de ese archivo
   3. Dependabot dejará de crear PRs peligrosos automáticamente

EOF

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

