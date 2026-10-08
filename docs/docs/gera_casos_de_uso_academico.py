import xml.etree.ElementTree as ET
from itertools import combinations
from math import cos, pi, sin

from gera_casos_de_uso import FONTE_UC, RX, RY, ator, associa, caso, dependencia, pacote, quebra
from gera_modelo_dados import Svg, medir

NS = "{http://www.w3.org/2000/svg}"
LARGURA, ALTURA = 1060, 1110
LIMITE = (170, 62, 890, 1056)
ESQ_X, DIR_X = 305, 755
ATOR_ESQ_X, ATOR_DIR_X = 92, 968
PASSO = 165
Y_INICIAL = 170
TOPO_PACOTE = 115
DESLOC_ATOR = 22
ESPACO_ATOR = 88
AFASTAMENTO = 9
TAM_REL = 12
TAM_CONDICAO = 11.5
MARGEM_CAIXA = 1.6
LARGURA_NOME = 2 * RX - 30
CORREDOR_A, CORREDOR_B, CORREDOR_C = 580, 520, 440
ARCO_DX_20 = RX * 0.6
ARCO_DX_15 = RX * 0.8
ARCO_DY_20 = RY * 0.8
ARCO_DY_15 = RY * 0.6
RECUO_TOPO = 20
RECUO_BASE = 42

TITULO = "Casos de uso — Acadêmico (Sprints 1 a 3)"
SECRETARIA = [
    "Manter alunos e responsáveis",
    "Manter turmas e oficinas",
    "Emitir declaração de matrícula",
    "Matricular aluno em turma",
    "Gerenciar lista de espera e convocação",
]
PLANO = "Gerar plano de pagamento da matrícula"
SALA = [
    "Registrar chamada",
    "Registrar parecer descritivo",
    "Consultar frequência e grade horária",
    "Registrar consentimento do responsável",
]
RESTRINGIR = "Restringir o acesso aos dados da própria organização"
CASOS = SECRETARIA + [PLANO] + SALA + [RESTRINGIR]
CONDICAO_TURMA = "[turma sem vaga]"
CONDICAO_FINANCEIRO = "[módulo Financeiro ativo]"
NOTA = "Casos fora dos pacotes pertencem a outros módulos e aparecem aqui só pelas relações."


def cy(i):
    return Y_INICIAL + i * PASSO


def ator_em(svg, x, altura, nome, sistema=False):
    return ator(svg, x, altura - DESLOC_ATOR, nome, sistema=sistema)


def rotulo_ao_lado(texto, x, base):
    return x + AFASTAMENTO + medir(texto, TAM_REL) / 2, base


