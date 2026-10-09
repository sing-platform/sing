from gera_modelo_dados import Svg, medir, seta_aberta, triangulo_vazio

FONTE_UC = 12.5
LARGURA, ALTURA = 1060, 1280
ESQ_X, DIR_X = 305, 755
RX, RY = 116, 25
LIMITE = (175, 168, 885, 1118)


def quebra(texto, largura):
    linhas, atual = [], ""
    for palavra in texto.split():
        candidato = (atual + " " + palavra).strip()
        if medir(candidato, FONTE_UC) <= largura or not atual:
            atual = candidato
        else:
            linhas.append(atual)
            atual = palavra
    return linhas + [atual]


def caso(svg, cx, cy, nome, rx=RX, ry=RY):
    svg.adicionar("caixas", f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="#EEF2FF" stroke="#3730A3" stroke-width="1.3"/>')
    linhas = quebra(nome, 2 * rx - 30)
    inicio = cy + 4.5 - (len(linhas) - 1) * 7.5
    for i, linha in enumerate(linhas):
        svg.texto(cx, inicio + i * 15, linha, tam=FONTE_UC, ancora="middle")
    return {"cx": cx, "cy": cy, "rx": rx, "ry": ry}


def ator(svg, x, y, nome, sistema=False):
    svg.adicionar("caixas",
                  f'<circle cx="{x}" cy="{y}" r="9" fill="#fff" stroke="#111827" stroke-width="1.5"/>'
                  f'<path d="M{x} {y + 9} V{y + 33} M{x - 15} {y + 17} H{x + 15} M{x} {y + 33} L{x - 12} {y + 50} '
                  f'M{x} {y + 33} L{x + 12} {y + 50}" stroke="#111827" stroke-width="1.5" fill="none"/>')
    cursor = y + 66
    if sistema:
        svg.texto(x, cursor, "«sistema»", tam=12, ancora="middle", cor="#444444")
        cursor += 15
    for linha in quebra(nome, 140):
        svg.texto(x, cursor, linha, tam=12.5, negrito=True, ancora="middle")
        cursor += 15
    return {"x": x, "y": y + 22}


def linha(svg, pontos, tracejada=False, camada="linhas"):
    lista = " ".join(f"{x:.1f},{y:.1f}" for x, y in pontos)
    extra = ' stroke-dasharray="6,4"' if tracejada else ""
    svg.adicionar(camada, f'<polyline points="{lista}" fill="none" stroke="#3b3b3b" stroke-width="1.2"{extra}/>')


def associa(svg, a, uc, lado):
    borda = uc["cx"] - uc["rx"] if lado == "e" else uc["cx"] + uc["rx"]
    sinal = 1 if lado == "e" else -1
    hub = a["x"] + 58 * sinal
    linha(svg, [(a["x"] + 20 * sinal, a["y"]), (hub, a["y"]), (borda, uc["cy"])])


def dependencia(svg, pontos, rotulo, pos):
    linha(svg, pontos, tracejada=True, camada="topo")
    a, b = pontos[-2], pontos[-1]
    dx, dy = b[0] - a[0], b[1] - a[1]
    comprimento = (dx * dx + dy * dy) ** 0.5
    seta_aberta(svg, b, (dx / comprimento, dy / comprimento))
    svg.texto(pos[0], pos[1], rotulo, tam=12, ancora="middle", halo=True, camada="topo", cor="#1f2937")


def pacote(svg, x, y, w, h, nome):
    svg.adicionar("fundo", f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="#FAFAFA" stroke="#9CA3AF" '
                           f'stroke-width="1.1" stroke-dasharray="5,4"/>')
    svg.texto(x + 10, y + 17, nome, tam=12.5, negrito=True, cor="#374151", camada="fundo")


def gera():
    svg = Svg(LARGURA, ALTURA)
    svg.texto(LARGURA / 2, 32, "Diagrama geral de casos de uso — SING", tam=19, negrito=True, ancora="middle")
    x0, y0, x1, y1 = LIMITE
    svg.adicionar("fundo", f'<rect x="{x0}" y="{y0}" width="{x1 - x0}" height="{y1 - y0}" fill="#fff" stroke="#111827" stroke-width="1.6"/>')
    svg.texto(x0 + 12, y0 + 22, "SING", tam=15, negrito=True, camada="fundo")

    esquerda = ["Provisionar organização", "Gerenciar organizações do parceiro",
                "Configurar a identidade visual da organização", "Configurar o vocabulário da organização",
                "Ativar e desativar módulos da organização", "Configurar regras acadêmicas da organização",
                "Gerenciar papéis e permissões", "Exportar os dados da organização", "Gerenciar usuários por convite",
                "Consultar painel gerencial", "Gerar relatórios gerenciais", "Gerar plano de pagamento da matrícula",
                "Registrar pagamento de cobrança"]
    direita = ["Manter alunos e responsáveis", "Manter turmas e oficinas", "Emitir declaração de matrícula",
               "Matricular aluno em turma", "Gerenciar lista de espera e convocação", "Registrar consentimento do responsável",
               "Consultar frequência e grade horária", "Registrar chamada", "Registrar parecer descritivo"]
    ys_esq = [292 + i * 60 for i in range(9)] + [880 + i * 60 for i in range(4)]
    ys_dir = [292 + i * 96 for i in range(9)]
    pacote(svg, ESQ_X - RX - 12, 250, 2 * RX + 24, 568, "Plataforma e personalização")
    pacote(svg, ESQ_X - RX - 12, 830, 2 * RX + 24, 278, "Gestão e financeiro")
    pacote(svg, DIR_X - RX - 12, 250, 2 * RX + 24, 858, "Acadêmico")
    uc = {}
    for nome, y in zip(esquerda, ys_esq):
        uc[nome] = caso(svg, ESQ_X, y, nome)
    for nome, y in zip(direita, ys_dir):
        uc[nome] = caso(svg, DIR_X, y, nome)
    uc["login"] = caso(svg, 530, 208, "Realizar login de usuário", rx=110, ry=22)
    uc["restringir"] = caso(svg, 530, 432, "Restringir o acesso aos dados da própria organização", rx=96, ry=40)

    usuario = ator(svg, 530, 52, "Usuário")
    linha(svg, [(530, 120), (530, 186)])
    a_esq = {
        "Operador da plataforma": ator(svg, 92, 262, "Operador da plataforma"),
        "Parceiro": ator(svg, 92, 370, "Parceiro"),
        "Administrador da escola": ator(svg, 92, 620, "Administrador da escola"),
        "Financeiro": ator(svg, 92, 932, "Financeiro"),
    }
    provedor = ator(svg, 92, 1070, "Provedor de pagamentos", sistema=True)
    a_dir = {
        "Secretaria": ator(svg, 968, 455, "Secretaria"),
        "Responsável": ator(svg, 968, 740, "Responsável"),
        "Aluno": ator(svg, 968, 858, "Aluno"),
        "Professor": ator(svg, 968, 980, "Professor"),
    }
    email = ator(svg, 530, 1150, "Serviço de e-mail", sistema=True)

    for a in a_esq.values():
        linha(svg, [(a["x"] - 20, a["y"] - 4), (16, a["y"] - 4)])
    linha(svg, [(16, a_esq["Financeiro"]["y"] - 4), (16, 96), (496, 96)])
    triangulo_vazio(svg, (512, 96), (1, 0))
    for a in a_dir.values():
        linha(svg, [(a["x"] + 20, a["y"] - 4), (1044, a["y"] - 4)])
    linha(svg, [(1044, a_dir["Professor"]["y"] - 4), (1044, 96), (564, 96)])
    triangulo_vazio(svg, (548, 96), (-1, 0))

    associa(svg, a_esq["Operador da plataforma"], uc["Provisionar organização"], "e")
    associa(svg, a_esq["Parceiro"], uc["Gerenciar organizações do parceiro"], "e")
    for nome in esquerda[2:11]:
        associa(svg, a_esq["Administrador da escola"], uc[nome], "e")
    for nome in esquerda[11:13]:
        associa(svg, a_esq["Financeiro"], uc[nome], "e")
    associa(svg, provedor, uc["Registrar pagamento de cobrança"], "e")
    for nome in direita[:5]:
        associa(svg, a_dir["Secretaria"], uc[nome], "d")
    for nome in direita[4:7]:
        associa(svg, a_dir["Responsável"], uc[nome], "d")
    associa(svg, a_dir["Aluno"], uc["Consultar frequência e grade horária"], "d")
    for nome in direita[7:9]:
        associa(svg, a_dir["Professor"], uc[nome], "d")

    lista = uc["Gerenciar lista de espera e convocação"]
    convite = uc["Gerenciar usuários por convite"]
    linha(svg, [(546, 1150), (546, lista["cy"]), (DIR_X - RX, lista["cy"])])
    linha(svg, [(514, 1150), (514, convite["cy"] + 8), (ESQ_X + RX - 2, convite["cy"] + 8)])

    prov = uc["Provisionar organização"]
    dependencia(svg, [(ESQ_X + RX, prov["cy"]), (446, prov["cy"]), (446, convite["cy"] - 10), (ESQ_X + RX - 1, convite["cy"] - 10)],
                "«include»", (446, 560))
    matr = uc["Matricular aluno em turma"]
    dependencia(svg, [(DIR_X - 40, lista["cy"] - RY), (DIR_X - 40, matr["cy"] + RY)], "«extend»", (DIR_X - 40, (lista["cy"] + matr["cy"]) / 2 + 3))
    svg.texto(DIR_X - 40, (lista["cy"] + matr["cy"]) / 2 + 19, "[turma sem vaga]", tam=11.5, ancora="middle", cor="#374151", halo=True, camada="topo")
    plano = uc["Gerar plano de pagamento da matrícula"]
    dependencia(svg, [(ESQ_X + RX, plano["cy"]), (478, plano["cy"]), (478, matr["cy"] + 12), (DIR_X - RX + 1, matr["cy"] + 12)],
                "«extend»", (520, 628))
    svg.texto(520, 644, "[módulo Financeiro ativo]", tam=11.5, ancora="middle", cor="#374151", halo=True, camada="topo")
    painel = uc["Consultar painel gerencial"]
    rel = uc["Gerar relatórios gerenciais"]
    dependencia(svg, [(ESQ_X + 40, rel["cy"] - RY), (ESQ_X + 40, painel["cy"] + RY)], "«extend»", (ESQ_X + 84, (rel["cy"] + painel["cy"]) / 2 + 4))
    alunos = uc["Manter alunos e responsáveis"]
    restr = uc["restringir"]
    dependencia(svg, [(DIR_X - RX, alunos["cy"]), (584, alunos["cy"]), (584, restr["cy"] - restr["ry"] + 6)], "«include»", (608, alunos["cy"] + 34))
    dependencia(svg, [(DIR_X - RX, matr["cy"] - 10), (600, matr["cy"] - 10), (600, restr["cy"] + 30)], "«include»", (608, matr["cy"] - 28))
    return svg.salvar("casos-de-uso.svg")


if __name__ == "__main__":
    print(gera(), LARGURA, ALTURA)
