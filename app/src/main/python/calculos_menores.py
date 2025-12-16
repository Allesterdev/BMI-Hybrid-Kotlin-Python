###
# Módulo específico para cálculos de IMC en menores (5-19 años)
# Incluye cálculo de percentiles usando tablas de la OMS
###

import io
import pandas as pd
import math
from java import jclass
from utilidades import convertir_peso_a_float, convertir_altura_a_metros, calcular_imc, calcular_edad_exacta_en_meses


def cargar_percentiles(sexo):
    """
    Carga la tabla de percentiles para el sexo dado desde un archivo CSV
    almacenado en los assets de la aplicación usando Chaquopy.
    """
    nombre_archivo = (
        'percentiles_imc_niños.csv' if sexo == 'Masculino' else
        'percentiles_imc_niñas.csv'
    )
    try:
        context = jclass("android.app.ActivityThread").currentApplication()
        asset_manager = context.getAssets()
        input_stream = asset_manager.open(nombre_archivo)

        # Leer el contenido usando Java BufferedReader
        BufferedReader = jclass("java.io.BufferedReader")
        InputStreamReader = jclass("java.io.InputStreamReader")

        reader = BufferedReader(InputStreamReader(input_stream, "UTF-8"))
        contenido = ""
        linea = reader.readLine()
        while linea is not None:
            contenido += linea + "\n"
            linea = reader.readLine()

        reader.close()
        input_stream.close()

        df = pd.read_csv(io.StringIO(contenido), sep=';', decimal=',')
        return df
    except Exception as e:
        print(f"Error al cargar el archivo CSV '{nombre_archivo}': {e}")
        return None


def normal_cdf(x, mu=0, sigma=1):
    """Función de distribución acumulada normal estándar."""
    return 0.5 * (1 + math.erf((x - mu) / (sigma * math.sqrt(2))))


def interpretar_percentil(percentil):
    """Interpreta el percentil de IMC y devuelve una clave de recurso (no texto)."""
    if percentil < 3:
        return "interpretacion_bajo_peso"
    elif percentil < 85:
        return "interpretacion_peso_saludable"
    elif percentil < 97:
        return "interpretacion_sobrepeso"
    else:
        return "interpretacion_obesidad"


def calcular_imc_menor(sexo: str, edad_input, peso_input, altura_input) -> dict:  # noqa: C901
    """
    Calcula el percentil de IMC para un menor usando las tablas de la OMS.
    Acepta múltiples formatos para peso y altura, y convierte edad a meses automáticamente.

    Args:
        sexo: "Masculino" o "Femenino"
        edad_input: Edad en años (int, float, string)
        peso_input: Peso en kg (acepta formatos variados)
        altura_input: Altura en m o cm (acepta formatos variados)

    Returns:
        dict: {"imc": float, "percentil": float, "interpretacion": str} o {"error": str}
    """
    try:
        # Validar sexo
        if sexo not in ["Masculino", "Femenino"]:
            return {"error": "Sexo debe ser 'Masculino' o 'Femenino'"}

        # Convertir y validar edad
        try:
            if isinstance(edad_input, str):
                edad = float(edad_input.replace(",", "."))
            else:
                edad = float(edad_input)

            if edad < 5 or edad > 19:
                return {"error": "La edad debe estar entre 5 y 19 años"}

            edad_meses = int(edad * 12)

        except (ValueError, TypeError):
            return {"error": f"Formato de edad inválido: {edad_input}"}

        # Convertir peso y altura usando las nuevas funciones
        try:
            peso = convertir_peso_a_float(peso_input)
            altura = convertir_altura_a_metros(altura_input)
        except ValueError as e:
            return {"error": str(e)}

        # Calcular IMC
        try:
            imc = calcular_imc(peso, altura)
        except ValueError as e:
            return {"error": str(e)}

        # Cargar percentiles
        df = cargar_percentiles(sexo)
        if df is None:
            return {"error": "No se pudieron cargar las tablas de percentiles"}

        # Buscar la fila más cercana a la edad en meses
        if 'Month' not in df.columns:
            return {"error": "Formato de tabla de percentiles inválido"}

        fila_edad = df.iloc[abs(df['Month'] - edad_meses).idxmin()]

        # Verificar que las columnas necesarias existen
        columnas_necesarias = ['L', 'M', 'S']
        for col in columnas_necesarias:
            if col not in df.columns:
                return {"error": f"Columna '{col}' no encontrada en tabla de percentiles"}

        L = fila_edad['L']
        M = fila_edad['M']
        S = fila_edad['S']

        # Calcular Z-score y percentil
        z_score = (((imc / M)**L) - 1) / (L * S)
        percentil = normal_cdf(z_score) * 100
        interpretacion = interpretar_percentil(percentil)

        return {
            "imc": round(imc, 2),
            "percentil": round(percentil, 1),
            "interpretacion": interpretacion,
            "edad_meses": edad_meses  # Información adicional útil
        }

    except Exception as e:
        return {"error": f"Error inesperado en cálculo: {str(e)}"}


