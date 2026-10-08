import copy
import io
import os
import re
import sys
import zipfile
from xml.sax.saxutils import escape

from lxml import etree
from PIL import Image

import spec
from caminhos import RAIZ
from casos_teste import CT
from gera_sequencia import SEQ

AQUI = os.path.dirname(os.path.abspath(__file__))
BASE = RAIZ
TEMPLATE = os.path.join(AQUI, "ESSW-template.docx")
SAIDA = os.path.join(BASE, f"{spec.PRODUTO} - Especificacao do Projeto.docx")

W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
WP = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
A = "http://schemas.openxmlformats.org/drawingml/2006/main"
PIC = "http://schemas.openxmlformats.org/drawingml/2006/picture"
MC = "http://schemas.openxmlformats.org/markup-compatibility/2006"
NS = {"w": W, "mc": MC}
FONTE = '<w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>'
CINZA = "F2F2F2"
DESTAQUE = "E8F1FB"
RETRATO = 10490
PAISAGEM = 15390
EMU_CM = 360000


def q(nome):
    return "{%s}%s" % (W, nome)


def xml(fragmento):
    raiz = etree.fromstring(
        f'<raiz xmlns:w="{W}" xmlns:r="{R}" xmlns:wp="{WP}" xmlns:a="{A}" xmlns:pic="{PIC}">{fragmento}</raiz>'
    )
    return list(raiz)


def rpr(b=False, i=False, sz=22, cor=None):
    partes = [FONTE]
    if b:
        partes.append("<w:b/><w:bCs/>")
    if i:
        partes.append("<w:i/><w:iCs/>")
    if cor:
        partes.append(f'<w:color w:val="{cor}"/>')
    partes.append(f'<w:sz w:val="{sz}"/><w:szCs w:val="{sz}"/>')
    return "<w:rPr>" + "".join(partes) + "</w:rPr>"


def r(texto, **fmt):
    return f'<w:r>{rpr(**fmt)}<w:t xml:space="preserve">{escape(texto)}</w:t></w:r>'


def p(conteudo="", jc=None, antes=40, depois=40, sz=22, b=False, i=False, recuo=None, manter=False,
      quebra_antes=False, lista=False):
    if isinstance(conteudo, str):
        runs = r(conteudo, sz=sz, b=b, i=i) if conteudo else ""
    else:
        runs = "".join(r(t, **{"sz": sz, **f}) for t, f in conteudo)
    ppr = []
    if lista:
        ppr.append('<w:pStyle w:val="PargrafodaLista"/>')
    if manter:
        ppr.append("<w:keepNext/>")
    if quebra_antes:
        ppr.append("<w:pageBreakBefore/>")
    if lista:
        ppr.append('<w:numPr><w:ilvl w:val="0"/><w:numId w:val="21"/></w:numPr>')
    ppr.append(f'<w:spacing w:before="{antes}" w:after="{depois}"/>')
    if lista:
        ppr.append('<w:ind w:left="340" w:hanging="255"/>')
    elif recuo:
        ppr.append(f'<w:ind w:left="{recuo}" w:hanging="{recuo}"/>')
    if jc:
        ppr.append(f'<w:jc w:val="{jc}"/>')
    ppr.append(rpr(sz=sz))
    return f'<w:p><w:pPr>{"".join(ppr)}</w:pPr>{runs}</w:p>'


def tc(conteudo, largura, span=1, fundo=None, valign="center"):
    if isinstance(conteudo, (list, tuple)):
        conteudo = "".join(conteudo)
    if not conteudo.startswith("<w:p"):
        conteudo = p(conteudo)
    pr = f'<w:tcW w:w="{largura}" w:type="dxa"/>'
    if span > 1:
        pr += f'<w:gridSpan w:val="{span}"/>'
    if fundo:
        pr += f'<w:shd w:val="clear" w:color="auto" w:fill="{fundo}"/>'
    pr += f'<w:vAlign w:val="{valign}"/>'
    return f"<w:tc><w:tcPr>{pr}</w:tcPr>{conteudo}</w:tc>"


def tr(celulas, altura=None, cabecalho=False, inteira=True):
    pr = ""
    if inteira:
        pr += "<w:cantSplit/>"
    if altura:
        pr += f'<w:trHeight w:val="{altura}"/>'
    if cabecalho:
        pr += "<w:tblHeader/>"
    return f'<w:tr><w:trPr>{pr}</w:trPr>{"".join(celulas)}</w:tr>'


