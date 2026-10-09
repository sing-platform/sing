from gera_modelo_dados import (OBSERVACOES_CHAVES, Svg, Tabela, legenda, nota, referencia, titulo)

TENANT = ("«FK»", "tenant_id: UUID")

COLUNAS = {
    "person": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "user_id: BIGINT [0..1]"), ("", "name: VARCHAR(160)"),
               ("", "cpf: CHAR(11)"), ("", "birth_date: DATE"), ("", "email: VARCHAR(255)"),
               ("", "phone: VARCHAR(20)"), ("«UK»", "(tenant_id, cpf)")],
    "student": [("«PK»«FK»", "person_id: BIGINT"), ("", "registration_number: VARCHAR(20)"),
                ("", "health_notes: BYTEA (cifrado)")],
    "guardian": [("«PK»«FK»", "person_id: BIGINT")],
    "collaborator": [("«PK»«FK»", "person_id: BIGINT"), ("", "type: VARCHAR(30)")],
    "student_guardian": [("«PK»«FK»", "student_id: BIGINT"), ("«PK»«FK»", "guardian_id: BIGINT"),
                         ("", "family_tie: VARCHAR(30)"), ("", "is_primary: BOOLEAN")],
    "workshop": [("«PK»", "id: BIGINT"), TENANT, ("", "name: VARCHAR(120)"), ("«UK»", "(tenant_id, name)")],
    "schoolroom": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "unit_id: BIGINT"), ("«FK»", "workshop_id: BIGINT"),
                   ("«FK»", "teacher_id: BIGINT"), ("", "name: VARCHAR(120)"), ("", "classroom: VARCHAR(60)"),
                   ("", "capacity: INTEGER"), ("«UK»", "(workshop_id, name)")],
    "schoolroom_schedule_slot": [("«PK»", "id: BIGINT"), ("«FK»", "schoolroom_id: BIGINT"),
                                 ("", "day_of_week: SMALLINT"), ("", "start_time: TIME"), ("", "end_time: TIME")],
    "enrollment": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "student_id: BIGINT"), ("«FK»", "schoolroom_id: BIGINT"),
                   ("", "enrollment_date: DATE"), ("", "status: VARCHAR(20)"), ("", "ended_at: DATE")],
    "waiting_list_entry": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "schoolroom_id: BIGINT"),
                           ("", "candidate_name: VARCHAR(160)"), ("", "guardian_email: VARCHAR(255)"),
                           ("", "position: INTEGER"), ("", "status: VARCHAR(20)"), ("", "summoned_at: TIMESTAMP"),
                           ("", "expires_at: TIMESTAMP")],
    "lecture": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "schoolroom_id: BIGINT"), ("", "lecture_date: DATE"),
                ("", "topic: VARCHAR(255)"), ("", "closed: BOOLEAN"), ("«UK»", "(schoolroom_id, lecture_date)")],
    "attendance": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "lecture_id: BIGINT"), ("«FK»", "student_id: BIGINT"),
                   ("", "status: VARCHAR(12)"), ("", "justified_by_document_id: BIGINT"),
                   ("«UK»", "(lecture_id, student_id)")],
    "descriptive_report": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "student_id: BIGINT"),
                           ("«FK»", "schoolroom_id: BIGINT"), ("", "content: TEXT"), ("", "status: VARCHAR(12)")],
    "payment_plan": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "enrollment_id: BIGINT"), ("«FK»", "payer_id: BIGINT"),
                     ("", "total_amount: DECIMAL(12,2)"), ("", "installments: SMALLINT"),
                     ("", "discount_pct: DECIMAL(5,2)")],
    "charge": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "payment_plan_id: BIGINT"), ("", "due_date: DATE"),
               ("", "amount: DECIMAL(12,2)"), ("", "status: VARCHAR(12)"), ("", "paid_at: TIMESTAMP"),
               ("", "provider_ref: VARCHAR(80)")],
    "consent": [("«PK»", "id: BIGINT"), TENANT, ("«FK»", "guardian_id: BIGINT"), ("«FK»", "student_id: BIGINT"),
                ("", "term_version: VARCHAR(10)"), ("", "accepted_at: TIMESTAMP"), ("", "revoked_at: TIMESTAMP")],
}

