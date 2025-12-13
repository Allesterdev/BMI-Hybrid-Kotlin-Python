#!/bin/bash
# Script de verificación antes de hacer push a GitHub

echo "🔍 ========================================"
echo "   Verificación Pre-Push"
echo "========================================"
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# 1. Verificar que no hay archivos sensibles
echo -e "${BLUE}📂 Verificando archivos sensibles...${NC}"

SENSITIVE_FILES=(
    "*.jks"
    "*.keystore"
    "*keystore*.jks"
    "keystore-base64.txt"
    "TUS-SECRETOS.md"
    "service-account*.json"
)

for pattern in "${SENSITIVE_FILES[@]}"; do
    if git ls-files | grep -q "$pattern"; then
        echo -e "${RED}❌ PELIGRO: Archivo sensible detectado: $pattern${NC}"
        ERRORS=$((ERRORS + 1))
    fi
done

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ No hay archivos sensibles en staging${NC}"
fi
echo ""

# 2. Verificar que existe .gitignore
echo -e "${BLUE}📄 Verificando .gitignore...${NC}"
if [ -f ".gitignore" ]; then
    echo -e "${GREEN}✅ .gitignore existe${NC}"

    # Verificar que contiene las protecciones necesarias
    if grep -q "*.jks" .gitignore && grep -q "TUS-SECRETOS.md" .gitignore; then
        echo -e "${GREEN}✅ .gitignore contiene protecciones necesarias${NC}"
    else
        echo -e "${YELLOW}⚠️  .gitignore podría necesitar más protecciones${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${RED}❌ .gitignore no existe${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 3. Verificar que existen los workflows
echo -e "${BLUE}🔧 Verificando workflows...${NC}"
if [ -f ".github/workflows/ci-cd-pipeline.yml" ]; then
    echo -e "${GREEN}✅ Pipeline CI/CD existe${NC}"
else
    echo -e "${RED}❌ Pipeline CI/CD no encontrado${NC}"
    ERRORS=$((ERRORS + 1))
fi

if [ -f ".github/workflows/codeql-analysis.yml" ]; then
    echo -e "${GREEN}✅ CodeQL workflow existe${NC}"
else
    echo -e "${YELLOW}⚠️  CodeQL workflow no encontrado${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 4. Verificar archivos de documentación
echo -e "${BLUE}📚 Verificando documentación...${NC}"
DOCS=(
    "README.md"
    "SECURITY.md"
    "QUICKSTART.md"
    "TOOLS_SUMMARY.md"
    "CHECKLIST.md"
)

for doc in "${DOCS[@]}"; do
    if [ -f "$doc" ]; then
        echo -e "${GREEN}✅ $doc existe${NC}"
    else
        echo -e "${YELLOW}⚠️  $doc no encontrado${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
done
echo ""

# 5. Verificar que existe el código Python
echo -e "${BLUE}🐍 Verificando código Python...${NC}"
if [ -d "app/src/main/python" ]; then
    PYTHON_FILES=$(find app/src/main/python -name "*.py" | wc -l)
    echo -e "${GREEN}✅ Código Python encontrado ($PYTHON_FILES archivos)${NC}"
else
    echo -e "${RED}❌ Directorio Python no encontrado${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 6. Verificar que existe pyproject.toml
echo -e "${BLUE}⚙️  Verificando configuración Python...${NC}"
if [ -f "pyproject.toml" ]; then
    echo -e "${GREEN}✅ pyproject.toml existe${NC}"
else
    echo -e "${YELLOW}⚠️  pyproject.toml no encontrado${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 7. Verificar remote de git
echo -e "${BLUE}🔗 Verificando remote de Git...${NC}"
if git remote -v | grep -q "origin"; then
    REMOTE_URL=$(git remote get-url origin)
    echo -e "${GREEN}✅ Remote configurado: $REMOTE_URL${NC}"
else
    echo -e "${YELLOW}⚠️  No hay remote configurado${NC}"
    echo -e "${BLUE}💡 Configúralo con:${NC}"
    echo -e "   ${YELLOW}git remote add origin https://github.com/TUUSUARIO/CalculadoraIMC2.git${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 8. Verificar branch
echo -e "${BLUE}🌿 Verificando branch...${NC}"
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "master" ]; then
    echo -e "${GREEN}✅ En branch principal: $CURRENT_BRANCH${NC}"
else
    echo -e "${YELLOW}⚠️  En branch: $CURRENT_BRANCH${NC}"
    echo -e "${BLUE}💡 Considera cambiar a main:${NC}"
    echo -e "   ${YELLOW}git checkout -b main${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Resumen
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}                    RESUMEN${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✅ TODO PERFECTO - LISTO PARA PUSH           ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}📋 Siguiente paso:${NC}"
    echo -e "   1. Asegúrate de que los secretos estén en GitHub"
    echo -e "   2. Ejecuta: ${YELLOW}git add .${NC}"
    echo -e "   3. Ejecuta: ${YELLOW}git commit -m 'feat: implementar DevSecOps'${NC}"
    echo -e "   4. Ejecuta: ${YELLOW}git push -u origin main${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  ⚠️  HAY ADVERTENCIAS                         ║${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
    echo -e "${BLUE}Puedes continuar, pero revisa las advertencias arriba${NC}"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ❌ ERRORES DETECTADOS                        ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${RED}Errores: $ERRORS${NC}"
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
    echo ""
    echo -e "${RED}⚠️  NO HAGAS PUSH HASTA CORREGIR LOS ERRORES${NC}"
    exit 1
fi

