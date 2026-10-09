import math
from xml.sax.saxutils import escape
from caminhos import diagrama

W, H = 1400, 950
INK = "#1b1b1b"
FONT = 'font-family="Arial, Helvetica, sans-serif"'
parts = []


def add(s):
    parts.append(s)


def txt(x, y, lines, size=13, anchor="middle", weight="normal", lh=16, fill=INK, halo=False, rotate=None):
    if isinstance(lines, str):
        lines = [lines]
    n = len(lines)
    y0 = y - (n - 1) * lh / 2 + size * 0.35
    spans = "".join(
        f'<tspan x="{x}" dy="{0 if i == 0 else lh}">{escape(t)}</tspan>' for i, t in enumerate(lines)
    )
    extra = ""
    if halo:
        extra += ' stroke="#ffffff" stroke-width="4" stroke-linejoin="round" paint-order="stroke"'
    if rotate is not None:
        extra += f' transform="rotate({rotate} {x} {y})"'
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


def task(cx, cy, w, h, lines):
    add(
        f'<rect x="{cx - w / 2}" y="{cy - h / 2}" width="{w}" height="{h}" rx="10" ry="10" '
        f'fill="#e4eefa" stroke="{INK}" stroke-width="1.8"/>'
    )
    txt(cx, cy, lines)
    return Box(cx, cy, w / 2, h / 2)


def envelope(cx, cy, k=1.0):
    w, h = 9 * k, 6 * k
    add(
        f'<rect x="{cx - w}" y="{cy - h}" width="{2 * w}" height="{2 * h}" fill="#ffffff" '
        f'stroke="{INK}" stroke-width="1.3"/>'
    )
    add(
        f'<polyline points="{cx - w},{cy - h} {cx},{cy + 1 * k} {cx + w},{cy - h}" fill="none" '
        f'stroke="{INK}" stroke-width="1.3"/>'
    )


def clock(cx, cy, k=1.0):
    r = 10 * k
    add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="#ffffff" stroke="{INK}" stroke-width="1.3"/>')
    for dx, dy in ((0, -1), (1, 0), (0, 1), (-1, 0)):
        add(
            f'<line x1="{cx + dx * (r - 2.5 * k)}" y1="{cy + dy * (r - 2.5 * k)}" '
            f'x2="{cx + dx * r}" y2="{cy + dy * r}" stroke="{INK}" stroke-width="1.3"/>'
        )
    add(f'<line x1="{cx}" y1="{cy}" x2="{cx}" y2="{cy - 7 * k}" stroke="{INK}" stroke-width="1.5"/>')
    add(f'<line x1="{cx}" y1="{cy}" x2="{cx + 5 * k}" y2="{cy + 2 * k}" stroke="{INK}" stroke-width="1.5"/>')


def event(kind, cx, cy, r=17, icon=None):
    if kind == "start":
        add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="#e3f3e1" stroke="{INK}" stroke-width="1.5"/>')
    elif kind == "end":
        add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="#f9dfdf" stroke="{INK}" stroke-width="4.5"/>')
    elif kind == "inter":
        add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="#fffbe6" stroke="{INK}" stroke-width="1.5"/>')
        add(f'<circle cx="{cx}" cy="{cy}" r="{r - 3.8}" fill="none" stroke="{INK}" stroke-width="1.5"/>')
    if icon == "msg":
        envelope(cx, cy, 0.85 if r < 18 else 1.0)
    elif icon == "timer":
        clock(cx, cy, 0.85)
    return Box(cx, cy, r, r)


