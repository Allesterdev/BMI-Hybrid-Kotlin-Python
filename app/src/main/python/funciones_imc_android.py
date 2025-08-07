###
# En este modulo ponemos la lógica principal de la aplicación que luego sera
# llamada desde otros módulos.
# Adaptado para su uso en Android con Chaquopy.
###

import sqlite3
from datetime import datetime
import io
import pandas as pd
import math
from java import jclass
import re


def convertir_altura_a_metros(altura_input):
    """
    Convierte diferentes formatos de altura a metros.
    Acepta: string, int, float
    Formatos: "170", "1.70", "1,70", 170, 1.70, etc.
    """
    try:
        # Si ya es un número, trabajamos con él
        if isinstance(altura_input, (int, float)):
            valor = float(altura_input)
        else:
            # Si es string, limpiamos y convertimos
            altura_str = str(altura_input).replace(",", ".").strip()
            valor = float(altura_str)

        # Si es mayor a 10, asumimos centímetros y convertimos a metros
        if valor > 10:
            return valor / 100.0  # 170 -> 1.70
        else:
            return valor  # 1.70 -> 1.70

    except (ValueError, TypeError):
        raise ValueError(f"Formato de altura inválido: {altura_input}")


def convertir_peso_a_float(peso_input):
    """
    Convierte diferentes formatos de peso a float.
    Acepta: string, int, float con punto o coma decimal.
    """
    try:
        if isinstance(peso_input, (int, float)):
            return float(peso_input)
        else:
            peso_str = str(peso_input).replace(",", ".").strip()
            return float(peso_str)
    except (ValueError, TypeError):
        raise ValueError(f"Formato de peso inválido: {peso_input}")


def calcular_imc(peso_input, altura_input):
    """
    Calcula el Índice de Masa Corporal (IMC) usando peso y altura.
    Acepta múltiples formatos de entrada y los convierte automáticamente.
    """
    try:
        peso = convertir_peso_a_float(peso_input)
        altura = convertir_altura_a_metros(altura_input)

        if peso <= 0:
            raise ValueError("El peso debe ser mayor a 0")
        if altura <= 0:
            raise ValueError("La altura debe ser mayor a 0")

        # Validaciones adicionales de rangos razonables
        if peso > 1000:  # 1000 kg es irreal
            raise ValueError("Peso fuera de rango válido")
        if altura > 3.0:  # 3 metros es irreal
            raise ValueError("Altura fuera de rango válido")

        return round(peso / (altura ** 2), 2)

    except ValueError as e:
        raise ValueError(f"Error en cálculo de IMC: {str(e)}")


def interpretar_imc(imc):
    """Interpreta el IMC y devuelve un mensaje descriptivo."""
    categoria_info = obtener_categoria_imc(imc)

    # Mapeo de nombres de categoría a mensajes descriptivos
    mensajes = {
        "Bajo peso": "Tienes bajo peso",
        "Normal": "Tu IMC es normal",
        "Sobrepeso": "Tienes sobrepeso",
        "Obesidad I": "Tienes obesidad de grado 1",
        "Obesidad II": "Tienes obesidad de grado 2",
        "Obesidad III": "Tienes obesidad de grado 3"
    }

    return mensajes.get(categoria_info["categoria"]["nombre"], "Categoría no reconocida")