def tbl(larguras, linhas):
    grade = "".join(f'<w:gridCol w:w="{w}"/>' for w in larguras)
    return (
        '<w:tbl><w:tblPr><w:tblStyle w:val="Tabelacomgrade"/>'
        f'<w:tblW w:w="{sum(larguras)}" w:type="dxa"/><w:tblInd w:w="-5" w:type="dxa"/>'
        '<w:tblLayout w:type="fixed"/>'
        '<w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/>'
        f'</w:tblPr><w:tblGrid>{grade}</w:tblGrid>{"".join(linhas)}</w:tbl>'
    )


def vazio(sz=12, quebra_antes=False):
    return p("", antes=0, depois=0, sz=sz, quebra_antes=quebra_antes)


def secao(paisagem):
    tamanho = (
        '<w:pgSz w:w="16838" w:h="11906" w:orient="landscape"/>' if paisagem else '<w:pgSz w:w="11906" w:h="16838"/>'
    )
    return (
        '<w:p><w:pPr><w:spacing w:before="0" w:after="0"/><w:sectPr>'
        f'<w:footerReference w:type="default" r:id="rId13"/>{tamanho}'
        '<w:pgMar w:top="720" w:right="720" w:bottom="720" w:left="720" w:header="708" w:footer="708" w:gutter="0"/>'
        '<w:cols w:space="708"/><w:docGrid w:linePitch="360"/></w:sectPr></w:pPr></w:p>'
    )


class Midia:
    def __init__(self):
        self.itens = []
        self.pendentes = []

    def imagem(self, relativo, max_l, max_a, descricao=""):
        caminho = os.path.join(BASE, relativo)
        if not os.path.exists(caminho):
            self.pendentes.append(relativo)
            return p(f"[imagem pendente: {relativo}]", jc="center", sz=20, i=True)
        with Image.open(caminho) as original:
            largura, altura = original.size
            reduzida = original.convert("RGB").quantize(colors=256, method=Image.Quantize.MEDIANCUT)
            memoria = io.BytesIO()
            reduzida.save(memoria, format="PNG", optimize=True)
        cx = int(min(max_l * EMU_CM, max_a * EMU_CM * largura / altura))
        cy = int(cx * altura / largura)
        numero = len(self.itens) + 1
        rid = f"rIdImg{numero}"
        nome = f"img{numero}.png"
        self.itens.append((rid, nome, memoria.getvalue()))
        return (
            '<w:p><w:pPr><w:spacing w:before="60" w:after="60"/><w:jc w:val="center"/></w:pPr><w:r><w:drawing>'
            f'<wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/>'
            f'<wp:docPr id="{1000 + numero}" name="Imagem {numero}" descr="{escape(descricao, {chr(34): "&quot;"})}"/>'
            '<wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr>'
            '<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic>'
            f'<pic:nvPicPr><pic:cNvPr id="{1000 + numero}" name="{nome}"/><pic:cNvPicPr/></pic:nvPicPr>'
            f'<pic:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
            f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>'
            '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic>'
            "</wp:inline></w:drawing></w:r></w:p>"
        )


MIDIA = Midia()


def moldura(titulo, relativo, total, max_l, max_a):
    return tbl([total], [
        tr([tc(p(titulo, b=True), total)], altura=375),
        tr([tc(MIDIA.imagem(relativo, max_l, max_a, titulo), total)]),
    ])


def maiuscula(texto):
    return texto[0].upper() + texto[1:]


def nome_rf(numero):
    return next(item[1] for item in spec.RF if item[0] == numero)


def sprint_rf(numero):
    return next(item[7] for item in spec.RF if item[0] == numero)


