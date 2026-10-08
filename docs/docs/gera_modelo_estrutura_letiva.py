from gera_modelo_academico import referencia_simples
from gera_modelo_dados import (
    GRUPOS, OBSERVACOES_CHAVES, Svg, Tabela, legenda, medir, nota, referencia, titulo,
)

GRUPOS["estrutura"] = ("#cdeef0", "Estrutura letiva")
GRUPOS["curriculo"] = ("#fde0c8", "Currículo")

LARGURA = 1040
ALTURA = 1280
MARGEM = 28
CORREDOR = 60
FOLGA = 50
TOPO = 62
TAM_NOTA = 12.5

PK = ("«PK»", "id: BIGINT")
TENANT = ("«FK»", "tenant_id: UUID")


def tabelas():
    return {
        "academic_year": Tabela("academic_year", [
            PK, TENANT, ("", "year: SMALLINT"), ("", "starts_on: DATE"), ("", "ends_on: DATE"),
            ("", "status: VARCHAR(20)"), ("«UK»", "(tenant_id, year)"),
        ], "estrutura", 0, 0),
        "assessment_period": Tabela("assessment_period", [
            PK, TENANT, ("«FK»", "academic_year_id: BIGINT"), ("", "name: VARCHAR(40)"),
            ("", "sequence: SMALLINT"), ("", "starts_on: DATE"), ("", "ends_on: DATE"),
            ("", "closed_at: TIMESTAMP"),
        ], "estrutura", 0, 0),
        "school_calendar_day": Tabela("school_calendar_day", [
            PK, TENANT, ("«FK»", "academic_year_id: BIGINT"), ("", "day: DATE"),
            ("", "kind: VARCHAR(20)"), ("", "description: VARCHAR(120)"),
            ("«UK»", "(academic_year_id, day)"),
        ], "estrutura", 0, 0),
        "evaluation_rule": Tabela("evaluation_rule", [
            PK, TENANT, ("", "scale: VARCHAR(12)"), ("", "max_score: DECIMAL(5,2)"),
            ("", "passing_score: DECIMAL(5,2)"), ("", "formula: VARCHAR(20)"),
            ("", "recovery: VARCHAR(20)"), ("", "min_attendance_pct: DECIMAL(5,2)"),
        ], "curriculo", 0, 0),
        "stage": Tabela("stage", [
            PK, TENANT, ("«FK»", "evaluation_rule_id: BIGINT"), ("", "name: VARCHAR(60)"),
        ], "curriculo", 0, 0),
        "grade_level": Tabela("grade_level", [
            PK, TENANT, ("«FK»", "stage_id: BIGINT"), ("", "name: VARCHAR(40)"),
            ("", "sequence: SMALLINT"),
        ], "curriculo", 0, 0),
        "curriculum_matrix": Tabela("curriculum_matrix", [
            PK, TENANT, ("«FK»", "academic_year_id: BIGINT"), ("«FK»", "grade_level_id: BIGINT"),
            ("«FK»", "workshop_id: BIGINT"), ("", "annual_hours: SMALLINT"),
            ("«UK»", "(academic_year_id, grade_level_id, workshop_id)"),
        ], "curriculo", 0, 0),
        "class_assignment": Tabela("class_assignment", [
            PK, TENANT, ("«FK»", "schoolroom_id: BIGINT"), ("«FK»", "workshop_id: BIGINT"),
            ("«FK»", "teacher_id: BIGINT"), ("", "weekly_lessons: SMALLINT"),
            ("«UK»", "(schoolroom_id, workshop_id)"),
        ], "oferta", 0, 0),
        "workshop": referencia_simples("workshop", [PK, ("", "kind: VARCHAR(20)")], 0, 0, "oferta"),
        "schoolroom": referencia_simples("schoolroom", [
            PK, ("«FK»", "academic_year_id: BIGINT"), ("«FK»", "grade_level_id: BIGINT [0..1]"),
            ("", "shift: VARCHAR(12)"),
        ], 0, 0, "oferta"),
        "collaborator": referencia_simples("collaborator", [("«PK»«FK»", "person_id: BIGINT")], 0, 0, "oferta"),
    }


