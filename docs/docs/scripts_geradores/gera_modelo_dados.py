import math
import os
from xml.sax.saxutils import escape

from PIL import ImageFont

from caminhos import diagrama

FONTE = "Arial, Helvetica, sans-serif"
PASTA = os.path.dirname(os.path.abspath(__file__))
FONTES_TTF = {
    (False, False): "arial.ttf",
    (True, False): "arialbd.ttf",
    (False, True): "ariali.ttf",
    (True, True): "arialbi.ttf",
}
_cache_fontes = {}

TINTA = "#222222"
LINHA = "#3b3b3b"
LINHA_CAIXA = "#4a4a4a"

GRUPOS = {
    "plataforma": ("#d6e6f7", "Plataforma e organização"),
    "acesso": ("#e4dcf3", "Identidade e acesso"),
    "auditoria": ("#e6e6e6", "Auditoria"),
    "pessoas": ("#d6e6f7", "Pessoas"),
    "oferta": ("#d9efd6", "Oferta e matrícula"),
    "frequencia": ("#e4dcf3", "Frequência e avaliação"),
    "financeiro": ("#fbe9c4", "Financeiro"),
    "consentimento": ("#f8dce2", "Consentimento"),
}


def medir(texto, tam, negrito=False, italico=False):
    chave = (tam, negrito, italico)
    if chave not in _cache_fontes:
        caminho = os.path.join(r"C:\Windows\Fonts", FONTES_TTF[(negrito, italico)])
        _cache_fontes[chave] = ImageFont.truetype(caminho, tam)
    return _cache_fontes[chave].getlength(texto) * 1.03


class Svg:
    def __init__(self, largura, altura):
        self.w = largura
        self.h = altura
        self.camadas = {"fundo": [], "linhas": [], "caixas": [], "topo": []}

    def adicionar(self, camada, fragmento):
        self.camadas[camada].append(fragmento)

    def texto(self, x, y, conteudo, tam=12.5, negrito=False, italico=False,
              ancora="start", cor=TINTA, sublinhado=False, halo=False, camada="caixas"):
        atributos = [
            f'x="{x:.1f}"', f'y="{y:.1f}"', f'font-size="{tam}"', f'text-anchor="{ancora}"',
            f'fill="{cor}"',
        ]
        if negrito:
            atributos.append('font-weight="bold"')
        if italico:
            atributos.append('font-style="italic"')
        if sublinhado:
            atributos.append('text-decoration="underline"')
        if halo:
            atributos.append('stroke="#ffffff" stroke-width="3.2" stroke-linejoin="round" paint-order="stroke"')
        self.adicionar(camada, f'<text {" ".join(atributos)}>{escape(conteudo)}</text>')

    def retangulo(self, x, y, w, h, preenchimento="#ffffff", borda=LINHA_CAIXA, espessura=1.3, camada="caixas"):
        self.adicionar(
            camada,
            f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" fill="{preenchimento}" '
            f'stroke="{borda}" stroke-width="{espessura}"/>',
        )

    def linha(self, x1, y1, x2, y2, cor=LINHA_CAIXA, espessura=1.0, camada="caixas", tracejado=None):
        extra = f' stroke-dasharray="{tracejado}"' if tracejado else ""
        self.adicionar(
            camada,
            f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{cor}" '
            f'stroke-width="{espessura}"{extra}/>',
        )

    def poligono(self, pontos, preenchimento, borda=LINHA, espessura=1.3, camada="topo"):
        lista = " ".join(f"{x:.1f},{y:.1f}" for x, y in pontos)
        self.adicionar(
            camada,
            f'<polygon points="{lista}" fill="{preenchimento}" stroke="{borda}" stroke-width="{espessura}" '
            f'stroke-linejoin="miter"/>',
        )

    def renderizar(self):
        corpo = "\n".join(
            fragmento for camada in ("fundo", "linhas", "caixas", "topo") for fragmento in self.camadas[camada]
        )
        return (
            '<?xml version="1.0" encoding="UTF-8"?>\n'
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" '
            f'viewBox="0 0 {self.w} {self.h}" font-family="{FONTE}">\n'
            f'<rect width="{self.w}" height="{self.h}" fill="#ffffff"/>\n{corpo}\n</svg>\n'
        )

    def salvar(self, nome):
        caminho = diagrama(nome)
        with open(caminho, "w", encoding="utf-8") as arquivo:
            arquivo.write(self.renderizar())
        return caminho