def obtener_rangos_imc():
    """
    Devuelve los rangos de IMC para la barra visual con sus respectivos datos.
    Retorna una lista de diccionarios con la información de cada rango.
    """
    return [
        {
            "nombre": "Bajo peso",
            "rango_texto": "<18.5",
            "min_valor": 0.0,
            "max_valor": 18.5,
            "color": "#2196F3"  # Azul
        },
        {
            "nombre": "Normal",
            "rango_texto": "18.5-24.9",
            "min_valor": 18.5,
            "max_valor": 24.9,
            "color": "#4CAF50"  # Verde
        },
        {
            "nombre": "Sobrepeso",
            "rango_texto": "25-29.9",
            "min_valor": 25.0,
            "max_valor": 29.9,
            "color": "#FF9800"  # Naranja
        },
        {
            "nombre": "Obesidad I",
            "rango_texto": "30-34.9",
            "min_valor": 30.0,
            "max_valor": 34.9,
            "color": "#FF5722"  # Rojo naranja
        },
        {
            "nombre": "Obesidad II",
            "rango_texto": "35-39.9",
            "min_valor": 35.0,
            "max_valor": 39.9,
            "color": "#D32F2F"  # Rojo
        },
        {
            "nombre": "Obesidad III",
            "rango_texto": "≥40",
            "min_valor": 40.0,
            "max_valor": 50.0,  # Valor máximo para la barra
            "color": "#7B1FA2"  # Morado
        }
    ]


def calcular_posicion_en_barra(imc):
    """
    Calcula la posición relativa del IMC en la barra (de 0.0 a 1.0).
    """
    if imc < 18.5:
        # Rango bajo peso: 0% al 16.67% de la barra
        return (imc / 18.5) * (1.0 / 6.0)
    elif imc < 25.0:
        # Rango normal: 16.67% al 33.33% de la barra
        return (1.0 / 6.0) + ((imc - 18.5) / (24.9 - 18.5)) * (1.0 / 6.0)
    elif imc < 30.0:
        # Rango sobrepeso: 33.33% al 50% de la barra
        return (2.0 / 6.0) + ((imc - 25.0) / (29.9 - 25.0)) * (1.0 / 6.0)
    elif imc < 35.0:
        # Rango obesidad I: 50% al 66.67% de la barra
        return (3.0 / 6.0) + ((imc - 30.0) / (34.9 - 30.0)) * (1.0 / 6.0)
    elif imc < 40.0:
        # Rango obesidad II: 66.67% al 83.33% de la barra
        return (4.0 / 6.0) + ((imc - 35.0) / (39.9 - 35.0)) * (1.0 / 6.0)
    else:
        # Rango obesidad III: 83.33% al 100% de la barra
        return (5.0 / 6.0) + ((imc - 40.0) / 10.0) * (1.0 / 6.0)


def obtener_categoria_imc(imc):
    """
    Devuelve la categoría del IMC como un diccionario con información detallada.
    Esta es la función central que determina la categoría basada en los rangos.
    """
    rangos = obtener_rangos_imc()

    # Determinar categoría basada en el valor del IMC
    if imc < 18.5:
        categoria = rangos[0]  # Bajo peso
    elif imc <= 24.9:
        categoria = rangos[1]  # Normal
    elif imc <= 29.9:
        categoria = rangos[2]  # Sobrepeso
    elif imc <= 34.9:
        categoria = rangos[3]  # Obesidad I
    elif imc <= 39.9:
        categoria = rangos[4]  # Obesidad II
    else:
        categoria = rangos[5]  # Obesidad III

    return {
        "categoria": categoria,
        "posicion": calcular_posicion_en_barra(imc),
        "imc_valor": round(imc, 1)
    }


def obtener_fecha():
    """Función para usar la fecha en la que se hacen las mediciones."""
    return datetime.now().strftime("%d-%m-%Y %H:%M:%S")


def inicializar_base_de_datos():
    """
    Configura la tabla 'perfiles' en una conexión existente si no existe.
    También se asegura de que las columnas para el cálculo de menores
    estén presentes.
    """
    with sqlite3.connect('historial_imc.db') as conexion:
        cur = conexion.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS perfiles(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                peso REAL,
                altura REAL,
                imc REAL,
                fecha TEXT
            )
        """)

        # Verificar y añadir columnas para el cálculo de menores si no existen
        cur.execute("PRAGMA table_info(perfiles)")
        columnas = [info[1] for info in cur.fetchall()]

        if 'sexo' not in columnas:
            cur.execute("ALTER TABLE perfiles ADD COLUMN sexo TEXT")

        if 'edad_meses' not in columnas:
            cur.execute("ALTER TABLE perfiles ADD COLUMN edad_meses INTEGER")

        if 'percentil' not in columnas:
            cur.execute("ALTER TABLE perfiles ADD COLUMN percentil REAL")

        conexion.commit()


def guardar_medicion(peso, altura, imc, sexo=None, edad_meses=None, percentil=None):
    """
    Aquí guardamos la medición del usuario.
    Incluye campos opcionales para sexo, edad y percentil para menores.
    """
    inicializar_base_de_datos()
    fecha = obtener_fecha()
    with sqlite3.connect('historial_imc.db') as conexion:
        cur = conexion.cursor()
        cur.execute("""
                INSERT INTO perfiles (peso, altura, imc, fecha, sexo, edad_meses, percentil)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, (peso, altura, imc, fecha, sexo, edad_meses, percentil))

        conexion.commit()


