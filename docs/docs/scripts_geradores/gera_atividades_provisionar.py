from xml.sax.saxutils import escape
from caminhos import diagrama

W, H = 1000, 1330
INK = "#1b1b1b"
FONT = 'font-family="Arial, Helvetica, sans-serif"'
parts = []


def add(s):
    parts.append(s)


def txt(x, y, lines, size=13, anchor="middle", weight="normal", lh=16, fill=INK, halo=False):
    if isinstance(lines, str):
        lines = [lines]
    n = len(lines)
    y0 = y - (n - 1) * lh / 2 + size * 0.35
    spans = "".join(
        f'<tspan x="{x}" dy="{0 if i == 0 else lh}">{escape(t)}</tspan>' for i, t in enumerate(lines)
    )
    extra = ""
    if halo:
        extra = ' stroke="#ffffff" stroke-width="4" stroke-linejoin="round" paint-order="stroke"'
    add(
        f'<text x="{x}" y="{y0}" {FONT} font-size="{size}" font-weight="{weight}" '
        f'text-anchor="{anchor}" fill="{fill}"{extra}>{spans}</text>'
    )


class Box:
    def __init__(self, cx, cy, hw, hh):
        self.cx, self.cy, self.hw, self.hh = cx, cy, hw, hh

    @property
    def L(self):
        return (self.cx - self.hw, self.cy)

    @property
    def R(self):
        return (self.cx + self.hw, self.cy)

    @property
    def T(self):
        return (self.cx, self.cy - self.hh)

    @property
    def B(self):
        return (self.cx, self.cy + self.hh)

    def top_at(self, x):
        return (x, self.cy - self.hh)

    def bottom_at(self, x):
        return (x, self.cy + self.hh)


def action(cx, cy, w, h, lines):
    add(
        f'<rect x="{cx - w / 2}" y="{cy - h / 2}" width="{w}" height="{h}" rx="22" ry="22" '
        f'fill="#e8f0fa" stroke="{INK}" stroke-width="1.8"/>'
    )
    txt(cx, cy, lines)
    return Box(cx, cy, w / 2, h / 2)


def initial(cx, cy, r=12):
    add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{INK}"/>')
    return Box(cx, cy, r, r)


def final(cx, cy, r=15):
    add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="#ffffff" stroke="{INK}" stroke-width="2"/>')
    add(f'<circle cx="{cx}" cy="{cy}" r="{r - 5.5}" fill="{INK}"/>')
    return Box(cx, cy, r, r)


def diamond(cx, cy, s=16):
    add(
        f'<polygon points="{cx},{cy - s} {cx + s},{cy} {cx},{cy + s} {cx - s},{cy}" '
        f'fill="#fff4cf" stroke="{INK}" stroke-width="1.8"/>'
    )
    return Box(cx, cy, s, s)


def bar(x0, x1, cy, h=8):
    add(f'<rect x="{x0}" y="{cy - h / 2}" width="{x1 - x0}" height="{h}" fill="{INK}"/>')
    return Box((x0 + x1) / 2, cy, (x1 - x0) / 2, h / 2)


def flow(pts):
    d = "M" + " L".join(f"{x},{y}" for x, y in pts)
    add(
        f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="1.7" stroke-linejoin="round" '
        f'marker-end="url(#arr)"/>'
    )


X0, X1, X2, X3 = 10, 250, 770, 990
HEAD_T, HEAD_B = 52, 92
BODY_B = 1304