def centro(caixa, cx, y):
    caixa.x = cx - caixa.w / 2
    caixa.y = y


def diagrama():
    t = tabelas()
    cm, w = t["curriculum_matrix"], t["workshop"]
    ap, a, scd = t["assessment_period"], t["academic_year"], t["school_calendar_day"]
    co, ca, s = t["collaborator"], t["class_assignment"], t["schoolroom"]
    g, st, er = t["grade_level"], t["stage"], t["evaluation_rule"]

    largura_l = max(cm.w, w.w)
    largura_m = max(ap.w, a.w, scd.w)
    largura_r = max(co.w, ca.w, s.w, g.w, st.w, er.w)
    x_m = MARGEM + largura_l + CORREDOR
    x_r = x_m + largura_m + CORREDOR
    cx_l = MARGEM + largura_l / 2
    cx_m = x_m + largura_m / 2
    cx_r = x_r + largura_r / 2

    centro(co, cx_r, TOPO)
    centro(ca, cx_r, co.base + FOLGA)
    centro(w, cx_l, ca.y)
    centro(ap, cx_m, ca.y + 70)
    centro(a, cx_m, ap.base + FOLGA)
    centro(cm, cx_l, a.y)
    centro(s, cx_r, a.y)
    centro(scd, cx_m, a.base + FOLGA)
    centro(g, cx_r, s.base + FOLGA)
    centro(st, cx_r, g.base + FOLGA)
    centro(er, cx_r, st.base + FOLGA)

    svg = Svg(LARGURA, ALTURA)
    x_corredor = x_r - CORREDOR / 2
    y_corredor = scd.base + 25
    cm_g = [cm.pt("s", 40), (cm.x + 40, y_corredor), (x_corredor, y_corredor),
            (x_corredor, g.y + 40), g.pt("w", 40)]

    referencia(svg, [ap.pt("s"), a.pt("n")], "1..*", "1")
    referencia(svg, [scd.pt("n"), a.pt("s")], "0..*", "1")
    referencia(svg, [cm.pt("e"), a.pt("w")], "0..*", "1")
    referencia(svg, [cm.pt("n"), w.pt("s")], "0..*", "1")
    referencia(svg, cm_g, "0..*", "1")
    referencia(svg, [s.pt("w", 55), a.pt("e", 55)], "0..*", "1")
    referencia(svg, [s.pt("s"), g.pt("n")], "0..*", "0..1")
    referencia(svg, [g.pt("s"), st.pt("n")], "1..*", "1")
    referencia(svg, [st.pt("s"), er.pt("n")], "0..*", "1")
    referencia(svg, [ca.pt("s"), s.pt("n")], "0..*", "1")
    referencia(svg, [ca.pt("w", 40), w.pt("e", 40)], "0..*", "1")
    referencia(svg, [ca.pt("n"), co.pt("s")], "0..*", "1")

    for caixa in t.values():
        caixa.desenhar(svg)

    titulo(svg, "Modelo de dados — Estrutura letiva e currículo (Sprint 5)")
    linhas_nota = [
        "Toda tabela tem tenant_id «FK» → organization.id, com segurança em nível de linha.",
        "workshop passa a representar qualquer componente: disciplina, oficina ou modalidade.",
    ]
    nota(svg, MARGEM, TOPO, max(medir(linha, TAM_NOTA) for linha in linhas_nota) + 30, linhas_nota)
    legenda(svg, MARGEM, er.y, ["estrutura", "curriculo", "oferta"], OBSERVACOES_CHAVES[:2])
    return svg.salvar("modelo-dados-estrutura-letiva.svg")


if __name__ == "__main__":
    print(diagrama())