def mostrar_historial(tipo_historial: str) -> list[dict]:
    """
    Busca los perfiles guardados en la base de datos y devuelve una
    lista de diccionarios con el historial.

    Args:
        tipo_historial (str): 'adultos' o 'menores'.
    """
    inicializar_base_de_datos()
    with sqlite3.connect('historial_imc.db') as conexion:
        cur = conexion.cursor()
        if tipo_historial == 'adultos':
            cur.execute(
                'SELECT peso, altura, imc, fecha FROM perfiles WHERE sexo IS NULL ORDER BY fecha DESC')
        else:
            cur.execute(
                'SELECT peso, altura, imc, fecha, sexo, edad_meses, percentil FROM perfiles WHERE sexo IS NOT NULL ORDER BY fecha DESC')
        datos = cur.fetchall()

    historial_list = []
    for d in datos:
        record = {
            "peso": d[0],
            "altura": d[1],
            "imc": d[2],
            "fecha": d[3]
        }
        if tipo_historial == 'menores':
            record["sexo"] = d[4]
            record["edad_meses"] = d[5]
            record["percentil"] = d[6]
        historial_list.append(record)
    return historial_list


def borrar_historial_adultos():
    """
    Borra todos los registros de IMC de adultos (donde sexo es NULL) de la base de datos.
    """
    inicializar_base_de_datos()
    with sqlite3.connect('historial_imc.db') as conexion:
        cur = conexion.cursor()
        cur.execute("DELETE FROM perfiles WHERE sexo IS NULL")
        conexion.commit()


def borrar_historial_menores():
    """
    Borra todos los registros de IMC de menores (donde sexo NO es NULL) de la base de datos.
    """
    inicializar_base_de_datos()
    with sqlite3.connect('historial_imc.db') as conexion:
        cur = conexion.cursor()
        cur.execute("DELETE FROM perfiles WHERE sexo IS NOT NULL")
        conexion.commit()


def obtener_datos_para_grafico(tipo_historial):
    """
    Obtiene fechas e IMCs de la base de datos para un tipo de historial.

    Args:
        tipo_historial (str): 'adultos' o 'menores'.

    Returns:
        tuple: Dos listas, una de fechas (datetime) y otra de IMCs (float).
    """
    inicializar_base_de_datos()
    with sqlite3.connect('historial_imc.db') as conexion:
        cur = conexion.cursor()
        if tipo_historial == 'adultos':
            query = "SELECT fecha, imc FROM perfiles WHERE sexo IS NULL ORDER BY fecha ASC"
        else:  # 'menores'
            query = "SELECT fecha, percentil FROM perfiles WHERE sexo IS NOT NULL ORDER BY fecha ASC"
        cur.execute(query)
        datos = cur.fetchall()

    if not datos:
        return [], []

    datos_desempaquetados = []
    for fecha_str, valor in datos:
        try:
            # Aseguramos que la fecha se pueda parsear
            fecha_dt = datetime.strptime(fecha_str, "%d-%m-%Y %H:%M:%S")
            # Ignoramos valores nulos que podrían estar en la BD
            if valor is not None:
                datos_desempaquetados.append((fecha_dt, valor))
        except (ValueError, TypeError):
            # Ignoramos registros con fecha inválida para el gráfico
            continue

    if not datos_desempaquetados:
        return [], []

    # Ordenamos por fecha por si la base de datos no lo hizo
    datos_ordenados = sorted(datos_desempaquetados, key=lambda x: x[0])

    fechas = [d[0] for d in datos_ordenados]
    valores = [d[1] for d in datos_ordenados]

    return fechas, valores


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