class Caixa:
    def __init__(self, x, y, w, h):
        self.x = x
        self.y = y
        self.w = w
        self.h = h

    def pt(self, lado, d=None):
        if lado in ("n", "s"):
            if d is None:
                d = self.w / 2
            elif d < 0:
                d = self.w + d
            return (self.x + d, self.y if lado == "n" else self.y + self.h)
        if d is None:
            d = self.h / 2
        elif d < 0:
            d = self.h + d
        return (self.x if lado == "w" else self.x + self.w, self.y + d)

    @property
    def direita(self):
        return self.x + self.w

    @property
    def base(self):
        return self.y + self.h


def _unitario(a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]
    comprimento = math.hypot(dx, dy)
    return dx / comprimento, dy / comprimento


def _verificar_ortogonal(pontos):
    for a, b in zip(pontos, pontos[1:]):
        if abs(a[0] - b[0]) > 0.01 and abs(a[1] - b[1]) > 0.01:
            raise ValueError(f"segmento nao ortogonal: {a} -> {b}")


def seta_aberta(svg, ponta, direcao, tam=11, abertura=5.5):
    dx, dy = direcao
    bx, by = ponta[0] - dx * tam, ponta[1] - dy * tam
    px, py = -dy, dx
    svg.adicionar(
        "topo",
        f'<polyline points="{bx + px * abertura:.1f},{by + py * abertura:.1f} {ponta[0]:.1f},{ponta[1]:.1f} '
        f'{bx - px * abertura:.1f},{by - py * abertura:.1f}" fill="none" stroke="{LINHA}" stroke-width="1.3" '
        f'stroke-linejoin="miter"/>',
    )


def triangulo_vazio(svg, ponta, direcao, tam=15, abertura=8):
    dx, dy = direcao
    bx, by = ponta[0] - dx * tam, ponta[1] - dy * tam
    px, py = -dy, dx
    svg.poligono(
        [ponta, (bx + px * abertura, by + py * abertura), (bx - px * abertura, by - py * abertura)],
        "#ffffff",
    )


def losango(svg, ponta, direcao, cheio=True, tam=24, abertura=6.5):
    dx, dy = direcao
    mx, my = ponta[0] - dx * tam / 2, ponta[1] - dy * tam / 2
    fx, fy = ponta[0] - dx * tam, ponta[1] - dy * tam
    px, py = -dy, dx
    svg.poligono(
        [ponta, (mx + px * abertura, my + py * abertura), (fx, fy), (mx - px * abertura, my - py * abertura)],
        LINHA if cheio else "#ffffff",
    )


def rotulo_extremo(svg, ponto, direcao, texto, lado=1, dist=0, tam=12.5):
    dx, dy = direcao
    if abs(dx) > abs(dy):
        x = ponto[0] + dx * (8 + dist)
        y = ponto[1] - 8 if lado > 0 else ponto[1] + 18
        ancora = "start" if dx > 0 else "end"
    else:
        y = ponto[1] + dy * dist + (15 if dy > 0 else -6)
        x = ponto[0] + (9 if lado > 0 else -9)
        ancora = "start" if lado > 0 else "end"
    svg.texto(x, y, texto, tam=tam, ancora=ancora, halo=True, camada="topo")


DECORACOES = {
    "seta": seta_aberta,
    "triangulo": triangulo_vazio,
    "losango": lambda svg, ponta, direcao: losango(svg, ponta, direcao, True),
    "losango_vazio": lambda svg, ponta, direcao: losango(svg, ponta, direcao, False),
}


