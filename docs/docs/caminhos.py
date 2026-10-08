import os

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = AQUI if os.path.isdir(os.path.join(AQUI, "diagramas")) else os.path.normpath(os.path.join(AQUI, ".."))
DIAGRAMAS = os.path.join(RAIZ, "diagramas")
PROTOTIPOS = os.path.join(RAIZ, "prototipos")