def s1_objetivos():
    linhas = [
        tr([tc(p("QUADRO “3 OBJETIVOS”", jc="center", b=True, sz=24), RETRATO, span=2)], altura=567),
        tr([tc(p([("NOME DO PRODUTO", {"b": True}), (f": {spec.PRODUTO} – {spec.SUBTITULO}", {})], sz=24), RETRATO, span=2)], altura=567),
        tr([tc(p("OBJETIVOS", jc="center", b=True, sz=24), 2019), tc(p("DESCRIÇÃO", jc="center", b=True, sz=24), 8471)], altura=567),
    ]
    for i, (titulo, problema, valor, metrica) in enumerate(spec.OBJETIVOS, 1):
        linhas.append(tr([tc(p(str(i), jc="center", sz=24), 2019), tc([
            p(titulo, b=True, antes=80, depois=40),
            p([("Problema: ", {"b": True}), (problema, {})], sz=20, antes=20, depois=20),
            p([("Valor: ", {"b": True}), (valor, {})], sz=20, antes=20, depois=20),
            p([("Métrica de sucesso: ", {"b": True}), (metrica, {})], sz=20, antes=20, depois=80),
        ], 8471)], altura=567))
    return [tbl([2019, 8471], linhas)]


def s2_quadro():
    def quadrante(titulo, subtitulo, itens, largura):
        partes = [p(titulo, b=True, sz=24, depois=0), p(subtitulo, i=True, sz=20, depois=80)]
        partes += [p(item, lista=True, antes=30, depois=30) for item in itens]
        return tc(partes, largura, valign="top")

    meio = RETRATO // 2
    linhas = [
        tr([tc(p("QUADRO “É – NÃO É – FAZ – NÃO FAZ”", jc="center", b=True, sz=24), RETRATO, span=2)], altura=567),
        tr([tc(p([("NOME DO PRODUTO", {"b": True}), (f": {spec.PRODUTO} – {spec.SUBTITULO}", {})], sz=24), RETRATO, span=2)], altura=567),
        tr([quadrante("É", "Atributos necessários ou desejados", spec.E_NAO_E["e"], meio),
            quadrante("Não é", "Atributos indesejados ou impeditivos", spec.E_NAO_E["nao_e"], meio)]),
        tr([quadrante("Faz", "Ações ou capacidades esperadas ou necessárias", spec.E_NAO_E["faz"], meio),
            quadrante("Não faz", "Ações ou capacidades indesejadas ou não permitidas", spec.E_NAO_E["nao_faz"], meio)]),
    ]
    return [tbl([meio, meio], linhas)]


def s3_visao():
    produto = p([("NOME DO PRODUTO", {"b": True}), (f": {spec.PRODUTO} – {spec.SUBTITULO}", {})], sz=24)
    primeira = tbl([2268, 8222], [
        tr([tc(produto, RETRATO, span=2)], altura=567),
        tr([tc(p("PROBLEMAS", b=True, sz=24), 2268), tc([p(t, lista=True, sz=20) for t in spec.PROBLEMAS], 8222)]),
        tr([tc(p("EXPECTATIVAS", b=True, sz=24), 2268), tc([p(t, lista=True, sz=20) for t in spec.EXPECTATIVAS], 8222)]),
    ])
    linhas = [
        tr([tc(p("VISÃO DE PRODUTO", jc="center", b=True, sz=24), RETRATO, span=2)], altura=567),
        tr([tc(produto, RETRATO, span=2)], altura=567),
    ]
    for campo, texto in spec.VISAO.items():
        linhas.append(tr([tc(p(campo, b=True, sz=20), 3402), tc(p(texto, antes=60, depois=60, sz=20), 7088)]))
    return [primeira, vazio(24), tbl([3402, 7088], linhas)]


def s4_bpmn():
    return [
        p("Processo de ingresso de aluno na versão TO BE: do pedido de vaga à matrícula, com verificação das "
          "regras da escola, lista de espera, convocação com prazo e geração do plano de pagamento.", depois=100),
        moldura("Processo TO BE – Ingresso de aluno: do pedido de vaga à matrícula",
                "diagramas/bpmn-ingresso.png", PAISAGEM, 26.3, 12.3),
    ]


def s5_atores():
    linhas = [tr([tc(p("#", jc="center", b=True), 709), tc(p("ATOR / USUÁRIO", jc="center", b=True), 2900),
                  tc(p("PAPEL, RESPONSABILIDADES E RELAÇÃO COM O SISTEMA", jc="center", b=True), 6881)],
                 altura=454, cabecalho=True)]
    for i, (nome, descricao) in enumerate(spec.ATORES, 1):
        linhas.append(tr([tc(p(str(i), jc="center"), 709), tc(p(nome, b=True), 2900), tc(p(descricao), 6881)]))
    return [
        p("O ator “Usuário” generaliza todos os atores humanos da lista: qualquer um deles realiza login. "
          "Provedor de pagamentos e Serviço de e-mail são sistemas externos.", depois=100),
        tbl([709, 2900, 6881], linhas),
    ]


