import xml.etree.ElementTree as ET
from itertools import combinations

from gera_casos_de_uso import FONTE_UC, RX, RY, ator, associa, caso, dependencia, pacote, quebra
from gera_modelo_dados import Svg, medir

NS = "{http://www.w3.org/2000/svg}"
TITULO = "Casos de uso — Plataforma e personalização (Sprints 1 a 4)"
LARGURA, ALTURA = 1060, 1120
LIMITE = (170, 62, 890, 1070)
ESQ_X, DIR_X = 305, 755
ATOR_ESQ_X, ATOR_DIR_X = 92, 968
Y_INICIAL = 170
PASSO_ESQ = 280
PASSO_DIR = 140
TOPO_PACOTE = 115
DESLOC_ATOR = 22
AFASTAMENTO = 9
TAM_REL = 12
TAM_NOTA = 11.5
MARGEM_CAIXA = 1.6
LARGURA_NOME = 2 * RX - 30
CORREDOR_PROVISIONAR = 480
CORREDOR_EXPORTAR = 520
RELACAO = "«include»"
NOTA = "Usuário generaliza todos os atores humanos (ver diagrama geral)."
ACESSO = [
    "Realizar login de usuário",
    "Restringir o acesso aos dados da própria organização",
    "Provisionar organização",
    "Gerenciar organizações do parceiro",
]
PERSONALIZACAO = [
    "Configurar a identidade visual da organização",
    "Configurar o vocabulário da organização",
    "Ativar e desativar módulos da organização",
    "Configurar regras acadêmicas da organização",
    "Gerenciar papéis e permissões",
    "Exportar os dados da organização",
    "Gerenciar usuários por convite",
]


def cy_esq(i):
    return Y_INICIAL + i * PASSO_ESQ


def cy_dir(i):
    return Y_INICIAL + i * PASSO_DIR


def ator_em(svg, x, altura, nome, sistema=False):
    return ator(svg, x, altura - DESLOC_ATOR, nome, sistema=sistema)


def rotulo_lateral(x, base, texto):
    return x + AFASTAMENTO + medir(texto, TAM_REL) / 2, base


def gera():
    svg = Svg(LARGURA, ALTURA)
    svg.texto(LARGURA / 2, 34, TITULO, tam=19, negrito=True, ancora="middle")
    x0, y0, x1, y1 = LIMITE
    svg.retangulo(x0, y0, x1 - x0, y1 - y0, preenchimento="#ffffff", borda="#111827", espessura=1.6, camada="fundo")
    svg.texto(x0 + 12, y0 + 22, "SING", tam=15, negrito=True, camada="fundo")
    base = cy_dir(len(PERSONALIZACAO) - 1) + RY + 14
    pacote(svg, ESQ_X - RX - 10, TOPO_PACOTE, 2 * RX + 20, base - TOPO_PACOTE, "Acesso e organizações")
    pacote(svg, DIR_X - RX - 10, TOPO_PACOTE, 2 * RX + 20, base - TOPO_PACOTE, "Personalização da escola")

    acesso = [caso(svg, ESQ_X, cy_esq(i), nome) for i, nome in enumerate(ACESSO)]
    personalizacao = [caso(svg, DIR_X, cy_dir(i), nome) for i, nome in enumerate(PERSONALIZACAO)]
    login, restringir, provisionar, parceiro = acesso
    exportar, convite = personalizacao[5], personalizacao[6]

    usuario = ator_em(svg, ATOR_ESQ_X, login["cy"], "Usuário")
    operador = ator_em(svg, ATOR_ESQ_X, provisionar["cy"], "Operador da plataforma")
    parceiro_ator = ator_em(svg, ATOR_ESQ_X, parceiro["cy"], "Parceiro")
    administrador = ator_em(svg, ATOR_DIR_X, personalizacao[3]["cy"], "Administrador da escola")
    email = ator_em(svg, ATOR_DIR_X, convite["cy"], "Serviço de e-mail", sistema=True)

    associa(svg, usuario, login, "e")
    associa(svg, operador, provisionar, "e")
    associa(svg, parceiro_ator, parceiro, "e")
    for caso_dir in personalizacao:
        associa(svg, administrador, caso_dir, "d")
    associa(svg, email, convite, "d")

    dependencia(svg, [(ESQ_X + RX, provisionar["cy"]), (CORREDOR_PROVISIONAR, provisionar["cy"]),
                      (CORREDOR_PROVISIONAR, convite["cy"]), (DIR_X - RX, convite["cy"])],
                RELACAO, ((CORREDOR_PROVISIONAR + DIR_X - RX) / 2, convite["cy"] - 7))
    dependencia(svg, [(DIR_X - RX, exportar["cy"]), (CORREDOR_EXPORTAR, exportar["cy"]),
                      (CORREDOR_EXPORTAR, restringir["cy"]), (ESQ_X + RX, restringir["cy"])],
                RELACAO, rotulo_lateral(CORREDOR_EXPORTAR, (restringir["cy"] + exportar["cy"]) / 2, RELACAO))
    svg.texto(x0, y1 + 24, NOTA, tam=TAM_NOTA, ancora="start", cor="#374151")
    return svg.salvar("casos-de-uso-plataforma.svg")


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
    return ((px - ecx) / erx) ** 2 + ((py - ecy) / ery) ** 2 < 1 - 1e-9


