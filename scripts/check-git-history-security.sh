#!/bin/bash
# Script para verificar si el repositorio Git actual tiene archivos sensibles en el historial

echo "🔍 ========================================"
echo "   Verificación de Seguridad del Historial Git"
echo "========================================"
echo ""

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

CRITICAL_ISSUES=0
WARNINGS=0
INFO=0

# Verificar que estamos en un repositorio git
if [ ! -d ".git" ]; then
    echo -e "${BLUE}ℹ️  No hay repositorio Git inicializado todavía${NC}"
    echo -e "${GREEN}✅ Puedes usar el repositorio existente o crear uno nuevo${NC}"
    exit 0
fi

echo -e "${BLUE}📂 Analizando historial de Git...${NC}"
echo ""

# 1. Buscar archivos .jks en el historial
echo -e "${YELLOW}🔍 1. Buscando archivos .jks en TODO el historial...${NC}"
JKS_FILES=$(git log --all --full-history --pretty=format: --name-only --diff-filter=A | grep -E '\.jks$|\.keystore$' | sort -u)

if [ -n "$JKS_FILES" ]; then
    echo -e "${RED}❌ CRÍTICO: Archivos .jks encontrados en el historial:${NC}"
    echo "$JKS_FILES" | while read file; do
        echo -e "${RED}   - $file${NC}"
        # Buscar en qué commits aparece
        git log --all --oneline -- "$file" | head -3 | while read commit; do
            echo -e "${YELLOW}     Commit: $commit${NC}"
        done
    done
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
    echo ""
else
    echo -e "${GREEN}✅ No se encontraron archivos .jks en el historial${NC}"
    echo ""
fi

# 2. Buscar google-services.json
echo -e "${YELLOW}🔍 2. Buscando google-services.json en el historial...${NC}"
GOOGLE_SERVICES=$(git log --all --full-history --pretty=format: --name-only --diff-filter=A | grep -E 'google-services\.json$' | sort -u)

if [ -n "$GOOGLE_SERVICES" ]; then
    echo -e "${YELLOW}⚠️  google-services.json encontrado en el historial${NC}"
    echo -e "${BLUE}   Verifica si contiene claves reales de producción${NC}"
    WARNINGS=$((WARNINGS + 1))
    echo ""
else
    echo -e "${GREEN}✅ No se encontró google-services.json en el historial${NC}"
    echo ""
fi

# 3. Buscar archivos con "password" o "secret" en el nombre
echo -e "${YELLOW}🔍 3. Buscando archivos con nombres sospechosos...${NC}"
SENSITIVE_NAMES=$(git log --all --full-history --pretty=format: --name-only --diff-filter=A | grep -iE '(password|secret|key|credential|token)' | grep -vE '\.(md|txt|sh|gradle|properties)$' | sort -u)

if [ -n "$SENSITIVE_NAMES" ]; then
    echo -e "${YELLOW}⚠️  Archivos con nombres sospechosos:${NC}"
    echo "$SENSITIVE_NAMES" | while read file; do
        echo -e "${YELLOW}   - $file${NC}"
    done
    WARNINGS=$((WARNINGS + 1))
    echo ""
else
    echo -e "${GREEN}✅ No se encontraron archivos con nombres sospechosos${NC}"
    echo ""
fi

# 4. Buscar strings de contraseñas hardcodeadas en commits
echo -e "${YELLOW}🔍 4. Buscando contraseñas hardcodeadas en commits (últimos 50 commits)...${NC}"
PASSWORD_COMMITS=$(git log --all -50 -p | grep -i password | grep -v 'buildConfigField' | wc -l)