def s6_rf():
    larguras = [780, 3400, 1560, 760, 3110, 880]
    titulos = ["#", "REQUISITO FUNCIONAL", "ATOR / USUÁRIO", "OBJ.", "PRIORIDADE E JUSTIFICATIVA", "SPRINT"]
    linhas = [
        tr([tc(p([("PRODUTO", {"b": True}), (f": {spec.PRODUTO} – {spec.SUBTITULO}", {})]), RETRATO, span=6)], altura=454, cabecalho=True),
        tr([tc(p(t, jc="center", b=True, sz=18), w) for t, w in zip(titulos, larguras)], altura=454, cabecalho=True),
    ]
    for numero, nome, descricao, ator, objetivo, prioridade, justificativa, sprint in spec.RF:
        fundo = DESTAQUE if sprint == 1 else None
        linhas.append(tr([
            tc(p(f"RF{numero}", jc="center", b=True, sz=18), larguras[0], fundo=fundo),
            tc([p(nome, b=True, sz=18, depois=0), p(descricao, sz=18, antes=0)], larguras[1], fundo=fundo),
            tc(p(ator, sz=18), larguras[2], fundo=fundo),
            tc(p(objetivo, jc="center", sz=18), larguras[3], fundo=fundo),
            tc(p([(spec.PRIORIDADES[prioridade] + ". ", {"b": True}), (justificativa, {})], sz=18), larguras[4], fundo=fundo),
            tc(p(str(sprint), jc="center", b=True, sz=18), larguras[5], fundo=fundo),
        ]))
    return [
        p("Cada requisito indica o ator que o utiliza, o objetivo do Quadro “3 Objetivos” que ele atende (coluna OBJ.), "
          "a prioridade com sua justificativa e a sprint planejada. As linhas sombreadas são os requisitos "
          "priorizados para a Sprint 1.", depois=100),
        tbl(larguras, linhas),
    ]


def s7_us():
    partes = [p("As 16 estórias abaixo cobrem os requisitos das Sprints 1 e 2. Cada estória referencia o requisito "
                "funcional que detalha e tem de 2 a 3 critérios de aceite.", depois=100)]
    atual = None
    for us in spec.US:
        sprint = sprint_rf(us["rf"])
        if sprint != atual:
            atual = sprint
            partes.append(p(f"Sprint {sprint}", b=True, sz=24, antes=160, depois=80, manter=True))
        linhas = [
            tr([tc(p([(f'{us["id"]} – REQUISITO RF{us["rf"]}', {"b": True}), (": " + nome_rf(us["rf"]), {})], antes=60, depois=60, manter=True),
                   RETRATO, span=2, fundo=CINZA)], altura=380),
            tr([tc([
                p([("COMO", {"b": True}), (": " + us["como"], {})], antes=60, depois=60, manter=True),
                p([("POSSO", {"b": True}), (": " + us["posso"], {})], antes=60, depois=60, manter=True),
                p([("PARA", {"b": True}), (": " + us["para"], {})], antes=60, depois=60, manter=True),
            ], RETRATO, span=2)]),
            tr([tc(p("Critérios de Aceite:", b=True, antes=60, depois=60, manter=True), RETRATO, span=2)]),
        ]
        for i, (dado, quando, entao) in enumerate(us["ca"], 1):
            linhas.append(tr([
                tc(p(str(i), jc="center"), 567),
                tc([
                    p([("DADO QUE", {"b": True}), (": " + dado, {})], antes=60, depois=60),
                    p([("QUANDO", {"b": True}), (": " + quando, {})], antes=60, depois=60),
                    p([("ENTÃO", {"b": True}), (": " + entao, {})], antes=60, depois=60),
                ], 9923),
            ]))
        partes += [tbl([567, 9923], linhas), vazio(20)]
    return partes


def s8_rnf():
    larguras = [1050, 6240, 3200]
    linhas = [
        tr([tc(p([("PRODUTO", {"b": True}), (f": {spec.PRODUTO} – {spec.SUBTITULO}", {})]), RETRATO, span=3)], altura=454, cabecalho=True),
        tr([tc(p(t, jc="center", b=True), w) for t, w in zip(["#", "REQUISITO NÃO-FUNCIONAL", "NORMA ISO/IEC 25010"], larguras)],
           altura=454, cabecalho=True),
    ]
    for i, (texto, norma) in enumerate(spec.RNF, 1):
        linhas.append(tr([tc(p(f"RNF{i}", jc="center", b=True, sz=20), larguras[0]), tc(p(texto, sz=20), larguras[1]),
                          tc(p(norma, sz=20), larguras[2])]))
    return [tbl(larguras, linhas)]


