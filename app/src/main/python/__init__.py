# Archivo __init__.py para el paquete de funciones IMC
# Permite que Python reconozca este directorio como un paquete

from .funciones_imc_android import (  # noqa: F401
    calcular_imc_adulto,
    calcular_imc_menor_edad,
    calcular_imc_menor_fecha,
    guardar_medicion_db,
    obtener_historial_adultos,
    obtener_historial_menores,
    borrar_historial_adultos,
    borrar_historial_menores,
    borrar_todos_historiales,
    obtener_datos_grafico,
)