if [ "$PASSWORD_COMMITS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Se encontraron $PASSWORD_COMMITS líneas potencialmente sospechosas${NC}"
    echo -e "${BLUE}   Revisa manualmente con: git log --all -p | grep -i password${NC}"
    WARNINGS=$((WARNINGS + 1))
    echo ""
else
    echo -e "${GREEN}✅ No se encontraron contraseñas hardcodeadas obvias${NC}"
    echo ""
fi

# 5. Verificar si hay archivos sensibles en el staging area
echo -e "${YELLOW}🔍 5. Verificando área de staging actual...${NC}"
STAGED_SENSITIVE=$(git diff --cached --name-only | grep -E '\.jks$|\.keystore$|google-services\.json|TUS-SECRETOS\.md')

if [ -n "$STAGED_SENSITIVE" ]; then
    echo -e "${RED}❌ CRÍTICO: Archivos sensibles en staging:${NC}"
    echo "$STAGED_SENSITIVE" | while read file; do
        echo -e "${RED}   - $file${NC}"
    done
    echo -e "${YELLOW}   Ejecuta: git reset HEAD <archivo>${NC}"
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
    echo ""
else
    echo -e "${GREEN}✅ No hay archivos sensibles en staging${NC}"
    echo ""
fi

# 6. Verificar tamaño del historial
echo -e "${YELLOW}🔍 6. Analizando tamaño del historial...${NC}"
COMMIT_COUNT=$(git rev-list --all --count)
REPO_SIZE=$(du -sh .git 2>/dev/null | cut -f1)

echo -e "${BLUE}   Total de commits: $COMMIT_COUNT${NC}"
echo -e "${BLUE}   Tamaño del .git: $REPO_SIZE${NC}"

if [ "$COMMIT_COUNT" -gt 100 ]; then
    echo -e "${BLUE}   ℹ️  Historial extenso - considera si vale la pena mantenerlo${NC}"
    INFO=$((INFO + 1))
fi
echo ""

# 7. Verificar remote actual
echo -e "${YELLOW}🔍 7. Verificando remote de GitHub...${NC}"
if git remote -v | grep -q "origin"; then
    REMOTE_URL=$(git remote get-url origin)
    echo -e "${BLUE}   Remote actual: $REMOTE_URL${NC}"

    # Verificar si es un repo existente en GitHub
    if echo "$REMOTE_URL" | grep -q "github.com"; then
        echo -e "${YELLOW}   ⚠️  Ya hay un repositorio configurado en GitHub${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${GREEN}   ℹ️  No hay remote configurado todavía${NC}"
fi
echo ""

# RESUMEN Y RECOMENDACIÓN
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BOLD}                 RESUMEN Y RECOMENDACIÓN${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ $CRITICAL_ISSUES -gt 0 ]; then
    echo -e "${RED}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ⛔ RECOMENDACIÓN: CREAR NUEVO REPOSITORIO    ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${RED}❌ Problemas críticos encontrados: $CRITICAL_ISSUES${NC}"
    echo -e "${YELLOW}⚠️  Advertencias: $WARNINGS${NC}"
    echo ""
    echo -e "${RED}El historial contiene archivos sensibles que NO se pueden borrar.${NC}"
    echo -e "${RED}Aunque los borres ahora, siguen en commits anteriores.${NC}"
    echo ""
    echo -e "${YELLOW}🔧 SOLUCIÓN:${NC}"
    echo -e "   1. Crear nuevo repositorio limpio en GitHub"
    echo -e "   2. Eliminar el .git actual: ${YELLOW}mv .git .git-OLD-BACKUP${NC}"
    echo -e "   3. Inicializar nuevo: ${YELLOW}git init${NC}"
    echo -e "   4. Conectar al nuevo repo y hacer push"
    echo ""
    echo -e "${BLUE}📖 Ver: STEP3-GITHUB-SECRETS.md → Sección 'Crear Nuevo Repositorio'${NC}"
    exit 1

elif [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  ⚠️  RECOMENDACIÓN: EVALUAR EL RIESGO         ║${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}⚠️  Advertencias encontradas: $WARNINGS${NC}"
    echo ""
    echo -e "${YELLOW}Revisa los hallazgos arriba y decide:${NC}"
    echo ""
    echo -e "${BLUE}Opción A: Usar repositorio existente${NC}"
    echo -e "   • Si las advertencias son menores"
    echo -e "   • Si el repo siempre fue privado"
    echo -e "   • Si confías en el historial"
    echo ""
    echo -e "${BLUE}Opción B: Crear nuevo repositorio${NC}"
    echo -e "   • Si tienes dudas razonables"
    echo -e "   • Si prefieres empezar limpio"
    echo -e "   • Si la seguridad es crítica"
    echo ""
    echo -e "${GREEN}💡 En caso de duda, siempre es mejor crear uno nuevo.${NC}"
    exit 0

else
    echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✅ RECOMENDACIÓN: USAR REPOSITORIO EXISTENTE ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}✅ No se encontraron problemas de seguridad${NC}"
    echo ""
    echo -e "${GREEN}El historial está limpio. Puedes:${NC}"
    echo -e "   1. Usar el repositorio existente"
    echo -e "   2. Añadir los archivos del pipeline DevSecOps"
    echo -e "   3. Configurar los secretos en GitHub"
    echo -e "   4. Hacer push"
    echo ""
    echo -e "${BLUE}📖 Continúa con: STEP3-GITHUB-SECRETS.md${NC}"
    exit 0
fi

