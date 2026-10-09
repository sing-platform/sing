from gera_modelo_academico import referencia_simples
from gera_modelo_dados import (
    GRUPOS,
    OBSERVACOES_CHAVES,
    Svg,
    Tabela,
    legenda,
    medir,
    nota,
    referencia,
    titulo,
)

GRUPOS["avaliacao"] = ("#e4dcf3", "Avaliação e resultados")

LARGURA = 1040
ORIGEM_Y = 70
ESPACO = 70
MARGEM = 20
TAM_TEXTO = 12.5
TENANT = ("«FK»", "tenant_id: UUID")
LINHAS_NOTAS = [
    ["Toda tabela tem tenant_id «FK» → organization.id, com segurança em nível de linha."],
    ["status de final_result: APROVADO, APROVADO_CONSELHO, REPROVADO_NOTA,",
     "REPROVADO_FALTA, TRANSFERIDO, DESISTENTE."],
]


def tabelas():
    return {
        "class_assignment": referencia_simples("class_assignment", [("«PK»", "id: BIGINT")], 0, 0, "oferta"),
        "assessment_period": referencia_simples("assessment_period", [("«PK»", "id: BIGINT")], 0, 0, "oferta"),
        "enrollment": referencia_simples(
            "enrollment",
            [("«PK»", "id: BIGINT"), ("«FK»", "academic_year_id: BIGINT"),
             ("", "registration_number: VARCHAR(20)"), ("", "situation: VARCHAR(20)")],
            0, 0, "oferta",
        ),
        "student": referencia_simples("student", [("«PK»«FK»", "person_id: BIGINT")], 0, 0, "pessoas"),
        "collaborator": referencia_simples("collaborator", [("«PK»«FK»", "person_id: BIGINT")], 0, 0, "pessoas"),
        "assessment": Tabela(
            "assessment",
            [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "class_assignment_id: BIGINT"),
             ("«FK»", "assessment_period_id: BIGINT"), ("", "title: VARCHAR(80)"), ("", "kind: VARCHAR(20)"),
             ("", "weight: DECIMAL(4,2)"), ("", "max_score: DECIMAL(5,2)"), ("", "applied_on: DATE")],
            "avaliacao", 0, 0,
        ),
        "score": Tabela(
            "score",
            [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "assessment_id: BIGINT"), ("«FK»", "student_id: BIGINT"),
             ("", "value: DECIMAL(5,2)"), ("", "concept: VARCHAR(4)"), ("", "remark: VARCHAR(255)"),
             ("«UK»", "(assessment_id, student_id)")],
            "avaliacao", 0, 0,
        ),
        "period_result": Tabela(
            "period_result",
            [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "enrollment_id: BIGINT"),
             ("«FK»", "class_assignment_id: BIGINT"), ("«FK»", "assessment_period_id: BIGINT"),
             ("", "average: DECIMAL(5,2)"), ("", "recovery_score: DECIMAL(5,2)"), ("", "absences: SMALLINT"),
             ("«UK»", "(enrollment_id, class_assignment_id,"), ("", "assessment_period_id)")],
            "avaliacao", 0, 0,
        ),
        "final_result": Tabela(
            "final_result",
            [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "enrollment_id: BIGINT"), ("", "status: VARCHAR(30)"),
             ("", "council_decision: TEXT"), ("", "decided_at: TIMESTAMP"), ("«UK»", "(enrollment_id)")],
            "avaliacao", 0, 0,
        ),
        "incident": Tabela(
            "incident",
            [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "student_id: BIGINT"), ("«FK»", "author_id: BIGINT"),
             ("", "occurred_at: TIMESTAMP"), ("", "kind: VARCHAR(20)"), ("", "description: TEXT"),
             ("", "guardian_notified_at: TIMESTAMP")],
            "avaliacao", 0, 0,
        ),
    }


def empilha(caixas, x, y):
    for caixa in caixas:
        caixa.x = x
        caixa.y = y
        y += caixa.h + ESPACO