def gateway(kind, cx, cy, s=21):
    add(
        f'<polygon points="{cx},{cy - s} {cx + s},{cy} {cx},{cy + s} {cx - s},{cy}" '
        f'fill="#fff4cf" stroke="{INK}" stroke-width="1.8"/>'
    )
    if kind == "x":
        a = 8.5
        add(f'<line x1="{cx - a}" y1="{cy - a}" x2="{cx + a}" y2="{cy + a}" stroke="{INK}" stroke-width="3.4"/>')
        add(f'<line x1="{cx - a}" y1="{cy + a}" x2="{cx + a}" y2="{cy - a}" stroke="{INK}" stroke-width="3.4"/>')
    elif kind == "plus":
        a = 11
        add(f'<line x1="{cx - a}" y1="{cy}" x2="{cx + a}" y2="{cy}" stroke="{INK}" stroke-width="3.4"/>')
        add(f'<line x1="{cx}" y1="{cy - a}" x2="{cx}" y2="{cy + a}" stroke="{INK}" stroke-width="3.4"/>')
    elif kind == "event":
        add(f'<circle cx="{cx}" cy="{cy}" r="12.5" fill="none" stroke="{INK}" stroke-width="1.4"/>')
        add(f'<circle cx="{cx}" cy="{cy}" r="9.5" fill="none" stroke="{INK}" stroke-width="1.4"/>')
        pts = []
        for i in range(5):
            ang = -math.pi / 2 + i * 2 * math.pi / 5
            pts.append(f"{cx + 6.2 * math.cos(ang):.2f},{cy + 6.2 * math.sin(ang):.2f}")
        add(f'<polygon points="{" ".join(pts)}" fill="none" stroke="{INK}" stroke-width="1.4"/>')
    return Box(cx, cy, s, s)