def s9_casos_de_uso():
    larguras = [1150, 900, 1150, 4890, 2400]
    linhas = [tr([tc(p(t, jc="center", b=True, sz=18), w) for t, w in zip(
        ["CASO DE USO", "RF", "ESTÓRIA", "NOME", "ESPECIFICAÇÃO"], larguras)], altura=400, cabecalho=True)]
    for uc in spec.UC:
        linhas.append(tr([tc(p(uc["id"], jc="center", sz=18), larguras[0]), tc(p(f'RF{uc["rf"]}', jc="center", sz=18), larguras[1]),
                          tc(p(uc["us"], jc="center", sz=18), larguras[2]), tc(p(uc["nome"], sz=18), larguras[3]),
                          tc(p(f'Seção 10 (Sprint {sprint_rf(uc["rf"])})', sz=18), larguras[4])]))
    for codigo, rf, nome in spec.UC_BACKLOG:
        linhas.append(tr([tc(p(codigo, jc="center", sz=18), larguras[0]), tc(p(f"RF{rf}", jc="center", sz=18), larguras[1]),
                          tc(p("–", jc="center", sz=18), larguras[2]), tc(p(nome, sz=18), larguras[3]),
                          tc(p(f"Prevista para a Sprint {sprint_rf(rf)}", sz=18), larguras[4])]))
    return [
        p("O diagrama geral apresenta os 24 casos de uso das Sprints 1 a 4, um por requisito funcional e com o mesmo nome, a "
          "generalização dos atores humanos no ator “Usuário” e as relações de inclusão e extensão. A inclusão de "
          "“Restringir o acesso aos dados da própria organização” está desenhada a partir de dois casos "
          "representativos, mas vale para todo caso de uso que lê ou grava dados da organização. Em seguida, um diagrama "
          "por módulo repete os casos do diagrama geral em escala legível: Plataforma e personalização, Acadêmico, e "
          "Gestão e financeiro. O último diagrama traz os 12 casos de uso do módulo escolar (Sprints 5 a 7).", depois=100),
        moldura("Diagrama Geral de Casos de Uso", "diagramas/casos-de-uso.png", 10485, 17.9, 19.8),
        vazio(2, quebra_antes=True),
        moldura("Casos de Uso – Plataforma e personalização", "diagramas/casos-de-uso-plataforma.png", 10485, 17.9, 23.0),
        vazio(2, quebra_antes=True),
        moldura("Casos de Uso – Acadêmico", "diagramas/casos-de-uso-academico.png", 10485, 17.9, 23.0),
        vazio(2, quebra_antes=True),
        moldura("Casos de Uso – Gestão e financeiro", "diagramas/casos-de-uso-gestao.png", 10485, 17.9, 23.0),
        vazio(2, quebra_antes=True),
        moldura("Casos de Uso – Módulo escolar (Sprints 5 a 7)", "diagramas/casos-de-uso-escolar.png", 10485, 17.9, 23.0),
        p("Rastreabilidade entre casos de uso, requisitos funcionais e estórias de usuário", b=True, antes=0, depois=100,
          quebra_antes=True, manter=True),
        tbl(larguras, linhas),
    ]


