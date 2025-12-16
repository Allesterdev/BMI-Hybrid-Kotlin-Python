###
# Módulo específico para cálculos de IMC en adultos
# Incluye interpretación, categorías y rangos para adultos
###

def interpretar_imc(imc):
    """Interpreta el IMC y devuelve una clave de recurso (string key) basada en el valor de IMC."""
    # Clasificación basada en los umbrales numéricos (evita depender de textos que pueden variar)
    if imc < 18.5:
        return "interpretacion_bajo_peso_adulto"
    elif imc <= 24.9:
        return "interpretacion_normal_adulto"
    elif imc <= 29.9:
        return "interpretacion_sobrepeso_adulto"
    elif imc <= 34.9:
        return "interpretacion_obesidad_1_adulto"
    elif imc <= 39.9:
        return "interpretacion_obesidad_2_adulto"
    else:
        return "interpretacion_obesidad_3_adulto"


def obtener_rangos_imc():
    """
    Devuelve los rangos de IMC para la barra visual con sus respectivos datos.
    Retorna una lista de diccionarios con la información de cada rango.
    Cada rango ahora incluye un campo 'key' que es la clave estable para localización en Android.
    """
    return [
        {
            "key": "bajo_peso",
            "nombre": "Bajo peso",
            "rango_texto": "<18.5",
            "min_valor": 0.0,
            "max_valor": 18.5,
            "color": "#2196F3"  # Azul
        },
        {
            "key": "peso_normal",
            "nombre": "Normal",
            "rango_texto": "18.5-24.9",
            "min_valor": 18.5,
            "max_valor": 24.9,
            "color": "#4CAF50"  # Verde
        },
        {
            "key": "sobrepeso",
            "nombre": "Sobrepeso",
            "rango_texto": "25-29.9",
            "min_valor": 25.0,
            "max_valor": 29.9,
            "color": "#FF9800"  # Naranja
        },
        {
            "key": "obesidad_1",
            "nombre": "Obes. I",
            "rango_texto": "30-34.9",
            "min_valor": 30.0,
            "max_valor": 34.9,
            "color": "#FF5722"  # Rojo naranja
        },
        {
            "key": "obesidad_2",
            "nombre": "Obes. II",
            "rango_texto": "35-39.9",
            "min_valor": 35.0,
            "max_valor": 39.9,
            "color": "#D32F2F"  # Rojo
        },
        {
            "key": "obesidad_3",
            "nombre": "Obes. III",
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