def relacao(svg, pontos, m_ini=None, m_fim=None, deco_ini=None, deco_fim=None, tracejada=False,
            lado_ini=1, lado_fim=1, dist_ini=0, dist_fim=0):
    _verificar_ortogonal(pontos)
    lista = " ".join(f"{x:.1f},{y:.1f}" for x, y in pontos)
    extra = ' stroke-dasharray="6,4"' if tracejada else ""
    svg.adicionar(
        "linhas",
        f'<polyline points="{lista}" fill="none" stroke="{LINHA}" stroke-width="1.3"{extra} '
        f'stroke-linejoin="miter"/>',
    )
    saida_ini = _unitario(pontos[0], pontos[1])
    chegada_fim = _unitario(pontos[-2], pontos[-1])
    if deco_ini:
        DECORACOES[deco_ini](svg, pontos[0], (-saida_ini[0], -saida_ini[1]))
    if deco_fim:
        DECORACOES[deco_fim](svg, pontos[-1], chegada_fim)
    if m_ini:
        rotulo_extremo(svg, pontos[0], saida_ini, m_ini, lado_ini, dist_ini)
    if m_fim:
        rotulo_extremo(svg, pontos[-1], (-chegada_fim[0], -chegada_fim[1]), m_fim, lado_fim, dist_fim)


TAM_ATRIBUTO = 12.5
TAM_NOME = 13.5
ALTURA_LINHA = 18.5
ALTURA_CABECALHO = 43
MARGEM_X = 10


class Tabela(Caixa):
    def __init__(self, nome, colunas, grupo, x, y, largura_minima=0):
        self.nome = nome
        self.colunas = colunas
        self.grupo = grupo
        marcador_max = max((medir(m, TAM_ATRIBUTO) for m, _ in colunas if m), default=0)
        self.col_marcador = marcador_max + 8 if marcador_max else 0
        texto_max = max(medir(t, TAM_ATRIBUTO) for _, t in colunas)
        largura = max(
            2 * MARGEM_X + self.col_marcador + texto_max,
            2 * MARGEM_X + medir(nome, TAM_NOME, negrito=True),
            largura_minima,
        )
        altura = ALTURA_CABECALHO + len(colunas) * ALTURA_LINHA + 10
        super().__init__(x, y, math.ceil(largura / 2) * 2, altura)

    def desenhar(self, svg):
        cor = GRUPOS[self.grupo][0]
        svg.retangulo(self.x, self.y, self.w, self.h)
        svg.retangulo(self.x, self.y, self.w, ALTURA_CABECALHO, preenchimento=cor)
        centro = self.x + self.w / 2
        svg.texto(centro, self.y + 17, "«table»", tam=TAM_ATRIBUTO, ancora="middle", cor="#444444")
        svg.texto(centro, self.y + 35, self.nome, tam=TAM_NOME, negrito=True, ancora="middle")
        y = self.y + ALTURA_CABECALHO + 6 + 13
        for marcador, texto in self.colunas:
            if marcador:
                svg.texto(self.x + MARGEM_X, y, marcador, tam=TAM_ATRIBUTO, cor="#444444")
            svg.texto(self.x + MARGEM_X + self.col_marcador, y, texto, tam=TAM_ATRIBUTO)
            y += ALTURA_LINHA


def titulo(svg, texto, y=34):
    svg.texto(svg.w / 2, y, texto, tam=20, negrito=True, ancora="middle")


def legenda(svg, x, y, grupos, observacoes=(), direita=None, itens_extra=()):
    larguras = [medir(GRUPOS[g][1], 12.5) + 24 for g in grupos] + [medir(o, 12.5) for o in observacoes]
    largura = 20 + max(larguras) + 10
    if direita is not None:
        x = direita - largura
    altura = 36 + (len(grupos) + len(observacoes)) * 20
    svg.retangulo(x, y, largura, altura, borda="#8a8a8a", espessura=1)
    svg.texto(x + 10, y + 19, "Legenda", tam=12.5, negrito=True)
    cursor = y + 30
    for g in grupos:
        svg.retangulo(x + 10, cursor + 3, 16, 12, preenchimento=GRUPOS[g][0], espessura=1)
        svg.texto(x + 34, cursor + 14, GRUPOS[g][1], tam=12.5)
        cursor += 20
    for observacao in observacoes:
        svg.texto(x + 10, cursor + 14, observacao, tam=12.5)
        cursor += 20
    return largura, altura