def calcular_imc_menor(sexo: str, edad_input, peso_input, altura_input) -> dict:
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


def normal_cdf(x, mu=0, sigma=1):
    """Función de distribución acumulada normal estándar."""
    return 0.5 * (1 + math.erf((x - mu) / (sigma * math.sqrt(2))))


def interpretar_percentil(percentil):
    """Interpreta el percentil de IMC y devuelve un mensaje descriptivo."""
    if percentil < 3:
        return "Bajo peso."
    elif percentil < 85:
        return "Peso saludable."
    elif percentil < 97:
        return "Sobrepeso."
    else:
        return "Obesidad."


def calcular_edad_exacta_en_meses(fecha_nacimiento_str):
    """
    Calcula la edad exacta en meses desde la fecha de nacimiento hasta hoy.

    Args:
        fecha_nacimiento_str: Fecha en formato "YYYY-MM-DD" o "DD/MM/YYYY"

    Returns:
        int: Edad en meses completos
    """

    try:
        # Intentar diferentes formatos de fecha
        fecha_str = fecha_nacimiento_str.strip()

        # Formato ISO: YYYY-MM-DD
        if re.match(r'^\d{4}-\d{2}-\d{2}$', fecha_str):
            fecha_nacimiento = datetime.strptime(fecha_str, "%Y-%m-%d")
        # Formato DD/MM/YYYY
        elif re.match(r'^\d{1,2}/\d{1,2}/\d{4}$', fecha_str):
            fecha_nacimiento = datetime.strptime(fecha_str, "%d/%m/%Y")
        # Formato DD-MM-YYYY
        elif re.match(r'^\d{1,2}-\d{1,2}-\d{4}$', fecha_str):
            fecha_nacimiento = datetime.strptime(fecha_str, "%d-%m-%Y")
        else:
            raise ValueError(f"Formato de fecha no válido. Use DD/MM/YYYY o YYYY-MM-DD")

        # Calcular edad exacta
        fecha_actual = datetime.now()

        # Verificar que la fecha de nacimiento no sea futura
        if fecha_nacimiento > fecha_actual:
            raise ValueError("La fecha de nacimiento no puede ser en el futuro")

        # Calcular diferencia en meses
        meses_diferencia = (fecha_actual.year - fecha_nacimiento.year) * 12 + (fecha_actual.month - fecha_nacimiento.month)

        # Si aún no ha cumplido el mes, restar 1
        if fecha_actual.day < fecha_nacimiento.day:
            meses_diferencia -= 1

        return meses_diferencia

    except ValueError as e:
        raise ValueError(f"Error procesando fecha de nacimiento: {str(e)}")


def calcular_imc_menor_por_fecha(sexo: str, fecha_nacimiento: str, peso_input, altura_input) -> dict:
    """
    Calcula el percentil de IMC para un menor usando fecha de nacimiento.

    Args:
        sexo: "Masculino" o "Femenino"
        fecha_nacimiento: Fecha de nacimiento en formato DD/MM/YYYY o YYYY-MM-DD
        peso_input: Peso en kg (acepta formatos variados)
        altura_input: Altura en m o cm (acepta formatos variados)

    Returns:
        dict: {"imc": float, "percentil": float, "interpretacion": str, "edad_meses": int, "edad_años": float} o {"error": str}
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

        return {
            "imc": round(imc, 2),
            "percentil": round(percentil, 1),
            "interpretacion": interpretacion,
            "edad_meses": edad_meses,
            "edad_años": round(edad_meses / 12.0, 1)  # Información adicional útil
        }

    except Exception as e:
        return {"error": f"Error inesperado en cálculo: {str(e)}"}