def gera():
    svg = Svg(LARGURA, ALTURA)
    svg.texto(LARGURA / 2, 34, TITULO, tam=19, negrito=True, ancora="middle")
    x0, y0, x1, y1 = LIMITE
    svg.retangulo(x0, y0, x1 - x0, y1 - y0, preenchimento="#ffffff", borda="#111827", espessura=1.6, camada="fundo")
    svg.texto(x0 + 12, y0 + 22, "SING", tam=15, negrito=True, camada="fundo")
    pacote(svg, ESQ_X - RX - 10, TOPO_PACOTE, 2 * RX + 20, cy(4) + RY + 14 - TOPO_PACOTE, "Secretaria")
    pacote(svg, DIR_X - RX - 10, TOPO_PACOTE, 2 * RX + 20, cy(3) + RY + 14 - TOPO_PACOTE, "Sala de aula e família")

    esq = [caso(svg, ESQ_X, cy(i), nome) for i, nome in enumerate(SECRETARIA)]
    caso(svg, ESQ_X, cy(5), PLANO)
    dir_ = [caso(svg, DIR_X, cy(i), nome) for i, nome in enumerate(SALA)]
    caso(svg, DIR_X, cy(4), RESTRINGIR)

    secretaria = ator_em(svg, ATOR_ESQ_X, cy(2), "Secretaria")
    responsavel_esq = ator_em(svg, ATOR_ESQ_X, cy(4), "Responsável")
    email = ator_em(svg, ATOR_ESQ_X, cy(4) + ESPACO_ATOR, "Serviço de e-mail", sistema=True)
    professor = ator_em(svg, ATOR_DIR_X, (cy(0) + cy(1)) / 2, "Professor")
    meio_dir = (cy(2) + cy(3)) / 2
    responsavel_dir = ator_em(svg, ATOR_DIR_X, meio_dir, "Responsável")
    aluno = ator_em(svg, ATOR_DIR_X, meio_dir + ESPACO_ATOR, "Aluno")

    for uc in esq:
        associa(svg, secretaria, uc, "e")
    associa(svg, responsavel_esq, esq[4], "e")
    associa(svg, email, esq[4], "e")
    associa(svg, professor, dir_[0], "d")
    associa(svg, professor, dir_[1], "d")
    associa(svg, responsavel_dir, dir_[2], "d")
    associa(svg, responsavel_dir, dir_[3], "d")
    associa(svg, aluno, dir_[2], "d")

    topo_lista, base_matricula = cy(4) - RY, cy(3) + RY
    meio_vao = (topo_lista + base_matricula) / 2
    dependencia(svg, [(ESQ_X, topo_lista), (ESQ_X, base_matricula)], "«extend»",
                rotulo_ao_lado("«extend»", ESQ_X, meio_vao - 6))
    svg.texto(ESQ_X + AFASTAMENTO, meio_vao + 13, CONDICAO_TURMA, tam=TAM_CONDICAO, ancora="start",
              halo=True, camada="topo", cor="#374151")

    entrada_topo = (DIR_X - ARCO_DX_20, cy(4) - ARCO_DY_20)
    dependencia(svg, [(ESQ_X + RX, cy(0)), (CORREDOR_A, cy(0)), (CORREDOR_A, entrada_topo[1] - RECUO_TOPO),
                      (entrada_topo[0], entrada_topo[1] - RECUO_TOPO), entrada_topo],
                "«include»", ((CORREDOR_A + entrada_topo[0]) / 2, entrada_topo[1] - RECUO_TOPO - 8))

    entrada_base = (DIR_X - ARCO_DX_20, cy(4) + ARCO_DY_20)
    dependencia(svg, [(ESQ_X + ARCO_DX_15, cy(3) - ARCO_DY_15), (CORREDOR_B, cy(3) - ARCO_DY_15),
                      (CORREDOR_B, entrada_base[1] + RECUO_BASE), (entrada_base[0], entrada_base[1] + RECUO_BASE),
                      entrada_base],
                "«include»", ((CORREDOR_B + entrada_base[0]) / 2, entrada_base[1] + RECUO_BASE - 10))

    dependencia(svg, [(ESQ_X + RX, cy(5)), (CORREDOR_C, cy(5)), (CORREDOR_C, cy(3)), (ESQ_X + RX, cy(3))],
                "«extend»", rotulo_ao_lado("«extend»", CORREDOR_C, cy(5) - 73))
    svg.texto(CORREDOR_C + AFASTAMENTO, cy(5) - 51, CONDICAO_FINANCEIRO, tam=TAM_CONDICAO, ancora="start",
              halo=True, camada="topo", cor="#374151")

    svg.texto(LARGURA / 2, y1 + 28, NOTA, tam=11.5, ancora="middle", cor="#374151")
    return svg.salvar("casos-de-uso-academico.svg")


def exige(condicao, mensagem):
    if not condicao:
        raise AssertionError(mensagem)


def caixa_texto(texto):
    x, y = float(texto.get("x")), float(texto.get("y"))
    tam = float(texto.get("font-size"))
    largura = medir(texto.text, tam, negrito=texto.get("font-weight") == "bold")
    esquerda = {"start": x, "middle": x - largura / 2, "end": x - largura}[texto.get("text-anchor")]
    return esquerda, y - 0.8 * tam, esquerda + largura, y + 0.25 * tam


def caixas_se_cruzam(a, b):
    return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]


def expande(caixa, margem):
    return caixa[0] - margem, caixa[1] - margem, caixa[2] + margem, caixa[3] + margem


def dentro_elipse(px, py, elipse):
    ecx, ecy, erx, ery = elipse
    return ((px - ecx) / erx) ** 2 + ((py - ecy) / ery) ** 2 < 1


def sobre_elipse(px, py, elipse):
    ecx, ecy, erx, ery = elipse
    return abs(((px - ecx) / erx) ** 2 + ((py - ecy) / ery) ** 2 - 1) < 1e-6


def dentro_caixa(p, caixa):
    return caixa[0] <= p[0] <= caixa[2] and caixa[1] <= p[1] <= caixa[3]


def grade(caixa, n=12):
    e, t, d, b = caixa
    return [(e + (d - e) * i / n, t + (b - t) * j / n) for i in range(n + 1) for j in range(n + 1)]


def amostra(a, b, passo=0.5):
    n = max(1, int(max(abs(b[0] - a[0]), abs(b[1] - a[1])) / passo))
    return [(a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(n + 1)]


def contorno(elipse, n=720):
    ecx, ecy, erx, ery = elipse
    return [(ecx + erx * cos(2 * pi * k / n), ecy + ery * sin(2 * pi * k / n)) for k in range(n)]


def sobrepoe(a, b):
    return any(dentro_elipse(px, py, b) for px, py in contorno(a)) or \
        any(dentro_elipse(px, py, a) for px, py in contorno(b))


def orientacao(p, q, r):
    return (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0])


def segmentos_se_cruzam(s, t):
    return orientacao(*s, t[0]) * orientacao(*s, t[1]) < 0 and orientacao(*t, s[0]) * orientacao(*t, s[1]) < 0