def nota(svg, x, y, w, linhas, tam=12.5):
    altura = 14 + len(linhas) * 17
    dobra = 14
    pontos = [(x, y), (x + w - dobra, y), (x + w, y + dobra), (x + w, y + altura), (x, y + altura)]
    svg.poligono(pontos, "#fffbe0", borda=LINHA_CAIXA, espessura=1.2, camada="caixas")
    svg.adicionar(
        "caixas",
        f'<polyline points="{x + w - dobra:.1f},{y:.1f} {x + w - dobra:.1f},{y + dobra:.1f} '
        f'{x + w:.1f},{y + dobra:.1f}" fill="none" stroke="{LINHA_CAIXA}" stroke-width="1.2"/>',
    )
    cursor = y + 20
    for linha in linhas:
        svg.texto(x + 10, cursor, linha, tam=tam)
        cursor += 17
    return altura


def dados_plataforma():
    return {
        "partner": [("«PK»", "id: UUID"), ("", "name: VARCHAR(120)"), ("«UK»", "slug: VARCHAR(40)")],
        "organization": [
            ("«PK»", "id: UUID"), ("«FK»", "partner_id: UUID [0..1]"), ("", "name: VARCHAR(120)"),
            ("«UK»", "slug: VARCHAR(40)"), ("", "profile: VARCHAR(30)"), ("", "status: VARCHAR(20)"),
            ("", "created_at: TIMESTAMP"),
        ],
        "organization_domain": [
            ("«PK»", "id: BIGINT"), ("«FK»", "tenant_id: UUID"), ("«UK»", "host: VARCHAR(255)"),
            ("", "verified: BOOLEAN"),
        ],
        "organization_setting": [
            ("«PK»«FK»", "tenant_id: UUID"), ("", "display_name: VARCHAR(120)"),
            ("", "logo_key: VARCHAR(255)"), ("", "primary_color: CHAR(7)"), ("", "vocabulary: JSONB"),
            ("", "rules: JSONB"), ("", "updated_at: TIMESTAMP"),
        ],
        "organization_module": [
            ("«PK»«FK»", "tenant_id: UUID"), ("«PK»", "module: VARCHAR(40)"), ("", "active: BOOLEAN"),
        ],
        "unit": [
            ("«PK»", "id: BIGINT"), ("«FK»", "tenant_id: UUID"), ("", "name: VARCHAR(120)"),
            ("", "active: BOOLEAN"),
        ],
        "users": [
            ("«PK»", "id: BIGINT"), ("«FK»", "tenant_id: UUID"), ("", "email: VARCHAR(255)"),
            ("", "password_hash: VARCHAR(100)"), ("", "status: VARCHAR(20)"),
            ("", "last_login_at: TIMESTAMP"), ("«UK»", "(tenant_id, email)"),
        ],
        "invitation": [
            ("«PK»", "id: BIGINT"), ("«FK»", "tenant_id: UUID"), ("«FK»", "user_id: BIGINT"),
            ("", "token_hash: CHAR(64)"), ("", "expires_at: TIMESTAMP"), ("", "used_at: TIMESTAMP"),
        ],
        "role": [
            ("«PK»", "id: BIGINT"), ("«FK»", "tenant_id: UUID"), ("", "name: VARCHAR(60)"),
            ("«UK»", "(tenant_id, name)"),
        ],
        "permission": [
            ("«PK»", "code: VARCHAR(60)"), ("", "module: VARCHAR(40)"), ("", "description: VARCHAR(160)"),
        ],
        "role_permission": [("«PK»«FK»", "role_id: BIGINT"), ("«PK»«FK»", "permission_code: VARCHAR(60)")],
        "user_role": [("«PK»«FK»", "user_id: BIGINT"), ("«PK»«FK»", "role_id: BIGINT")],
        "audit_log": [
            ("«PK»", "id: BIGINT"), ("«FK»", "tenant_id: UUID"), ("«FK»", "user_id: BIGINT"),
            ("", "entity: VARCHAR(60)"), ("", "entity_id: VARCHAR(40)"), ("", "action: VARCHAR(20)"),
            ("", "old_value: JSONB"), ("", "new_value: JSONB"), ("", "created_at: TIMESTAMP"),
        ],
    }


OBSERVACOES_CHAVES = [
    "«PK» chave primária; «FK» chave estrangeira",
    "«UK» restrição de unicidade",
    "Seta: da tabela com a FK para a referenciada",
]