def iguala(caixas, largura):
    for caixa in caixas:
        caixa.w = largura


def montar():
    t = tabelas()
    a = t["assessment"]
    s = t["assessment_period"]
    p = t["period_result"]
    f = t["final_result"]
    sc = t["score"]
    st = t["student"]
    i = t["incident"]
    co = t["collaborator"]
    c = t["class_assignment"]
    e = t["enrollment"]

    w1 = max(c.w, e.w)
    w2 = max(a.w, s.w, p.w, f.w)
    w3 = max(sc.w, st.w, i.w, co.w)
    iguala([c, e], w1)
    iguala([a, s, p, f], w2)
    iguala([sc, st, i, co], w3)
    folga = (LARGURA - 2 * MARGEM - w1 - w2 - w3) / 2
    x1 = MARGEM
    x2 = x1 + w1 + folga
    x3 = x2 + w2 + folga
    direita1 = x1 + w1
    faixa_pc = direita1 + 22
    faixa_fe = direita1 + 44

    empilha([a, s, p, f], x2, ORIGEM_Y)
    empilha([sc, st, i, co], x3, ORIGEM_Y)
    c.x, c.y = x1, ORIGEM_Y
    e.x, e.y = x1, p.y + 80
    y_pe = e.y + 40

    ligacoes = [
        ("assessment", "class_assignment", [a.pt("w", 34), c.pt("e", 34)], "0..*", "1"),
        ("assessment", "assessment_period", [a.pt("s", 60), s.pt("n", 60)], "0..*", "1"),
        ("score", "assessment", [sc.pt("w", 44), a.pt("e", 44)], "0..*", "1"),
        ("score", "student", [sc.pt("s", 60), st.pt("n", 60)], "0..*", "1"),
        ("period_result", "enrollment", [p.pt("w", y_pe - p.y), e.pt("e", 40)], "0..*", "1"),
        ("period_result", "class_assignment",
         [p.pt("w", 40), (faixa_pc, p.y + 40), (faixa_pc, c.y + 60), c.pt("e", 60)], "0..*", "1"),
        ("period_result", "assessment_period", [p.pt("n", 60), s.pt("s", 60)], "0..*", "1"),
        ("final_result", "enrollment",
         [f.pt("w", 40), (faixa_fe, f.y + 40), (faixa_fe, e.y + 100), e.pt("e", 100)], "0..1", "1"),
        ("incident", "student", [i.pt("n", 60), st.pt("s", 60)], "0..*", "1"),
        ("incident", "collaborator", [i.pt("s", 60), co.pt("n", 60)], "0..*", "1"),
    ]

    svg = Svg(LARGURA, 0)
    titulo(svg, "Modelo de dados — Avaliação, resultados e ocorrências (Sprints 6 e 7)")
    for _, _, pontos, m_fk, m_pk in ligacoes:
        referencia(svg, pontos, m_fk, m_pk)
    for caixa in t.values():
        caixa.desenhar(svg)

    base = max(caixa.y + caixa.h for caixa in t.values()) + 50
    notas = []
    y = base
    for linhas in LINHAS_NOTAS:
        w = max(medir(linha, TAM_TEXTO) for linha in linhas) + 36
        h = nota(svg, MARGEM, y, w, linhas)
        notas.append((MARGEM, y, w, h))
        y += h + 24
    lw, lh = legenda(
        svg, MARGEM, base, ["avaliacao", "oferta", "pessoas"], OBSERVACOES_CHAVES[:2], direita=LARGURA - MARGEM,
    )
    svg.h = max(lh, y - base - 24) + base + MARGEM

    registro = {
        "caixas": t,
        "notas": notas,
        "legenda": (LARGURA - MARGEM - lw, base, lw, lh),
        "ligacoes": ligacoes,
    }
    return svg, registro


if __name__ == "__main__":
    svg, _ = montar()
    print(svg.salvar("modelo-dados-avaliacao.svg"))