def seq(pts, arrow=True):
    d = "M" + " L".join(f"{x},{y}" for x, y in pts)
    mk = ' marker-end="url(#seq)"' if arrow else ""
    add(f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="1.6" stroke-linejoin="round"{mk}/>')


def msg(pts):
    d = "M" + " L".join(f"{x},{y}" for x, y in pts)
    add(
        f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="1.5" stroke-dasharray="6 4" '
        f'stroke-linejoin="round" marker-end="url(#msg)"/>'
    )
    x, y = pts[0]
    add(f'<circle cx="{x}" cy="{y}" r="4.2" fill="#ffffff" stroke="{INK}" stroke-width="1.5"/>')


RESP_TOP, RESP_BOT = 48, 224
ESC_TOP, SEC_BOT, SING_BOT, FIN_BOT = 248, 364, 706, 814
X_LANE = 76
X_END = 1380

add(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">')
add(
    '<defs>'
    f'<marker id="seq" markerWidth="12" markerHeight="10" refX="11.5" refY="5" orient="auto" markerUnits="userSpaceOnUse">'
    f'<path d="M0,0 L12,5 L0,10 z" fill="{INK}"/></marker>'
    f'<marker id="msg" markerWidth="13" markerHeight="12" refX="12" refY="6" orient="auto" markerUnits="userSpaceOnUse">'
    f'<path d="M1,1 L12,6 L1,11 z" fill="#ffffff" stroke="{INK}" stroke-width="1.3" stroke-linejoin="round"/></marker>'
    '</defs>'
)
add(f'<rect x="0" y="0" width="{W}" height="{H}" fill="#ffffff"/>')
txt(W / 2, 28, "Processo TO BE — Ingresso de aluno: do pedido de vaga à matrícula", size=20, weight="bold")

add(f'<rect x="20" y="{RESP_TOP}" width="{X_END - 20}" height="{RESP_BOT - RESP_TOP}" fill="#ffffff" stroke="{INK}" stroke-width="2"/>')
add(f'<rect x="20" y="{RESP_TOP}" width="28" height="{RESP_BOT - RESP_TOP}" fill="#dfe7f1" stroke="{INK}" stroke-width="2"/>')
txt(34, (RESP_TOP + RESP_BOT) / 2, "Responsável", size=15, weight="bold", rotate=-90)

add(f'<rect x="20" y="{ESC_TOP}" width="{X_END - 20}" height="{FIN_BOT - ESC_TOP}" fill="#ffffff" stroke="{INK}" stroke-width="2"/>')
add(f'<rect x="20" y="{ESC_TOP}" width="28" height="{FIN_BOT - ESC_TOP}" fill="#dfe7f1" stroke="{INK}" stroke-width="2"/>')
txt(34, (ESC_TOP + FIN_BOT) / 2, "Escola", size=15, weight="bold", rotate=-90)
lanes = [("Secretaria", ESC_TOP, SEC_BOT), ("SING (sistema)", SEC_BOT, SING_BOT), ("Financeiro", SING_BOT, FIN_BOT)]
for i, (nome, a, b) in enumerate(lanes):
    add(f'<rect x="48" y="{a}" width="28" height="{b - a}" fill="#eef2f7" stroke="{INK}" stroke-width="1.5"/>')
    txt(62, (a + b) / 2, nome, size=14, weight="bold", rotate=-90)
    if i > 0:
        add(f'<line x1="48" y1="{a}" x2="{X_END}" y2="{a}" stroke="{INK}" stroke-width="1.5"/>')

RY = 96
SY = 298
y1, y2, y3 = 446, 544, 650
FIN_Y = (SING_BOT + FIN_BOT) // 2

r1 = event("start", 104, RY)
r2 = task(200, RY, 116, 56, ["Pedir vaga em", "uma turma"])
r3 = task(1140, RY, 128, 56, ["Registrar", "consentimento e", "acessar o portal"])
r4 = event("end", 1300, RY)
txt(1300, RY + 17 + 16, "Aluno matriculado", halo=True)
r5 = task(940, 176, 168, 52, ["Responder à convocação"])

s1 = event("start", 200, SY, icon="msg")
txt(200, SY + 17 + 16, ["Pedido de vaga", "recebido"])
s2 = task(306, SY, 116, 56, ["Cadastrar aluno", "e responsável"])
s3 = task(540, SY, 116, 56, ["Confirmar", "matrícula"])

g1 = task(306, y1, 112, 60, ["Verificar vagas", "e regras da", "escola"])
g2 = gateway("x", 430, y1)
txt(430, y1 + 21 + 15, ["Há vaga", "na turma?"], halo=True)
g3 = gateway("plus", 540, y2)
g4 = task(658, y2, 112, 60, ["Incluir aluno", "na chamada", "da turma"])
g5 = gateway("x", 650, y3)
txt(650, y3 - 21 - 24, ["Módulo Financeiro", "ativo?"], halo=True)
f1 = task(762, FIN_Y, 124, 56, ["Gerar plano de", "pagamento"])
g6 = gateway("x", 860, y3)
g7 = gateway("plus", 905, y2)
g8 = task(1130, y2, 124, 60, ["Enviar confirmação", "e acesso ao", "portal"])

w1 = task(642, y1, 112, 60, ["Incluir na", "lista de espera"])
w2 = event("inter", 743, y1, icon="msg")
txt(743, y1 + 17 + 16, ["Vaga", "liberada"], halo=True)
w3 = task(842, y1, 112, 60, ["Convocar o", "primeiro", "da lista"])
w4 = gateway("event", 940, y1, s=20)
w5 = event("inter", 1004, y1 - 40, icon="msg")
w6 = event("inter", 1004, y1 + 40, icon="timer")
txt(1004, y1 + 40 + 17 + 15, "Prazo de 3 dias", halo=True)
txt(1004, y1 - 40 + 17 + 15, ["Resposta", "recebida"], halo=True)
w7 = gateway("x", 1080, y1)
txt(1080, y1 + 21 + 15, ["Confirmou", "no prazo?"], halo=True)
w8 = task(1236, y1, 118, 74, ["Marcar como", "‘Não respondeu’", "e convocar o", "próximo"])
w9 = event("end", 1338, y1)
txt(1338, y1 + 17 + 32, ["Vaga", "oferecida", "ao próximo"], halo=True)

seq([r1.R, r2.L])
seq([r2.R, r3.L])
seq([r3.R, r4.L])
seq([s1.R, s2.L])
seq([s2.B, g1.T])
seq([g1.R, g2.L])
seq([g2.T, (g2.cx, SY), s3.L])
txt(g2.cx + 8, y1 - 40, "Sim", anchor="start", halo=True)
seq([g2.R, w1.L])
txt(g2.cx + 28, y1 - 9, "Não", anchor="start", halo=True)
seq([s3.B, g3.T])
seq([g3.R, g4.L])
seq([g3.B, (g3.cx, y3), g5.L])
seq([g4.R, g7.L])
seq([g5.R, g6.L])
txt(g5.cx + 60, y3 - 10, "Não", halo=True)
seq([g5.B, (g5.cx, FIN_Y), f1.L])
txt(g5.cx + 9, g5.cy + 36, "Sim", anchor="start", halo=True)
seq([f1.R, (g6.cx, FIN_Y), g6.B])
seq([g6.R, (g7.cx, y3), g7.B])
seq([g7.R, g8.L])

seq([w1.R, w2.L])
seq([w2.R, w3.L])
seq([w3.R, w4.L])
seq([w4.T, (w4.cx, w5.cy), w5.L])
seq([w4.B, (w4.cx, w6.cy), w6.L])
seq([w5.R, (1042, w5.cy), (1042, y1), w7.L])
seq([w6.R, (1042, w6.cy), (1042, y1)], arrow=False)
seq([w7.T, (w7.cx, SY), s3.R])
txt(w7.cx + 8, y1 - 40, "Sim", anchor="start", halo=True)
seq([w7.R, w8.L])
txt(w7.cx + 23, y1 - 9, "Não", anchor="start", halo=True)
seq([w8.R, w9.L])

msg([r2.bottom_at(200), s1.T])
msg([g8.top_at(1144), r3.bottom_at(1144)])
msg([w3.top_at(876), r5.bottom_at(876)])
msg([r5.bottom_at(1004), w5.T])

LEG_Y = 826
add(f'<rect x="20" y="{LEG_Y}" width="{X_END - 20}" height="{H - 8 - LEG_Y}" fill="#ffffff" stroke="{INK}" stroke-width="1.2"/>')
txt(34, LEG_Y + 20, "Legenda", anchor="start", size=14, weight="bold")

cell = 232
x0 = 44


def lg(col, row, drawer, label):
    cx = x0 + col * cell
    cy = LEG_Y + 48 + row * 40
    drawer(cx + 18, cy)
    txt(cx + 52, cy, label, anchor="start")


def d_start(x, y):
    event("start", x, y, r=14)


def d_msgstart(x, y):
    event("start", x, y, r=14, icon="msg")


def d_msginter(x, y):
    event("inter", x, y, r=14, icon="msg")


def d_timer(x, y):
    event("inter", x, y, r=14, icon="timer")


def d_end(x, y):
    event("end", x, y, r=14)


def d_task(x, y):
    add(
        f'<rect x="{x - 20}" y="{y - 13}" width="40" height="26" rx="6" fill="#e4eefa" '
        f'stroke="{INK}" stroke-width="1.6"/>'
    )


def d_gx(x, y):
    gateway("x", x, y, s=16)


def d_gp(x, y):
    gateway("plus", x, y, s=16)


def d_ge(x, y):
    gateway("event", x, y, s=16)


def d_seq(x, y):
    seq([(x - 20, y), (x + 22, y)])


def d_msg(x, y):
    msg([(x - 20, y), (x + 22, y)])


lg(0, 0, d_start, "Evento de início")
lg(1, 0, d_msgstart, "Início por mensagem")
lg(2, 0, d_msginter, "Mensagem (intermediário)")
lg(3, 0, d_timer, "Temporizador")
lg(4, 0, d_end, "Evento de fim")
lg(5, 0, d_task, "Tarefa")
lg(0, 1, d_gx, "Gateway exclusivo")
lg(1, 1, d_gp, "Gateway paralelo")
lg(2, 1, d_ge, "Gateway baseado em evento")
lg(3.2, 1, d_seq, "Fluxo de sequência")
lg(4.2, 1, d_msg, "Fluxo de mensagem")

add("</svg>")

destino = diagrama("bpmn-ingresso.svg")
with open(destino, "w", encoding="utf-8") as f:
    f.write("\n".join(parts))
