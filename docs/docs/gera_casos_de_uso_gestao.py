import math
import xml.etree.ElementTree as ET
from itertools import combinations

from gera_casos_de_uso import FONTE_UC, RX, RY, associa, ator, caso, dependencia, pacote, quebra
from gera_casos_de_uso_escolar import (
    amostra, caixa_texto, caixas_se_cruzam, dentro_caixa, dentro_elipse, expande, exige, grade,
)
from gera_modelo_dados import Svg, medir

NS = "{http://www.w3.org/2000/svg}"
LARGURA, ALTURA = 1060, 600
LIMITE = (165, 62, 895, 545)
ESQ_X, DIR_X = 305, 755
ATOR_ESQ_X, ATOR_DIR_X = 92, 968
DESLOC_ATOR = 22
TOPO_PACOTE = 115
L1, L2, L3 = 180, 320, 500
R1, R2, R3 = 180, 380, 500
XI, XJ = 455, 600
AFASTAMENTO = 9
TAM_REL = 12
TAM_CONDICAO = 11.5
MARGEM_CAIXA = 1.6
LARGURA_NOME = 2 * RX - 30
COR_TEXTO_SECUNDARIO = "#374151"
TITULO = "Casos de uso — Gestão e financeiro (Sprints 2 e 3)"
NOTA = "Casos fora dos pacotes pertencem a outros módulos e aparecem aqui só pelas relações."
PAINEL = "Consultar painel gerencial"
RELATORIOS = "Gerar relatórios gerenciais"
RESTRINGIR = "Restringir o acesso aos dados da própria organização"
PLANO = "Gerar plano de pagamento da matrícula"
COBRANCA = "Registrar pagamento de cobrança"
MATRICULA = "Matricular aluno em turma"
CASOS = [PAINEL, RELATORIOS, RESTRINGIR, PLANO, COBRANCA, MATRICULA]


def ator_em(svg, x, altura, nome, sistema=False):
    return ator(svg, x, altura - DESLOC_ATOR, nome, sistema=sistema)


def rotulo_ao_lado(texto, x_inicio, y):
    return x_inicio + medir(texto, TAM_REL) / 2, y


def condicao(svg, x_inicio, y, texto):
    svg.texto(x_inicio, y, texto, tam=TAM_CONDICAO, ancora="start", halo=True, camada="topo",
              cor=COR_TEXTO_SECUNDARIO)


def contorno(elipse, n=720):
    cx, cy, rx, ry = elipse
    return [(cx + rx * math.cos(2 * math.pi * i / n), cy + ry * math.sin(2 * math.pi * i / n)) for i in range(n)]


def elipses_se_cruzam(a, b):
    return any(dentro_elipse(px, py, b) for px, py in contorno(a)) or any(
        dentro_elipse(px, py, a) for px, py in contorno(b))


def gera():
    svg = Svg(LARGURA, ALTURA)
    svg.texto(LARGURA / 2, 34, TITULO, tam=19, negrito=True, ancora="middle")
    x0, y0, x1, y1 = LIMITE
    svg.retangulo(x0, y0, x1 - x0, y1 - y0, preenchimento="#ffffff", borda="#111827", espessura=1.6, camada="fundo")
    svg.texto(x0 + 12, y0 + 22, "SING", tam=15, negrito=True, camada="fundo")
    svg.texto(LARGURA / 2, y1 + 22, NOTA, tam=11.5, ancora="middle", cor=COR_TEXTO_SECUNDARIO)
    pacote(svg, ESQ_X - RX - 12, TOPO_PACOTE, 2 * RX + 24, L2 + RY + 14 - TOPO_PACOTE, "Gestão")
    pacote(svg, DIR_X - RX - 12, TOPO_PACOTE, 2 * RX + 24, R2 + RY + 14 - TOPO_PACOTE, "Financeiro")

    painel = caso(svg, ESQ_X, L1, PAINEL)
    relatorios = caso(svg, ESQ_X, L2, RELATORIOS)
    caso(svg, ESQ_X, L3, RESTRINGIR)
    plano = caso(svg, DIR_X, R1, PLANO)
    cobranca = caso(svg, DIR_X, R2, COBRANCA)
    caso(svg, DIR_X, R3, MATRICULA)

    administrador = ator_em(svg, ATOR_ESQ_X, (L1 + L2) / 2, "Administrador da escola")
    financeiro = ator_em(svg, ATOR_DIR_X, (R1 + R2) / 2, "Financeiro")
    provedor = ator_em(svg, ATOR_DIR_X, R2, "Provedor de pagamentos", sistema=True)

    associa(svg, administrador, painel, "e")
    associa(svg, administrador, relatorios, "e")
    associa(svg, financeiro, plano, "d")
    associa(svg, financeiro, cobranca, "d")
    associa(svg, provedor, cobranca, "d")

    dependencia(svg, [(ESQ_X, L2 - RY), (ESQ_X, L1 + RY)], "«extend»",
                rotulo_ao_lado("«extend»", ESQ_X + AFASTAMENTO, 240))
    condicao(svg, ESQ_X + AFASTAMENTO, 264, "[detalhar indicador]")
    dependencia(svg, [(ESQ_X + RX, L1), (XI, L1), (XI, L3), (ESQ_X + RX, L3)], "«include»",
                rotulo_ao_lado("«include»", XI + AFASTAMENTO, 240))
    dependencia(svg, [(DIR_X - RX, R1), (XJ, R1), (XJ, R3), (DIR_X - RX, R3)], "«extend»",
                rotulo_ao_lado("«extend»", XJ + AFASTAMENTO, 270))
    condicao(svg, XJ + AFASTAMENTO, 294, "[módulo Financeiro ativo]")
    return svg.salvar("casos-de-uso-gestao.svg")


