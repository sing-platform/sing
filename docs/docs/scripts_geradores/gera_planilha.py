import os
import zipfile
from xml.sax.saxutils import escape

import spec
from caminhos import RAIZ
from casos_teste import CT

AQUI = os.path.dirname(os.path.abspath(__file__))
SAIDA = os.path.join(RAIZ, "casos-de-teste.xlsx")

COLUNAS = [
    ("CT", 8), ("Caso de uso", 10), ("Estória", 9), ("Critério de aceite", 9), ("Cenário", 30), ("Tipo", 10),
    ("Pré-condição (DADO QUE)", 42), ("Dados de entrada", 40), ("Passos", 50), ("Resultado esperado (ENTÃO)", 50),
    ("Resultado obtido", 30), ("Situação", 12),
]


def letra(indice):
    texto = ""
    indice += 1
    while indice:
        indice, resto = divmod(indice - 1, 26)
        texto = chr(65 + resto) + texto
    return texto


def celula(linha, coluna, valor, estilo):
    referencia = f"{letra(coluna)}{linha}"
    if valor == "":
        return f'<c r="{referencia}" s="{estilo}"/>'
    return f'<c r="{referencia}" s="{estilo}" t="inlineStr"><is><t xml:space="preserve">{escape(valor)}</t></is></c>'


def linhas():
    uc_por_us = {uc["us"]: uc["id"] for uc in spec.UC}
    us_por_id = {us["id"]: us for us in spec.US}
    for numero, (us_id, criterio, cenario, tipo, dados, passos) in enumerate(CT, 1):
        dado, _, entao = us_por_id[us_id]["ca"][criterio - 1]
        etapas = [p.strip() for p in passos.split("|")]
        if not etapas[-1].lower().startswith(("conferir", "observar")):
            etapas.append("Conferir o resultado esperado")
        lista = "\n".join(f"{i}. {p}" for i, p in enumerate(etapas, 1))
        yield [f"CT{numero:03d}", uc_por_us[us_id], us_id, f"CA{criterio}", cenario, tipo,
               dado[0].upper() + dado[1:] + ".", dados, lista, entao[0].upper() + entao[1:], "", "Não executado"]


def planilha():
    xml = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
           '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">',
           '<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>',
           "<cols>" + "".join(f'<col min="{i + 1}" max="{i + 1}" width="{w}" customWidth="1"/>' for i, (_, w) in enumerate(COLUNAS)) + "</cols>",
           "<sheetData>",
           '<row r="1">' + "".join(celula(1, i, nome, 1) for i, (nome, _) in enumerate(COLUNAS)) + "</row>"]
    total = 1
    for indice, valores in enumerate(linhas(), 2):
        xml.append(f'<row r="{indice}">' + "".join(celula(indice, i, v, 2) for i, v in enumerate(valores)) + "</row>")
        total = indice
    xml.append("</sheetData>")
    xml.append(f'<autoFilter ref="A1:{letra(len(COLUNAS) - 1)}{total}"/>')
    xml.append("</worksheet>")
    return "".join(xml)


ESTILOS = (
    '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    '<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
    '<fonts count="2"><font><sz val="10"/><name val="Arial"/></font><font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Arial"/></font></fonts>'
    '<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>'
    '<fill><patternFill patternType="solid"><fgColor rgb="FF0F766E"/><bgColor indexed="64"/></patternFill></fill></fills>'
    '<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border>'
    '<border><left style="thin"><color rgb="FFCBD5E1"/></left><right style="thin"><color rgb="FFCBD5E1"/></right>'
    '<top style="thin"><color rgb="FFCBD5E1"/></top><bottom style="thin"><color rgb="FFCBD5E1"/></bottom><diagonal/></border></borders>'
    '<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>'
    '<cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>'
    '<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">'
    '<alignment vertical="center" wrapText="1"/></xf>'
    '<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">'
    '<alignment vertical="top" wrapText="1"/></xf></cellXfs>'
    '<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>'
)

ARQUIVOS = {
    "[Content_Types].xml": (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        '<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>'
        "</Types>"
    ),
    "_rels/.rels": (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>'
        "</Relationships>"
    ),
    "xl/workbook.xml": (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        '<sheets><sheet name="Casos de teste" sheetId="1" r:id="rId1"/></sheets>'
        '<definedNames><definedName name="_xlnm._FilterDatabase" localSheetId="0" hidden="1">\'Casos de teste\'!$A$1:$L$'
        + str(len(CT) + 1) + "</definedName></definedNames></workbook>"
    ),
    "xl/_rels/workbook.xml.rels": (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>'
        '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>'
        "</Relationships>"
    ),
    "xl/styles.xml": ESTILOS,
}

if __name__ == "__main__":
    ARQUIVOS["xl/worksheets/sheet1.xml"] = planilha()
    with zipfile.ZipFile(SAIDA, "w", zipfile.ZIP_DEFLATED) as destino:
        for nome, conteudo in ARQUIVOS.items():
            destino.writestr(nome, conteudo)
    print(SAIDA, len(CT), "casos de teste")