def pontos(pl):
    return [tuple(float(v) for v in p.split(",")) for p in pl.get("points").split()]


def verifica(caminho):
    raiz = ET.parse(caminho).getroot()
    elipses = [tuple(float(e.get(k)) for k in ("cx", "cy", "rx", "ry")) for e in raiz.iter(NS + "ellipse")]
    textos = list(raiz.iter(NS + "text"))
    poligonais = [pontos(pl) for pl in raiz.iter(NS + "polyline")]
    tracejadas = [pontos(pl) for pl in raiz.iter(NS + "polyline") if pl.get("stroke-dasharray")]
    x0, y0, x1, y1 = LIMITE
    exige(len(elipses) == len(CASOS), "numero de casos de uso")

    for ecx, ecy, erx, ery in elipses:
        exige(ecx - erx >= 0 and ecx + erx <= LARGURA and ecy - ery >= 0 and ecy + ery <= ALTURA,
              "caso de uso fora do canvas")
        exige(x0 <= ecx - erx and ecx + erx <= x1 and y0 <= ecy - ery and ecy + ery <= y1,
              "caso de uso fora da fronteira")
    for a, b in combinations(elipses, 2):
        exige(not sobrepoe(a, b), "casos de uso sobrepostos")

    caixas_atores = []
    for bolinha in raiz.iter(NS + "circle"):
        hx, hy = float(bolinha.get("cx")), float(bolinha.get("cy"))
        rotulos = [caixa_texto(t) for t in textos if t.get("stroke") is None and abs(float(t.get("x")) - hx) < 0.05
                   and hy + 60 < float(t.get("y")) < hy + 110]
        caixa = (min([hx - 15] + [r[0] for r in rotulos]), min([hy - 9] + [r[1] for r in rotulos]),
                 max([hx + 15] + [r[2] for r in rotulos]), max([hy + 50] + [r[3] for r in rotulos]))
        exige(caixa[0] >= 0 and caixa[2] <= LARGURA and caixa[1] >= 0 and caixa[3] <= ALTURA,
              "ator fora do canvas")
        exige(caixa[2] <= x0 or caixa[0] >= x1 or caixa[3] <= y0 or caixa[1] >= y1,
              "ator dentro da fronteira")
        caixas_atores.append(caixa)
    exige(len(caixas_atores) == 6, "numero de atores")
    for a, b in combinations(caixas_atores, 2):
        exige(not caixas_se_cruzam(a, b), "atores sobrepostos")

    for nome in CASOS:
        linhas = quebra(nome, LARGURA_NOME)
        exige(len(linhas) <= 3, f"mais de 3 linhas: {nome}")
        exige(all(medir(linha, FONTE_UC) <= LARGURA_NOME for linha in linhas), f"linha larga demais: {nome}")

    rotulos = [(t, caixa_texto(t)) for t in textos if t.get("stroke") == "#ffffff"]
    exige(len(rotulos) == 6, "numero de rotulos de relacao")
    areas = []
    for t, caixa in rotulos:
        px, py = float(t.get("x")), float(t.get("y"))
        area = expande(caixa, MARGEM_CAIXA)
        exige(not any(dentro_elipse(px, py, e) for e in elipses), f"ponto de rotulo dentro de elipse: {t.text}")
        exige(not any(dentro_elipse(qx, qy, e) for e in elipses for qx, qy in grade(area)),
              f"rotulo toca elipse: {t.text}")
        exige(not any(dentro_caixa(p, area) for pl in poligonais for a, b in zip(pl, pl[1:]) for p in amostra(a, b)),
              f"rotulo toca linha: {t.text}")
        exige(not any(caixas_se_cruzam(area, c) for c in caixas_atores), f"rotulo toca ator: {t.text}")
        areas.append(area)
    for a, b in combinations(areas, 2):
        exige(not caixas_se_cruzam(a, b), "rotulos sobrepostos")

    for pl in poligonais:
        permitidas = {i for i, e in enumerate(elipses) if any(sobre_elipse(px, py, e) for px, py in pl)}
        for i, e in enumerate(elipses):
            if i not in permitidas:
                exige(not any(dentro_elipse(px, py, e) for a, b in zip(pl, pl[1:]) for px, py in amostra(a, b)),
                      "linha atravessa caso de uso")

    segmentos = [list(zip(pl, pl[1:])) for pl in tracejadas]
    for i, j in combinations(range(len(segmentos)), 2):
        exige(not any(segmentos_se_cruzam(s, t) for s in segmentos[i] for t in segmentos[j]),
              "relacoes se cruzam")
    exige(abs(CORREDOR_A - CORREDOR_B) >= 14, "include sem 14 px de distancia")


if __name__ == "__main__":
    caminho = gera()
    verifica(caminho)
    print(caminho, LARGURA, ALTURA)
    print("verificacao ok")
