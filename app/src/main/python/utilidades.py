###
# Módulo de utilidades generales para la aplicación IMC
# Funciones de conversión, validación y utilidades compartidas
###

import sqlite3
from datetime import datetime
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