def verifica(caminho):
    raiz = ET.parse(caminho).getroot()
    elipses = [tuple(float(e.get(k)) for k in ("cx", "cy", "rx", "ry")) for e in raiz.iter(NS + "ellipse")]
    textos = list(raiz.iter(NS + "text"))
    poligonais = [[tuple(float(v) for v in p.split(",")) for p in pl.get("points").split()]
                  for pl in raiz.iter(NS + "polyline")]
    x0, y0, x1, y1 = LIMITE
    exige(len(elipses) == len(CASOS), "numero de casos de uso")

    for ecx, ecy, erx, ery in elipses:
        exige(ecx - erx >= 0 and ecx + erx <= LARGURA and ecy - ery >= 0 and ecy + ery <= ALTURA,
              "caso de uso fora do canvas")
        exige(x0 <= ecx - erx and ecx + erx <= x1 and y0 <= ecy - ery and ecy + ery <= y1,
              "caso de uso fora da fronteira")
    for a, b in combinations(elipses, 2):
        exige(not elipses_se_cruzam(a, b), "casos de uso sobrepostos")

    caixas_atores = []
    for bolinha in raiz.iter(NS + "circle"):
        hx, hy = float(bolinha.get("cx")), float(bolinha.get("cy"))
        rotulos_ator = [caixa_texto(t) for t in textos
                        if t.get("stroke") is None and abs(float(t.get("x")) - hx) < 0.05
                        and hy + 60 < float(t.get("y")) < hy + 110]
        caixa = (min([hx - 15] + [r[0] for r in rotulos_ator]), min([hy - 9] + [r[1] for r in rotulos_ator]),
                 max([hx + 15] + [r[2] for r in rotulos_ator]), max([hy + 50] + [r[3] for r in rotulos_ator]))
        exige(caixa[0] >= 0 and caixa[2] <= LARGURA and caixa[1] >= 0 and caixa[3] <= ALTURA,
              "ator fora do canvas")
        exige(caixa[2] <= x0 or caixa[0] >= x1 or caixa[3] <= y0 or caixa[1] >= y1,
              "ator dentro da fronteira")
        caixas_atores.append(caixa)
    exige(len(caixas_atores) == 3, "numero de atores")
    for a, b in combinations(caixas_atores, 2):
        exige(not caixas_se_cruzam(a, b), "atores sobrepostos")

    for nome in CASOS:
        linhas = quebra(nome, LARGURA_NOME)
        exige(len(linhas) <= 3, f"mais de 3 linhas: {nome}")
        exige(all(medir(linha, FONTE_UC) <= LARGURA_NOME for linha in linhas), f"linha larga demais: {nome}")

    rotulos = [(t, caixa_texto(t)) for t in textos if t.get("stroke") == "#ffffff"]
    exige(len(rotulos) == 5, "numero de rotulos de relacao")
    outros = [caixa_texto(t) for t in textos if t.get("stroke") != "#ffffff"]
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
        exige(not any(caixas_se_cruzam(area, o) for o in outros), f"rotulo sobre texto: {t.text}")
        areas.append(area)
    for a, b in combinations(areas, 2):
        exige(not caixas_se_cruzam(a, b), "rotulos sobrepostos")

    for pl in poligonais:
        for e in elipses:
            exige(not any(dentro_elipse(px, py, e) for a, b in zip(pl, pl[1:]) for px, py in amostra(a, b)),
                  "linha atravessa caso de uso")


if __name__ == "__main__":
    caminho = gera()
    verifica(caminho)
    print(caminho, LARGURA, ALTURA)
    print("verificacao ok")