add(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">')
add(
    '<defs>'
    '<marker id="arr" markerWidth="12" markerHeight="12" refX="10.5" refY="6" orient="auto" markerUnits="userSpaceOnUse">'
    f'<path d="M1,1.5 L10.5,6 L1,10.5" fill="none" stroke="{INK}" stroke-width="1.7" stroke-linejoin="miter"/></marker>'
    '</defs>'
)
add(f'<rect x="0" y="0" width="{W}" height="{H}" fill="#ffffff"/>')
txt(W / 2, 28, "Diagrama de atividades — Provisionar organização e primeiro acesso", size=20, weight="bold")

lanes = [(X0, X1, "Operador da plataforma", "#ffffff"), (X1, X2, "SING (sistema)", "#f7f9fc"), (X2, X3, "Administrador da escola", "#ffffff")]
for a, b, nome, fill in lanes:
    add(f'<rect x="{a}" y="{HEAD_T}" width="{b - a}" height="{BODY_B - HEAD_T}" fill="{fill}" stroke="{INK}" stroke-width="2"/>')
    add(f'<rect x="{a}" y="{HEAD_T}" width="{b - a}" height="{HEAD_B - HEAD_T}" fill="#dfe7f1" stroke="{INK}" stroke-width="2"/>')
    txt((a + b) / 2, (HEAD_T + HEAD_B) / 2, nome, size=15, weight="bold")

CL1, CL2, CL3 = (X0 + X1) // 2, (X1 + X2) // 2, (X2 + X3) // 2

n0 = initial(CL1, 124)
a1 = action(CL1, 192, 200, 60, ["Informar dados da", "organização e escolher", "o perfil"])
m1 = diamond(CL1, 268)
a2 = action(CL2, 268, 230, 60, ["Validar identificador de", "endereço e e-mail"])
d1 = diamond(CL2, 366)
a3 = action(CL1, 366, 200, 56, ["Corrigir os dados"])
fk = bar(272, 748, 434)
p1 = action(345, 506, 150, 72, ["Criar organização", "e unidade"])
p2 = action(CL2, 506, 150, 72, ["Copiar papéis-modelo", "do perfil"])
p3 = action(675, 506, 150, 72, ["Aplicar módulos,", "vocabulário e regras", "do perfil"])
jn = bar(272, 748, 578)
a4 = action(CL2, 646, 260, 56, ["Criar administrador com", "convite pendente"])
m2 = diamond(CL2, 722, 16)
a5 = action(CL2, 796, 230, 56, ["Enviar e-mail de convite"])
a6 = action(CL3, 796, 190, 56, ["Abrir o link do convite"])
d2 = diamond(CL3, 880)
a7 = action(CL1, 880, 190, 56, ["Reenviar convite"])
a8 = action(CL3, 962, 190, 56, ["Definir senha"])
a9 = action(CL2, 1040, 230, 56, ["Ativar a conta"])
a10 = action(CL3, 1118, 190, 56, ["Configurar identidade", "visual"])
a11 = action(CL2, 1196, 250, 56, ["Publicar tela de login", "com a marca da escola"])
nf = final(CL2, 1272)

flow([n0.B, a1.T])
flow([a1.B, m1.T])
flow([m1.R, a2.L])
flow([a2.B, d1.T])
flow([d1.L, a3.R])
txt(370, 354, "[dados inválidos]", halo=True)
flow([a3.T, m1.B])
flow([d1.B, (CL2, 434 - 4)])
txt(CL2 + 10, 402, "[dados válidos]", anchor="start", halo=True)

for p in (p1, p2, p3):
    flow([(p.cx, 434 + 4), p.T])
    flow([p.B, (p.cx, 578 - 4)])

flow([(CL2, 578 + 4), a4.T])
flow([a4.B, m2.T])
flow([m2.B, a5.T])
flow([a5.R, a6.L])
flow([a6.B, d2.T])
flow([d2.L, a7.R])
txt(540, 868, "[convite expirado]", halo=True)
flow([a7.T, (CL1, 722), m2.L])
flow([d2.B, a8.T])
txt(CL3 + 10, 922, "[convite válido]", anchor="start", halo=True)
flow([a8.B, (CL3, 1040), a9.R])
flow([a9.B, (CL2, 1118), a10.L])
flow([a10.B, (CL3, 1196), a11.R])
flow([a11.B, nf.T])

add("</svg>")

destino = diagrama("atividades-provisionar.svg")
with open(destino, "w", encoding="utf-8") as f:
    f.write("\n".join(parts))
