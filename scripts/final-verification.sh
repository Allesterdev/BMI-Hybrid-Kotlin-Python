#!/bin/bash
# Verificación final antes de hacer push - Versión completa

echo "🔍 ========================================"
echo "   VERIFICACIÓN FINAL PRE-PUSH"
echo "========================================"
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

ERRORS=0
WARNINGS=0

# 1. Verificar archivos sensibles que NO deben subirse
echo -e "${BLUE}📂 Verificando archivos sensibles...${NC}"
echo ""

SENSITIVE_PATTERNS=(
    "*.jks"
    "*.keystore"
    "keystore-base64.txt"
    "TUS-SECRETOS.md"
    "REFERENCIA-SECRETOS.md"
    "STEP2-KEYSTORE.md"
    "STEP3-GITHUB-SECRETS.md"
    "QUE-SUBIR-AL-REPO.md"
    "scripts/generate-keystore.sh"
    "scripts/convert-keystore-to-base64.sh"
    "scripts/convert-existing-keystore.sh"
    "scripts/show-secrets-for-github.sh"
)

for pattern in "${SENSITIVE_PATTERNS[@]}"; do
    if git ls-files --cached | grep -q "$pattern"; then
        echo -e "${RED}❌ PELIGRO: $pattern está en staging${NC}"
        ERRORS=$((ERRORS + 1))
    fi
done

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ No hay archivos sensibles en staging${NC}"
fi
echo ""

# 2. Listar archivos que SÍ se van a subir
echo -e "${BLUE}📋 Archivos que SE SUBIRÁN:${NC}"
echo ""
echo -e "${GREEN}Documentación para reclutadores:${NC}"
ls -1 *.md 2>/dev/null | grep -v -E "(TUS-SECRETOS|REFERENCIA-SECRETOS|STEP2|STEP3|QUE-SUBIR)" | while read file; do
    echo -e "  ${GREEN}✓${NC} $file"
done

echo ""
echo -e "${GREEN}Configuración del pipeline:${NC}"
find .github -name "*.yml" 2>/dev/null | while read file; do
    echo -e "  ${GREEN}✓${NC} $file"
done

echo ""
echo -e "${GREEN}Configuración Python:${NC}"
for file in pyproject.toml .flake8 requirements-dev.txt; do
    if [ -f "$file" ]; then
        echo -e "  ${GREEN}✓${NC} $file"
    fi
done

echo ""
echo -e "${GREEN}Scripts públicos:${NC}"
ls -1 scripts/*.sh 2>/dev/null | grep -v -E "(generate-keystore|convert.*keystore|show-secrets)" | while read file; do
    echo -e "  ${GREEN}✓${NC} $file"
done

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 3. Mostrar resumen
echo ""
echo -e "${BOLD}📊 RESUMEN:${NC}"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✅ TODO CORRECTO - LISTO PARA PUSH           ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}Los reclutadores verán:${NC}"
    echo -e "  ${GREEN}✓${NC} Pipeline CI/CD profesional"
    echo -e "  ${GREEN}✓${NC} Documentación DevSecOps completa"
    echo -e "  ${GREEN}✓${NC} 9 herramientas de seguridad"
    echo -e "  ${GREEN}✓${NC} Scripts de verificación"
    echo -e "  ${GREEN}✓${NC} Código Android + Python"
    echo ""
    echo -e "${YELLOW}Los reclutadores NO verán:${NC}"
    echo -e "  ${RED}✗${NC} Tus keystores o certificados"
    echo -e "  ${RED}✗${NC} Guías internas de setup"
    echo -e "  ${RED}✗${NC} Scripts para generar certificados"
    echo -e "  ${RED}✗${NC} Información sensible"
    echo ""
    echo -e "${GREEN}🎯 Esto es PERFECTO para un portfolio profesional${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ❌ ERRORES - NO HACER PUSH                   ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${RED}Errores encontrados: $ERRORS${NC}"
    echo ""
    echo -e "${YELLOW}Elimina los archivos sensibles del staging:${NC}"
    echo -e "  ${BLUE}git reset HEAD <archivo>${NC}"
    echo ""
    exit 1
fi