def s10_especificacoes():
    rotulo, valor = 2300, 8190
    partes = [p("Especificações, no formato reduzido, dos 16 casos de uso das Sprints 1 e 2. Nos fluxos, as "
                "referências (A‑n) e (E‑n) indicam o passo em que um fluxo alternativo ou de exceção pode ocorrer.", depois=100)]

    def campo(nome, conteudo):
        return tr([tc(p(nome, b=True, sz=20), rotulo, fundo=CINZA, valign="top"), tc(conteudo, valor, valign="top")], inteira=False)

    def itens(prefixo, textos, digitos=2):
        return [p([(f"{prefixo}{str(i).zfill(digitos)}  ", {"b": True}), (t, {})], sz=20, recuo=640) for i, t in enumerate(textos, 1)]

    for indice, uc in enumerate(spec.UC):
        if indice:
            partes.append(vazio(20))
        fluxos_alt, fluxos_exc = [], []
        for n, (titulo, passos) in enumerate(uc["alt"], 1):
            fluxos_alt.append(p(f"A-{n} – {titulo}", b=True, sz=20, antes=80, manter=True))
            fluxos_alt += [p([(f"A-{n}.{k}  ", {"b": True}), (t, {})], sz=20, recuo=640) for k, t in enumerate(passos, 1)]
        for n, (titulo, passos) in enumerate(uc["exc"], 1):
            fluxos_exc.append(p(f"E-{n} – {titulo}", b=True, sz=20, antes=80, manter=True))
            fluxos_exc += [p([(f"E-{n}.{k}  ", {"b": True}), (t, {})], sz=20, recuo=640) for k, t in enumerate(passos, 1)]
        telas = []
        for codigo, titulo in uc["telas"]:
            telas.append(p([("Protótipo(s) de tela: ", {"b": True}), (f"{codigo} – {titulo}", {})], sz=20))
            telas.append(MIDIA.imagem(f"prototipos/{codigo}.png", 17.6, 11.4, f"{codigo} – {titulo}"))
        linhas = [
            campo("Nome do caso de uso", p(f'{uc["id"]} – {uc["nome"]}', b=True)),
            campo("Ator(es)", p(uc["atores"], sz=20)),
            campo("Descrição", p(uc["descricao"], sz=20)),
            campo("Rastreabilidade", p(f'RF{uc["rf"]} · {uc["us"]} · Sprint {sprint_rf(uc["rf"])}', sz=20)),
            campo("Pré-condições", itens("PC", uc["pre"])),
            campo("Pós-condições", itens("PO", uc["pos"])),
            campo("Regras de negócio", [p([(f"R-{i}  ", {"b": True}), (t, {})], sz=20, recuo=640) for i, t in enumerate(uc["regras"], 1)]),
            tr([tc(telas, rotulo + valor, span=2)]),
            campo("Fluxo básico", [p([(f"{i}  ", {"b": True}), (t, {})], sz=20, recuo=400) for i, t in enumerate(uc["basico"], 1)]),
            campo("Fluxos alternativos", fluxos_alt),
            campo("Fluxos de exceção", fluxos_exc),
        ]
        partes.append(tbl([rotulo, valor], linhas))
    return partes


def s11_atividades():
    return [
        p("Diagrama de atividades do provisionamento de uma organização até o primeiro acesso do administrador, "
          "com as responsabilidades de cada participante, as decisões e o trecho executado em paralelo.", depois=100),
        moldura("Diagrama de Atividades – Provisionar organização e primeiro acesso",
                "diagramas/atividades-provisionar.png", 10485, 17.9, 19.4),
    ]


def s12_modelo():
    return [
        p("Modelo lógico relacional em notação UML: cada tabela é uma classe com o estereótipo «table», com colunas "
          "tipadas e marcações de chave primária (PK), estrangeira (FK) e única (UK). Toda tabela de negócio carrega "
          "tenant_id, que a liga à organização dona do registro.", depois=100),
        moldura("Modelo de Dados – Plataforma, organização e acesso", "diagramas/modelo-dados-plataforma.png", 10485, 17.9, 19.2),
        vazio(2, quebra_antes=True),
        moldura("Modelo de Dados – Pessoas, oferta, matrícula e frequência", "diagramas/modelo-dados-academico.png", 10485, 17.9, 23.0),
        vazio(2, quebra_antes=True),
        moldura("Modelo de Dados – Financeiro e consentimento", "diagramas/modelo-dados-financeiro.png", 10485, 17.9, 23.0),
        vazio(2, quebra_antes=True),
        moldura("Modelo de Dados – Estrutura letiva e currículo", "diagramas/modelo-dados-estrutura-letiva.png", 10485, 17.9, 23.0),
        vazio(2, quebra_antes=True),
        moldura("Modelo de Dados – Avaliação, resultados e ocorrências", "diagramas/modelo-dados-avaliacao.png", 10485, 17.9, 23.0),
    ]


