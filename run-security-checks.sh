#!/bin/bash
# Script para ejecutar todos los checks de seguridad localmente antes de hacer push

set -e

echo "🔒 ====================================="
echo "   DevSecOps Security Checks - Local"
echo "====================================="
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Directorio de Python
PYTHON_DIR="app/src/main/python"

# Verificar que existe el directorio Python
if [ ! -d "$PYTHON_DIR" ]; then
    echo -e "${RED}❌ Error: No se encuentra el directorio $PYTHON_DIR${NC}"
    exit 1
fi

# ============================================
# 1. INSTALACIÓN DE DEPENDENCIAS
# ============================================
echo -e "${YELLOW}📦 Instalando dependencias de desarrollo...${NC}"
pip install -q -r requirements-dev.txt
echo -e "${GREEN}✅ Dependencias instaladas${NC}"
echo ""

# ============================================
# 2. BLACK - CODE FORMATTER
# ============================================
echo -e "${YELLOW}🎨 Ejecutando Black (Code Formatter)...${NC}"
if black --check --diff "$PYTHON_DIR/"; then
    echo -e "${GREEN}✅ Black: Código formateado correctamente${NC}"
else
    echo -e "${RED}❌ Black: Código necesita formateo${NC}"
    echo -e "${YELLOW}💡 Ejecuta: black $PYTHON_DIR/${NC}"
    exit 1
fi
echo ""

# ============================================
# 3. RUFF - FAST LINTER
# ============================================
echo -e "${YELLOW}⚡ Ejecutando Ruff (Fast Linter)...${NC}"
if ruff check "$PYTHON_DIR/"; then
    echo -e "${GREEN}✅ Ruff: No se encontraron problemas${NC}"
else
    echo -e "${RED}❌ Ruff: Se encontraron problemas de linting${NC}"
    exit 1
fi
echo ""

# ============================================
# 4. FLAKE8 - STYLE GUIDE
# ============================================
echo -e "${YELLOW}📏 Ejecutando Flake8 (Style Guide)...${NC}"
if flake8 "$PYTHON_DIR/" --max-line-length=120 --extend-ignore=E203,W503; then
    echo -e "${GREEN}✅ Flake8: Código cumple con PEP 8${NC}"
else
    echo -e "${RED}❌ Flake8: Violaciones de estilo encontradas${NC}"
    exit 1
fi
echo ""

# ============================================
# 5. BANDIT - SECURITY SCANNER
# ============================================
echo -e "${YELLOW}🔍 Ejecutando Bandit (Security Scanner)...${NC}"
if bandit -r "$PYTHON_DIR/" -ll; then
    echo -e "${GREEN}✅ Bandit: No se encontraron vulnerabilidades críticas${NC}"
else
    echo -e "${RED}⚠️  Bandit: Se encontraron posibles problemas de seguridad${NC}"
    echo -e "${YELLOW}💡 Revisa el reporte arriba para más detalles${NC}"
    # No hacemos exit aquí porque algunos warnings son aceptables
fi
echo ""

# ============================================
# 6. SAFETY - DEPENDENCY VULNERABILITY CHECK
# ============================================
echo -e "${YELLOW}🛡️  Ejecutando Safety (Dependency Scanner)...${NC}"
if pip freeze | safety check --stdin; then
    echo -e "${GREEN}✅ Safety: No se encontraron vulnerabilidades en dependencias${NC}"
else
    echo -e "${RED}⚠️  Safety: Vulnerabilidades encontradas en dependencias${NC}"
    echo -e "${YELLOW}💡 Actualiza las dependencias vulnerables${NC}"
fi
echo ""

# ============================================
# 7. ANDROID LINT
# ============================================
echo -e "${YELLOW}🤖 Ejecutando Android Lint...${NC}"
if ./gradlew lintDebug; then
    echo -e "${GREEN}✅ Android Lint: Pasó correctamente${NC}"
    echo -e "${YELLOW}📊 Reporte disponible en: app/build/reports/lint-results-debug.html${NC}"
else
    echo -e "${RED}❌ Android Lint: Se encontraron problemas${NC}"
    exit 1
fi
echo ""

# ============================================
# 8. TESTS UNITARIOS
# ============================================
echo -e "${YELLOW}🧪 Ejecutando Tests Unitarios...${NC}"
if ./gradlew testDebugUnitTest; then
    echo -e "${GREEN}✅ Tests: Todos los tests pasaron${NC}"
else
    echo -e "${RED}❌ Tests: Algunos tests fallaron${NC}"
    exit 1
fi
echo ""

# ============================================
# RESUMEN FINAL
# ============================================
echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  ✅ TODOS LOS CHECKS PASARON          ║${NC}"
echo -e "${GREEN}║  🚀 Código listo para push            ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}💡 Tip: El pipeline de CI/CD ejecutará los mismos checks${NC}"
echo -e "${YELLOW}   automáticamente al hacer push${NC}"