def referencia(svg, pontos, m_fk, m_pk, **extras):
    relacao(svg, pontos, m_ini=m_fk, m_fim=m_pk, deco_fim="seta", **extras)


def diagrama_plataforma():
    svg = Svg(1000, 1170)
    titulo(svg, "Modelo de dados — Plataforma, organização e acesso")
    colunas = dados_plataforma()
    grupo_de = {
        "partner": "plataforma", "organization": "plataforma", "organization_domain": "plataforma",
        "organization_setting": "plataforma", "organization_module": "plataforma", "unit": "plataforma",
        "users": "acesso", "invitation": "acesso", "role": "acesso", "permission": "acesso",
        "role_permission": "acesso", "user_role": "acesso", "audit_log": "auditoria",
    }
    posicoes = {
        "partner": ("w", 30, 70), "organization_domain": ("c", 515, 70), "organization_module": ("e", 970, 70),
        "organization_setting": ("w", 30, 260), "organization": ("c", 515, 260), "unit": ("e", 970, 260),
        "role": ("w", 30, 520), "users": ("c", 515, 520), "audit_log": ("e", 970, 520),
        "role_permission": ("w", 30, 800), "user_role": ("c", 515, 800), "invitation": ("e", 970, 800),
        "permission": ("w", 30, 970),
    }
    t = {}
    for nome, (ancora, x, y) in posicoes.items():
        tabela = Tabela(nome, colunas[nome], grupo_de[nome], 0, y)
        if ancora == "w":
            tabela.x = x
        elif ancora == "c":
            tabela.x = x - tabela.w / 2
        else:
            tabela.x = x - tabela.w
        t[nome] = tabela

    org = t["organization"]
    x_esq, x_dir = 353, 690
    referencia(svg, [t["organization_domain"].pt("s"), org.pt("n")], "0..*", "1")
    referencia(svg, [org.pt("w", 35), (x_esq, org.y + 35), (x_esq, 110), t["partner"].pt("e", 40)], "0..*", "0..1")
    referencia(svg, [t["organization_setting"].pt("e", 110), org.pt("w", 110)], "1", "1")
    referencia(svg, [t["organization_module"].pt("w", 40), (x_dir, 110), (x_dir, org.y + 35), org.pt("e", 35)], "0..*", "1")
    referencia(svg, [t["unit"].pt("w", 110), org.pt("e", 110)], "1..*", "1")

    y_corredor = 482
    referencia(svg, [t["role"].pt("n"), (t["role"].pt("n")[0], y_corredor), (org.x + 30, y_corredor), org.pt("s", 30)], "0..*", "1")
    referencia(svg, [t["users"].pt("n"), org.pt("s")], "0..*", "1")
    referencia(svg, [t["audit_log"].pt("n"), (t["audit_log"].pt("n")[0], y_corredor), (org.x + org.w - 30, y_corredor), org.pt("s", -30)], "0..*", "1")

    referencia(svg, [t["audit_log"].pt("w", 80), t["users"].pt("e", 80)], "0..*", "0..1")
    referencia(svg, [t["invitation"].pt("n"), (t["invitation"].pt("n")[0], 770), (t["users"].pt("s", -34)[0], 770), t["users"].pt("s", -34)], "0..*", "1")
    referencia(svg, [t["user_role"].pt("n"), t["users"].pt("s")], "0..*", "1")
    referencia(svg, [t["user_role"].pt("w", 60), (353, t["user_role"].y + 60), (353, 585), t["role"].pt("e", 65)], "0..*", "1")
    referencia(svg, [t["role_permission"].pt("n", 95), t["role"].pt("s", 95)], "0..*", "1")
    referencia(svg, [t["role_permission"].pt("s", 115), t["permission"].pt("n", 115)], "0..*", "1")

    for tabela in t.values():
        tabela.desenhar(svg)

    legenda(
        svg, 0, 995, ["plataforma", "acesso", "auditoria"], OBSERVACOES_CHAVES, direita=970,
    )
    return svg.salvar("modelo-dados-plataforma.svg")


if __name__ == "__main__":
    print(diagrama_plataforma())