def s13_classes():
    return [
        p("Diagramas de classes em nível técnico do serviço core (Java e Spring Boot), um por contexto: tenant e "
          "identidade, organização e matrícula. A cor do cabeçalho indica a camada (api, application, domain ou infra). "
          "Tipos citados sem classe desenhada (UserStatus, ModuleCode, OrganizationStatus, WaitingStatus, Invitation e os "
          "DTOs de requisição) são enumerações ou registros auxiliares sem comportamento.", depois=100),
        moldura("Diagrama de Classes – Contexto de tenant e identidade", "diagramas/classes-tenant.png", 10485, 17.9, 19.4),
        vazio(2, quebra_antes=True),
        moldura("Diagrama de Classes – Organização e configuração", "diagramas/classes-organizacao.png", 10485, 17.9, 23.0),
        vazio(2, quebra_antes=True),
        moldura("Diagrama de Classes – Matrícula (núcleo acadêmico)", "diagramas/classes-matricula.png", 10485, 17.9, 23.0),
    ]


def s14_sequencia():
    partes = [p("Um diagrama de sequência para cada caso de uso especificado na seção 10, com a interação entre o "
                "ator, a interface (SPA), o controlador, o serviço e o repositório.", depois=100)]
    for indice, uc in enumerate(spec.UC):
        if indice:
            partes.append(vazio(2, quebra_antes=True))
        partes.append(moldura(f'Diagrama de Sequência do {uc["id"]} – {uc["nome"]}',
                              f'diagramas/sequencia-{uc["id"]}.png', 10485, 17.9, 19.4 if indice == 0 else 23.0))
    return partes


def s15_testes():
    larguras = [700, 1150, 800, 1900, 900, 2500, 2300, 2700, 2440]
    titulos = ["CT", "ESTÓRIA E CRITÉRIO", "CASO DE USO", "CENÁRIO", "TIPO", "PRÉ-CONDIÇÃO", "DADOS DE ENTRADA", "PASSOS", "RESULTADO ESPERADO"]
    linhas = [tr([tc(p(t, jc="center", b=True, sz=16), w, fundo=CINZA) for t, w in zip(titulos, larguras)], altura=400, cabecalho=True)]
    uc_por_us = {uc["us"]: uc["id"] for uc in spec.UC}
    us_por_id = {us["id"]: us for us in spec.US}
    for numero, (us_id, criterio, cenario, tipo, dados, passos) in enumerate(CT, 1):
        dado, _, entao = us_por_id[us_id]["ca"][criterio - 1]
        etapas = [t.strip() for t in passos.split("|")]
        if not etapas[-1].lower().startswith(("conferir", "observar")):
            etapas.append("Conferir o resultado esperado")
        lista_passos = [p(f"{i}. {t}", sz=16, antes=10, depois=10) for i, t in enumerate(etapas, 1)]
        linhas.append(tr([
            tc(p(f"CT{numero:03d}", jc="center", b=True, sz=16), larguras[0]),
            tc(p(f"{us_id} · critério {criterio}", jc="center", sz=16), larguras[1]),
            tc(p(uc_por_us[us_id], jc="center", sz=16), larguras[2]),
            tc(p(cenario, sz=16), larguras[3]),
            tc(p(tipo, jc="center", sz=16), larguras[4]),
            tc(p(maiuscula(dado) + ".", sz=16), larguras[5]),
            tc(p(dados, sz=16), larguras[6]),
            tc(lista_passos, larguras[7]),
            tc(p(maiuscula(entao), sz=16), larguras[8]),
        ]))
    return [
        p(f"Os {len(CT)} casos de teste abaixo derivam, um a um, dos critérios de aceite das estórias de usuário "
          "(seção 7) e cobrem o fluxo básico e as principais exceções dos casos de uso (seção 10). A mesma "
          "planilha acompanha este documento no arquivo casos-de-teste.xlsx.", depois=100),
        tbl(larguras, linhas),
    ]


SECOES = [
    (29, s1_objetivos, False), (38, s2_quadro, False), (45, s3_visao, False), (55, s4_bpmn, True),
    (59, s5_atores, False), (67, s6_rf, False), (73, s7_us, False), (85, s8_rnf, False),
    (90, s9_casos_de_uso, False), (97, s10_especificacoes, False), (112, s11_atividades, False),
    (118, s12_modelo, False), (126, s13_classes, False), (134, s14_sequencia, False), (142, s15_testes, True),
]


def troca_texto(paragrafo, texto):
    textos = paragrafo.findall(".//w:t", NS)
    textos[0].text = texto
    for resto in textos[1:]:
        resto.text = ""


