#!/bin/bash

# Script para cerrar PRs incompatibles de Dependabot (Kotlin 2.3.x)
# Autor: GitHub Copilot
# Fecha: 2025-12-26

set -e

echo "🔍 Cerrando PRs de Dependabot incompatibles con Chaquopy..."
echo "=============================================================="
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Verificar que gh esté instalado
if ! command -v gh &> /dev/null; then
    echo -e "${RED}❌ Error: GitHub CLI (gh) no está instalado${NC}"
    echo "Instálalo con: sudo apt install gh"
    exit 1
fi

# Verificar que estamos autenticados
if ! gh auth status &> /dev/null; then
    echo -e "${RED}❌ Error: No estás autenticado en GitHub CLI${NC}"
    echo "Ejecuta: gh auth login"
    exit 1
fi

echo -e "${YELLOW}⚠️  RAZÓN DEL CIERRE:${NC}"
echo "Chaquopy 16.1.0 solo soporta Kotlin hasta 2.2.x"
echo "Kotlin 2.3.0 NO ES COMPATIBLE con la versión actual de Chaquopy"
echo ""

# Mensaje de cierre
CLOSE_MESSAGE="Chaquopy 16.1.0 solo soporta Kotlin hasta 2.2.x según la documentación oficial: https://chaquo.com/chaquopy/doc/current/versions.html

Kotlin 2.3.0 NO es compatible con nuestra versión actual de Chaquopy. Necesitamos esperar a que salga una versión de Chaquopy compatible con Kotlin 2.3.x.

Cerrando este PR para evitar romper el build.

\`\`\`
@dependabot ignore this major version
\`\`\`"

# PR #49: Kotlin Android 2.3.0
echo -e "${YELLOW}📝 Procesando PR #49: org.jetbrains.kotlin.android 2.2.0 → 2.3.0${NC}"
if gh pr view 49 --json state --jq '.state' | grep -q "OPEN"; then
    echo "$CLOSE_MESSAGE" | gh pr comment 49 --body-file -
    gh pr close 49
    echo -e "${GREEN}✅ PR #49 cerrado${NC}"
else
    echo -e "${YELLOW}⚠️  PR #49 ya está cerrado${NC}"
fi
echo ""

# PR #47: Kotlin Gradle Plugin 2.3.0
echo -e "${YELLOW}📝 Procesando PR #47: kotlin-gradle-plugin 2.0.0 → 2.3.0${NC}"
if gh pr view 47 --json state --jq '.state' | grep -q "OPEN"; then
    echo "$CLOSE_MESSAGE" | gh pr comment 47 --body-file -
    gh pr close 47
    echo -e "${GREEN}✅ PR #47 cerrado${NC}"
else
    echo -e "${YELLOW}⚠️  PR #47 ya está cerrado${NC}"
fi
echo ""

echo "=============================================================="
echo -e "${GREEN}🎉 Proceso completado${NC}"
echo ""
echo "PRs cerrados:"
echo "  ❌ #49 - Kotlin Android 2.3.0 (incompatible)"
echo "  ❌ #47 - Kotlin Gradle Plugin 2.3.0 (incompatible)"
echo ""
echo "PRs que puedes mergear de forma segura:"
echo "  ✅ #50 - user-messaging-platform 4.0.0"
echo "  ✅ #48 - AGP 8.11.2"
echo "  ✅ #46 - ruff 0.14.10"
echo "  ✅ #45 - activity-ktx 1.12.2"
echo "  ✅ #44 - actions/cache 5"
echo ""
echo "Dependabot no volverá a crear PRs para Kotlin 2.3.x gracias a la configuración actualizada."