def calcular_imc_menor_por_fecha(sexo: str, fecha_nacimiento: str, peso_input, altura_input) -> dict:  # noqa: C901
    """
    Calcula el percentil de IMC para un menor usando fecha de nacimiento.

    Args:
        sexo: "Masculino" o "Femenino"
        fecha_nacimiento: Fecha de nacimiento en formato DD/MM/YYYY o YYYY-MM-DD
        peso_input: Peso en kg (acepta formatos variados)
        altura_input: Altura en m o cm (acepta formatos variados)

    Returns:
        dict: {
            "imc": float, "percentil": float, "interpretacion": str,
            "edad_meses": int, "edad_años": float
        } or {"error": str}
    """
    try:
        # Validar sexo
        if sexo not in ["Masculino", "Femenino"]:
            return {"error": "Sexo debe ser 'Masculino' o 'Femenino'"}

        # Calcular edad exacta en meses
        try:
            edad_meses = calcular_edad_exacta_en_meses(fecha_nacimiento)
        except ValueError as e:
            return {"error": str(e)}

        # Validar rango de edad (5-19 años = 60-228 meses)
        if edad_meses < 60:
            edad_años = edad_meses / 12.0
            return {"error": f"La edad debe ser mayor a 5 años (actualmente {edad_años:.1f} años)"}
        elif edad_meses > 228:
            edad_años = edad_meses / 12.0
            return {"error": f"La edad debe ser menor a 19 años (actualmente {edad_años:.1f} años)"}

        # Convertir peso y altura usando las funciones existentes
        try:
            peso = convertir_peso_a_float(peso_input)
            altura = convertir_altura_a_metros(altura_input)
        except ValueError as e:
            return {"error": str(e)}

        # Calcular IMC
        try:
            imc = calcular_imc(peso, altura)
        except ValueError as e:
            return {"error": str(e)}

        # Cargar percentiles
        df = cargar_percentiles(sexo)
        if df is None:
            return {"error": "No se pudieron cargar las tablas de percentiles"}

        # Buscar la fila más cercana a la edad en meses
        if 'Month' not in df.columns:
            return {"error": "Formato de tabla de percentiles inválido"}

        fila_edad = df.iloc[abs(df['Month'] - edad_meses).idxmin()]

        # Verificar que las columnas necesarias existen
        columnas_necesarias = ['L', 'M', 'S']
        for col in columnas_necesarias:
            if col not in df.columns:
                return {"error": f"Columna '{col}' no encontrada en tabla de percentiles"}

        L = fila_edad['L']
        M = fila_edad['M']
        S = fila_edad['S']

        # Calcular Z-score y percentil
        z_score = (((imc / M)**L) - 1) / (L * S)
        percentil = normal_cdf(z_score) * 100
        interpretacion = interpretar_percentil(percentil)

        edad_años = edad_meses / 12.0

        return {
            "imc": round(imc, 2),
            "percentil": round(percentil, 1),
            "interpretacion": interpretacion,
            "edad_meses": edad_meses,
            "edad_años": round(edad_años, 1)
        }

    except Exception as e:
        return {"error": f"Error inesperado en cálculo: {str(e)}"}


