###
# Archivo principal de la aplicación IMC para Android con Chaquopy
# Este archivo mantiene la interfaz pública para compatibilidad con Kotlin
# Las funciones están organizadas en módulos separados para mejor mantenimiento
###

# Importar todas las funciones de los módulos especializados usando importaciones absolutas
from utilidades import (
    convertir_altura_a_metros,
    convertir_peso_a_float,
    calcular_imc,
    obtener_fecha,
    inicializar_base_de_datos,
    guardar_medicion,
    mostrar_historial,
    borrar_historial_adultos,
    borrar_historial_menores,
    obtener_datos_para_grafico,
    calcular_edad_exacta_en_meses,
    obtener_historial_adultos,
    obtener_historial_menores,
)

from calculos_adultos import interpretar_imc, obtener_rangos_imc, calcular_posicion_en_barra, obtener_categoria_imc

from calculos_menores import (
    cargar_percentiles,
    normal_cdf,
    interpretar_percentil,
    calcular_imc_menor,
    calcular_imc_menor_por_fecha,
    obtener_rangos_percentiles,
    calcular_posicion_en_barra_percentil,
    obtener_categoria_percentil,
    interpretar_percentil_detallado,
)

# Re-exportar todas las funciones para mantener compatibilidad
# Esto asegura que Kotlin pueda seguir llamando las funciones como antes
__all__ = [
    # Utilidades generales
    "convertir_altura_a_metros",
    "convertir_peso_a_float",
    "calcular_imc",
    "obtener_fecha",
    "inicializar_base_de_datos",
    "guardar_medicion",
    "mostrar_historial",
    "borrar_historial_adultos",
    "borrar_historial_menores",
    "obtener_datos_para_grafico",
    "calcular_edad_exacta_en_meses",
    # Funciones para adultos
    "interpretar_imc",
    "obtener_rangos_imc",
    "calcular_posicion_en_barra",
    "obtener_categoria_imc",
    # Funciones para menores
    "cargar_percentiles",
    "normal_cdf",
    "interpretar_percentil",
    "calcular_imc_menor",
    "calcular_imc_menor_por_fecha",
    # Funciones de la barra de percentiles para menores
    "obtener_rangos_percentiles",
    "calcular_posicion_en_barra_percentil",
    "obtener_categoria_percentil",
    "interpretar_percentil_detallado",
    # Funciones de historial separado
    "obtener_historial_adultos",
    "obtener_historial_menores",
]