GRUPO = {
    "person": "pessoas", "student": "pessoas", "guardian": "pessoas", "collaborator": "pessoas",
    "student_guardian": "pessoas", "workshop": "oferta", "schoolroom": "oferta",
    "schoolroom_schedule_slot": "oferta", "enrollment": "oferta", "waiting_list_entry": "oferta",
    "lecture": "frequencia", "attendance": "frequencia", "descriptive_report": "frequencia",
    "payment_plan": "financeiro", "charge": "financeiro", "consent": "consentimento",
}


def posiciona(grade, xs, ys):
    tabelas = {}
    for (coluna, linha), nome in grade.items():
        tabela = Tabela(nome, COLUNAS[nome], GRUPO[nome], 0, ys[linha])
        tabela.x = xs[coluna]
        tabelas[nome] = tabela
    return tabelas


def nota_tenant(svg, x, y, w, extra=()):
    linhas = ["Toda tabela com tenant_id: UUID «FK» → organization.id,",
              "protegida por política de segurança em nível de linha (RLS).",
              "Tabelas de negócio também têm created_at, created_by,",
              "updated_at e deleted_at (auditoria e exclusão lógica)."]
    return nota(svg, x, y, w, linhas + list(extra))


def academico():
    svg = Svg(1040, 1345)
    titulo(svg, "Modelo de dados — Pessoas, oferta, matrícula e frequência")
    xs = {"A": 20, "B": 352, "C": 712}
    ys = {1: 66, 2: 340, 3: 584, 4: 846, 5: 1092}
    t = posiciona({
        ("A", 1): "guardian", ("B", 1): "person", ("C", 1): "collaborator",
        ("A", 2): "student_guardian", ("B", 2): "student", ("C", 2): "workshop",
        ("A", 3): "descriptive_report", ("B", 3): "enrollment", ("C", 3): "schoolroom",
        ("A", 4): "attendance", ("B", 4): "lecture", ("C", 4): "schoolroom_schedule_slot",
        ("C", 5): "waiting_list_entry",
    }, xs, ys)
    for nome in ("guardian", "student_guardian", "descriptive_report", "attendance"):
        t[nome].x = xs["A"]
    ab = [t[n].direita for n in ("guardian", "student_guardian", "descriptive_report", "attendance")]
    a_dir = max(ab)
    b_esq = xs["B"]
    b_dir = max(t[n].direita for n in ("person", "student", "enrollment", "lecture"))
    c_esq = xs["C"]
    c_dir = max(t[n].direita for n in ("collaborator", "workshop", "schoolroom", "schoolroom_schedule_slot", "waiting_list_entry"))
    lane_ab = [a_dir + 14, a_dir + 26, a_dir + 38]
    lane_bc = [b_dir + 16, b_dir + 32]
    lane_dir = c_dir + 44

    person, student, enrollment, lecture = t["person"], t["student"], t["enrollment"], t["lecture"]
    schoolroom = t["schoolroom"]
    referencia(svg, [t["guardian"].pt("e", 34), (b_esq, t["guardian"].y + 34)], "0..1", "1")
    referencia(svg, [t["collaborator"].pt("w", 34), (person.direita, t["collaborator"].y + 34)], "0..1", "1")
    referencia(svg, [student.pt("n"), (student.pt("n")[0], person.base)], "0..1", "1")
    referencia(svg, [t["student_guardian"].pt("e", 30), (b_esq, student.y + 30)], "0..*", "1")
    referencia(svg, [t["student_guardian"].pt("n", 60), (t["student_guardian"].x + 60, t["guardian"].base)], "0..*", "1")
    referencia(svg, [enrollment.pt("n"), (enrollment.pt("n")[0], student.base)], "0..*", "1")
    referencia(svg, [enrollment.pt("e", 40), (c_esq, enrollment.y + 40)], "0..*", "1")

    desc = t["descriptive_report"]
    referencia(svg, [desc.pt("e", 40), (lane_ab[1], desc.y + 40), (lane_ab[1], student.y + 62), (b_esq, student.y + 62)], "0..*", "1")
    corredor = enrollment.base + (lecture.y - enrollment.base) / 2
    referencia(svg, [desc.pt("e", 110), (lane_ab[0], desc.y + 110), (lane_ab[0], corredor), (lane_bc[0], corredor),
                     (lane_bc[0], schoolroom.y + 150), (c_esq, schoolroom.y + 150)], "0..*", "1")
    att = t["attendance"]
    referencia(svg, [att.pt("e", 40), (b_esq, lecture.y + 40)], "0..*", "1")
    referencia(svg, [att.pt("e", 100), (lane_ab[2], att.y + 100), (lane_ab[2], student.y + 92), (b_esq, student.y + 92)], "0..*", "1")
    referencia(svg, [lecture.pt("e", 50), (lane_bc[1], lecture.y + 50), (lane_bc[1], schoolroom.y + 190), (c_esq, schoolroom.y + 190)], "0..*", "1")

    workshop = t["workshop"]
    referencia(svg, [schoolroom.pt("n", 60), (schoolroom.x + 60, workshop.base)], "0..*", "1")
    referencia(svg, [schoolroom.pt("e", 40), (lane_dir, schoolroom.y + 40), (lane_dir, t["collaborator"].y + 60),
                     (t["collaborator"].direita, t["collaborator"].y + 60)], "0..*", "1")
    slot = t["schoolroom_schedule_slot"]
    referencia(svg, [slot.pt("n", 60), (slot.x + 60, schoolroom.base)], "1..*", "1")
    espera = t["waiting_list_entry"]
    referencia(svg, [espera.pt("e", 40), (lane_dir, espera.y + 40), (lane_dir, schoolroom.y + 120),
                     (schoolroom.direita, schoolroom.y + 120)], "0..*", "1")

    for tabela in t.values():
        tabela.desenhar(svg)
    nota_tenant(svg, 20, 1104, 600, ["person.user_id → users.id e schoolroom.unit_id → unit.id: diagrama de plataforma.",
                                       "attendance.justified_by_document_id: id lógico no banco do serviço documents."])
    legenda(svg, 20, 1196, ["pessoas", "oferta", "frequencia"], OBSERVACOES_CHAVES[:2])
    return svg.salvar("modelo-dados-academico.svg")