def obtener_rangos_percentiles():
    """
    Devuelve los rangos de percentiles para la barra visual con sus respectivos datos.
    Basado en los estándares de la OMS para menores.
    Retorna una lista de diccionarios con la información de cada rango.
    Cada rango incluye ahora una clave 'key' estable para localizar el nombre en Android.
    """
    return [
        {
            "key": "bajo_peso",
            "nombre": "Bajo peso",
            "rango_texto": "<3",
            "min_valor": 0.0,
            "max_valor": 3.0,
            "color": "#2196F3"  # Azul
        },
        {
            "key": "peso_saludable",
            "nombre": "Peso saludable",
            "rango_texto": "3-84",
            "min_valor": 3.0,
            "max_valor": 85.0,
            "color": "#4CAF50"  # Verde
        },
        {
            "key": "sobrepeso",
            "nombre": "Sobrepeso",
            "rango_texto": "85-96",
            "min_valor": 85.0,
            "max_valor": 97.0,
            "color": "#FF9800"  # Naranja
        },
        {
            "key": "obesidad",
            "nombre": "Obesidad",
            "rango_texto": "≥97",
            "min_valor": 97.0,
            "max_valor": 100.0,  # Valor máximo para la barra
            "color": "#D32F2F"  # Rojo
        }
    ]


def calcular_posicion_en_barra_percentil(percentil):
    """
    Calcula la posición relativa del percentil en la barra (de 0.0 a 1.0).
    Distribuye los rangos de manera proporcional en la barra.
    """
    if percentil < 3:
        # Rango bajo peso: 0% al 15% de la barra
        return (percentil / 3.0) * 0.15
    elif percentil < 85:
        # Rango peso saludable: 15% al 70% de la barra (más espacio por ser el rango más amplio)
        return 0.15 + ((percentil - 3.0) / (85.0 - 3.0)) * 0.55
    elif percentil < 97:
        # Rango sobrepeso: 70% al 90% de la barra
        return 0.70 + ((percentil - 85.0) / (97.0 - 85.0)) * 0.20
    else:
        # Rango obesidad: 90% al 100% de la barra
        return 0.90 + ((percentil - 97.0) / 3.0) * 0.10


def obtener_categoria_percentil(percentil):
    """
    Devuelve la categoría del percentil como un diccionario con información detallada.
    Esta es la función central que determina la categoría basada en los percentiles de la OMS.
    """
    rangos = obtener_rangos_percentiles()

    # Determinar categoría basada en el valor del percentil
    if percentil < 3:
        categoria = rangos[0]  # Bajo peso
    elif percentil < 85:
        categoria = rangos[1]  # Peso saludable
    elif percentil < 97:
        categoria = rangos[2]  # Sobrepeso
    else:
        categoria = rangos[3]  # Obesidad

    return {
        "categoria": categoria,
        "posicion": calcular_posicion_en_barra_percentil(percentil),
        "percentil_valor": round(percentil, 1)
    }


def interpretar_percentil_detallado(percentil):
    """
    Interpreta el percentil de IMC y devuelve un mensaje más detallado.
    Incluye información específica sobre la posición en el percentil.
    """
    if percentil < 3:
        return (
            f"Bajo peso. Tu percentil es {percentil:.1f}, "
            "lo que significa que tienes un peso menor al 97% de "
            "niños de tu edad y sexo."
        )
    elif percentil < 50:
        return (
            f"Peso saludable (percentil bajo-medio). "
            f"Tu percentil es {percentil:.1f}, estás en un rango saludable."
        )
    elif percentil < 85:
        return (
            f"Peso saludable (percentil medio-alto). "
            f"Tu percentil es {percentil:.1f}, estás en un rango saludable."
        )
    elif percentil < 97:
        return (
            f"Sobrepeso. Tu percentil es {percentil:.1f}, "
            f"tienes un peso mayor al {percentil:.0f}% de niños "
            "de tu edad y sexo."
        )
    else:
        return (
            f"Obesidad. Tu percentil es {percentil:.1f}, "
            "tienes un peso mayor al 97% de niños de tu edad y sexo."
        )