def sobre_elipse(px, py, elipse):
    ecx, ecy, erx, ery = elipse
    return abs(((px - ecx) / erx) ** 2 + ((py - ecy) / ery) ** 2 - 1) < 1e-6


def dentro_caixa(p, caixa):
    return caixa[0] <= p[0] <= caixa[2] and caixa[1] <= p[1] <= caixa[3]


def grade(caixa, n=6):
    e, t, d, b = caixa
    return [(e + (d - e) * i / n, t + (b - t) * j / n) for i in range(n + 1) for j in range(n + 1)]


def amostra(a, b, passo=0.5):
    n = max(1, int(max(abs(b[0] - a[0]), abs(b[1] - a[1])) / passo))
    return [(a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(n + 1)]


def sobrepoe(a, b):
    ecx, ecy, erx, ery = a
    return any(dentro_elipse(px, py, a) and dentro_elipse(px, py, b)
               for px, py in grade((ecx - erx, ecy - ery, ecx + erx, ecy + ery), n=80))


def orientacao(p, q, r):
    return (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0])


def cruzam(a, b, c, d):
    return orientacao(a, b, c) * orientacao(a, b, d) < 0 and orientacao(c, d, a) * orientacao(c, d, b) < 0


def verifica(caminho):
    raiz = ET.parse(caminho).getroot()
    elipses = [tuple(float(e.get(k)) for k in ("cx", "cy", "rx", "ry")) for e in raiz.iter(NS + "ellipse")]
    textos = list(raiz.iter(NS + "text"))
    poligonais = [[tuple(float(v) for v in p.split(",")) for p in pl.get("points").split()]
                  for pl in raiz.iter(NS + "polyline")]
    x0, y0, x1, y1 = LIMITE
    exige(len(elipses) == len(ACESSO) + len(PERSONALIZACAO), "numero de casos de uso")

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
        rotulos = [caixa_texto(t) for t in textos
                   if t.get("stroke") is None and abs(float(t.get("x")) - hx) < 0.05
                   and hy + 60 < float(t.get("y")) < hy + 110]
        caixa = (min([hx - 15] + [r[0] for r in rotulos]), min([hy - 9] + [r[1] for r in rotulos]),
                 max([hx + 15] + [r[2] for r in rotulos]), max([hy + 50] + [r[3] for r in rotulos]))
        exige(caixa[0] >= 0 and caixa[2] <= LARGURA and caixa[1] >= 0 and caixa[3] <= ALTURA,
              "ator fora do canvas")
        exige(caixa[2] <= x0 or caixa[0] >= x1 or caixa[3] <= y0 or caixa[1] >= y1,
              "ator dentro da fronteira")
        caixas_atores.append(caixa)
    exige(len(caixas_atores) == 5, "numero de atores")
    for a, b in combinations(caixas_atores, 2):
        exige(not caixas_se_cruzam(a, b), "atores sobrepostos")

    for nome in ACESSO + PERSONALIZACAO:
        linhas = quebra(nome, LARGURA_NOME)
        exige(len(linhas) <= 3, f"mais de 3 linhas: {nome}")
        exige(all(medir(linha, FONTE_UC) <= LARGURA_NOME for linha in linhas), f"linha larga demais: {nome}")

    for t in textos:
        esq, topo, dir_, base = caixa_texto(t)
        exige(esq >= 0 and dir_ <= LARGURA and topo >= 0 and base <= ALTURA, f"texto fora do canvas: {t.text}")

    rotulos = [(t, caixa_texto(t)) for t in textos if t.get("stroke") == "#ffffff"]
    exige(len(rotulos) == 2, "numero de rotulos de relacao")
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
        for a, b in zip(pl, pl[1:]):
            for px, py in amostra(a, b):
                exige(not any(dentro_elipse(px, py, e) for e in elipses), "linha atravessa caso de uso")

    for pl_a, pl_b in combinations(poligonais, 2):
        for a, b in zip(pl_a, pl_a[1:]):
            for c, d in zip(pl_b, pl_b[1:]):
                exige(not cruzam(a, b, c, d), "linhas se cruzam")
    exige(abs(CORREDOR_PROVISIONAR - CORREDOR_EXPORTAR) >= 14, "setas include com corredor muito proximo")


if __name__ == "__main__":
    caminho = gera()
    verifica(caminho)
    print(caminho, LARGURA, ALTURA)
    print("verificacao ok")