def ajusta_capa(filhos):
    for i, nome in enumerate(spec.AUTORES):
        troca_texto(filhos[1 + i], nome)
    troca_texto(filhos[10], f"- {spec.PRODUTO}: {spec.SUBTITULO.upper()} -")
    troca_texto(filhos[21], spec.ANO)
    alternativas = filhos[13].findall(".//mc:AlternateContent", NS)
    alternativas[1].getparent().remove(alternativas[1])
    for elemento in filhos[:29]:
        for cor in elemento.findall(".//w:color", NS):
            if cor.get(q("val")) in ("00B0F0", "ED7D31"):
                cor.getparent().remove(cor)


def titulo_da_secao(original, primeira):
    titulo = copy.deepcopy(original)
    for marca in titulo.findall(".//w:lastRenderedPageBreak", NS):
        marca.getparent().remove(marca)
    if not primeira:
        ppr = titulo.find("w:pPr", NS)
        estilo = ppr.find("w:pStyle", NS)
        estilo.addnext(etree.Element(q("pageBreakBefore")))
    return titulo


def monta():
    with zipfile.ZipFile(TEMPLATE) as origem:
        arquivos = {nome: origem.read(nome) for nome in origem.namelist()}
    documento = etree.fromstring(arquivos["word/document.xml"])
    corpo = documento.find("w:body", NS)
    filhos = list(corpo)
    ajusta_capa(filhos)
    secao_final = filhos[148]
    novos = []
    orientacao = False
    for indice, (posicao, construtor, paisagem) in enumerate(SECOES):
        troca = indice > 0 and paisagem != orientacao
        if troca:
            novos += xml(secao(orientacao))
        orientacao = paisagem
        novos.append(titulo_da_secao(filhos[posicao], indice == 0 or troca))
        novos.append(xml(vazio(12))[0])
        for bloco in construtor():
            novos += xml(bloco)
        novos += xml(vazio(12))
    for elemento in filhos[29:]:
        corpo.remove(elemento)
    for elemento in novos:
        corpo.append(elemento)
    tamanho = secao_final.find("w:pgSz", NS)
    if orientacao:
        tamanho.set(q("w"), "16838")
        tamanho.set(q("h"), "11906")
        tamanho.set(q("orient"), "landscape")
    corpo.append(secao_final)
    arquivos["word/document.xml"] = etree.tostring(documento, xml_declaration=True, encoding="UTF-8", standalone=True)

    relacoes = arquivos["word/_rels/document.xml.rels"].decode("utf-8")
    extras = "".join(
        f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" '
        f'Target="media/{nome}"/>' for rid, nome, _ in MIDIA.itens
    )
    arquivos["word/_rels/document.xml.rels"] = relacoes.replace("</Relationships>", extras + "</Relationships>").encode("utf-8")
    for _, nome, dados in MIDIA.itens:
        arquivos[f"word/media/{nome}"] = dados

    cabecalho = arquivos["word/header1.xml"].decode("utf-8")
    cabecalho = re.sub(r"(>202</w:t>.*?<w:t[^>]*>)5(</w:t>)", r"\g<1>" + spec.ANO[-1] + r"\2", cabecalho, flags=re.S)
    arquivos["word/header1.xml"] = cabecalho.encode("utf-8")
    rodape = arquivos["word/footer2.xml"].decode("utf-8")
    rodape = rodape.replace("Nome do Produto de Software", f"{spec.PRODUTO} – {spec.SUBTITULO}")
    rodape = re.sub(r'<w:color w:val="00B0F0"[^>]*/>', "", rodape)
    arquivos["word/footer2.xml"] = rodape.encode("utf-8")

    with zipfile.ZipFile(SAIDA, "w", zipfile.ZIP_DEFLATED) as destino:
        if "[Content_Types].xml" in arquivos:
            destino.writestr("[Content_Types].xml", arquivos.pop("[Content_Types].xml"))
        for nome, dados in arquivos.items():
            destino.writestr(nome, dados)
    return SAIDA


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    assert set(SEQ) == {uc["id"] for uc in spec.UC}
    caminho = monta()
    print(caminho, os.path.getsize(caminho) // 1024, "KB;", len(MIDIA.itens), "imagens")
    for pendente in MIDIA.pendentes:
        print("PENDENTE:", pendente)
