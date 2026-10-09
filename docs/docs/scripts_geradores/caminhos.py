import os

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.normpath(os.path.join(AQUI, ".."))
DIAGRAMAS = os.path.join(RAIZ, "diagramas")
PROTOTIPOS = os.path.join(RAIZ, "prototipos")
TIPOS_DE_DIAGRAMA = (
    ("casos-de-uso", "casos-de-uso"), ("sequencia", "sequencia"), ("modelo-dados", "modelo-de-dados"),
    ("classes", "classes"), ("atividades", "atividades"), ("bpmn", "bpmn"),
)


def diagrama(nome):
    pasta = next(pasta for prefixo, pasta in TIPOS_DE_DIAGRAMA if nome.startswith(prefixo))
    os.makedirs(os.path.join(DIAGRAMAS, pasta), exist_ok=True)
    return os.path.join(DIAGRAMAS, pasta, nome)