def referencia_simples(nome, colunas, x, y, grupo):
    tabela = Tabela(nome, colunas, grupo, x, y)
    return tabela


def financeiro():
    svg = Svg(1010, 810)
    titulo(svg, "Modelo de dados — Financeiro e consentimento")
    guardian = referencia_simples("guardian", [("«PK»«FK»", "person_id: BIGINT")], 40, 90, "pessoas")
    enrollment = referencia_simples("enrollment", [("«PK»", "id: BIGINT"), ("«FK»", "student_id: BIGINT"), ("", "…")], 740, 90, "oferta")
    plano = Tabela("payment_plan", COLUNAS["payment_plan"], "financeiro", 380, 90)
    student = referencia_simples("student", [("«PK»«FK»", "person_id: BIGINT"), ("", "…")], 40, 430, "pessoas")
    consent = Tabela("consent", COLUNAS["consent"], "consentimento", 340, 430)
    cobranca = Tabela("charge", COLUNAS["charge"], "financeiro", 680, 430)
    referencia(svg, [plano.pt("e", 40), (enrollment.x, plano.y + 40)], "0..1", "1")
    referencia(svg, [plano.pt("w", 40), (guardian.direita, plano.y + 40)], "0..*", "1")
    x_plano = plano.x + plano.w - 50
    referencia(svg, [cobranca.pt("n", 50), (cobranca.x + 50, 370), (x_plano, 370), (x_plano, plano.base)], "1..*", "1")
    referencia(svg, [consent.pt("w", 60), (student.direita, consent.y + 60)], "0..*", "1")
    x_guarda = guardian.x + guardian.w - 40
    referencia(svg, [consent.pt("n", 50), (consent.x + 50, 330), (x_guarda, 330), (x_guarda, guardian.base)], "0..*", "1")
    for tabela in (guardian, student, enrollment, plano, cobranca, consent):
        tabela.desenhar(svg)
    nota_tenant(svg, 40, 660, 560, ["guardian, student e enrollment: detalhados no diagrama anterior."])
    legenda(svg, 0, 640, ["pessoas", "oferta", "financeiro", "consentimento"], OBSERVACOES_CHAVES[:2], direita=990)
    return svg.salvar("modelo-dados-financeiro.svg")


if __name__ == "__main__":
    print(academico())
    print(financeiro())
