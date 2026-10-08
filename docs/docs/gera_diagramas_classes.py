from gera_classes import CORES, Classe, rotulo
from gera_modelo_dados import Svg, nota, relacao, titulo

LARGURA = 236
TEXTO_MAX = 218
X_COL = [12, 276, 540, 804]
MARGEM = 1046


def classe(nome, camada, estereotipo=None, atributos=(), operacoes=(), **extras):
    return Classe(nome, 0, 0, camada=camada, estereotipo=estereotipo, atributos=atributos, operacoes=operacoes,
                  largura_maxima=TEXTO_MAX, **extras)


def colunas_x(colunas, inicio=16, intervalo=58):
    xs, x = [], inicio
    for lista in colunas:
        largura = max(item.w for item in lista if not isinstance(item, (int, float)))
        for item in lista:
            if not isinstance(item, (int, float)):
                item.w = largura
        xs.append(x)
        x += largura + intervalo
    return xs, x - intervalo


def empilha_colunas(colunas, topo, intervalo):
    xs, direita = colunas_x(colunas)
    for indice, lista in enumerate(colunas):
        y = topo
        for item in lista:
            if isinstance(item, (int, float)):
                y += item
                continue
            item.x, item.y = xs[indice], y
            y += item.h + intervalo
    return xs, direita


def empilha(colunas, topo, intervalo):
    for indice, lista in enumerate(colunas):
        y = topo
        for item in lista:
            if isinstance(item, (int, float)):
                y += item
                continue
            item.x = X_COL[indice]
            item.y = y
            y += item.h + intervalo


def dep(svg, pontos, texto=None, pos=None, deco="seta"):
    relacao(svg, pontos, deco_fim=deco, tracejada=True)
    if texto:
        rotulo(svg, pos[0], pos[1], texto)


def legenda_camadas(svg, x, y):
    itens = [("api", "API (controladores e DTOs)"), ("aplicacao", "Aplicação (serviços)"),
             ("dominio", "Domínio (entidades)"), ("infra", "Infraestrutura e contexto"),
             ("externo", "Interface de biblioteca")]
    svg.retangulo(x, y, LARGURA, 30 + 20 * len(itens), borda="#8a8a8a", espessura=1)
    svg.texto(x + 10, y + 19, "Legenda", tam=12.5, negrito=True)
    for i, (chave, nome) in enumerate(itens):
        svg.retangulo(x + 10, y + 30 + 20 * i, 16, 12, preenchimento=CORES[chave], espessura=1)
        svg.texto(x + 34, y + 41 + 20 * i, nome, tam=12.5)


def tenant():
    filtro = classe("TenantResolverFilter", "infra", "«Component»",
                    ["- organizations: OrganizationRepository", "- jwtService: JwtService"],
                    ["# doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain): void",
                     "- resolveByHost(host: String): UUID"])
    contexto = classe("TenantContext", "infra", None, [("- CURRENT: ThreadLocal<UUID>", True)],
                      [("+ set(tenantId: UUID): void", True), ("+ get(): UUID", True), ("+ clear(): void", True)])
    resolvedor = classe("TenantIdentifierResolver", "infra", "«Component»", [],
                        ["+ resolveCurrentTenantIdentifier(): UUID", "+ validateExistingCurrentSessions(): boolean"])
    interface = classe("CurrentTenantIdentifierResolver", "externo", "«interface»", [],
                       ["+ resolveCurrentTenantIdentifier(): UUID"])
    jwt = classe("JwtService", "aplicacao", "«Service»", ["- privateKey: RSAPrivateKey"],
                 ["+ generateToken(user: User): String", "+ parse(token: String): TokenClaims", "+ publicJwks(): JwkSet"])
    convite = classe("InvitationService", "aplicacao", "«Service»", [],
                     ["+ invite(name: String, email: String, roleIds: Set<Long>): Invitation",
                      "+ accept(token: String, password: String): User", "+ resend(userId: Long): void"])
    repositorio = classe("OrganizationRepository", "infra", "«interface»", [],
                         ["+ findBySlug(slug: String): Optional<Organization>",
                          "+ findByDomainHost(host: String): Optional<Organization>"])
    permissao = classe("Permission", "dominio", "«Entity»", ["- code: String", "- module: ModuleCode", "- description: String"], [])
    usuario = classe("User", "dominio", "«Entity»", ["- id: Long", "- email: String", "- passwordHash: String", "- status: UserStatus",
                                                       "- lastLoginAt: Instant"],
                     ["+ hasPermission(code: String): boolean"])
    papel = classe("Role", "dominio", "«Entity»", ["- id: Long", "- name: String"], [])
    base = classe("TenantScopedEntity", "dominio", "«MappedSuperclass»",
                  ["# tenantId: UUID", "# createdAt: Instant", "# createdBy: Long", "# updatedAt: Instant", "# deletedAt: Instant"],
                  [], abstrata=True)
    xs, direita = empilha_colunas([[filtro, contexto, resolvedor, interface], [jwt, convite, repositorio, permissao],
                                   [usuario, papel, base]], 92, 50)
    permissao.y = max(permissao.y, papel.y)
    papel.y = permissao.y
    base.y = papel.base + 60
    todas = [filtro, contexto, resolvedor, interface, jwt, convite, repositorio, permissao, usuario, papel, base]
    margem = direita + 18
    svg = Svg(int(margem + 16), int(max(c.base for c in todas) + 30))
    titulo(svg, "Diagrama de classes — Contexto de tenant e identidade")
    dep(svg, [filtro.pt("s", 60), (filtro.x + 60, contexto.y)], "«use»", (filtro.x + 66, filtro.base + 28))
    dep(svg, [resolvedor.pt("n", 60), (resolvedor.x + 60, contexto.base)], "«use»", (resolvedor.x + 66, contexto.base + 28))
    dep(svg, [resolvedor.pt("s", 100), (resolvedor.x + 100, interface.y)], deco="triangulo")
    relacao(svg, [filtro.pt("e", 40), (jwt.x, filtro.y + 40)], deco_fim="seta")
    g1 = xs[1] - 24
    relacao(svg, [filtro.pt("e", 90), (g1, filtro.y + 90), (g1, repositorio.y + 40), repositorio.pt("w", 40)], deco_fim="seta")
    dep(svg, [jwt.pt("e", 40), (usuario.x, jwt.y + 40)], "«use»", (jwt.direita + 6, jwt.y + 32))
    g2 = xs[2] - 24
    dep(svg, [convite.pt("e", 40), (g2, convite.y + 40), (g2, usuario.y + 100), usuario.pt("w", 100)], "«create»",
        (convite.direita + 6, convite.y + 32))
    relacao(svg, [usuario.pt("s", 100), (usuario.x + 100, papel.y)], deco_fim="seta")
    rotulo(svg, usuario.x + 106, usuario.base + 16, "*")
    rotulo(svg, usuario.x + 106, papel.y - 6, "roles  1..*")
    relacao(svg, [papel.pt("w", 40), (permissao.direita, papel.y + 40)], deco_fim="seta")
    rotulo(svg, papel.x - 6, papel.y + 32, "*", ancora="end")
    rotulo(svg, permissao.direita + 6, papel.y + 32, "permissions  *")
    relacao(svg, [papel.pt("s", 100), (papel.x + 100, base.y)], deco_fim="triangulo")
    relacao(svg, [usuario.pt("e", 70), (margem, usuario.y + 70), (margem, base.y + 30), base.pt("e", 30)], deco_fim="triangulo")
    for item in todas:
        item.desenhar(svg)
    legenda_camadas(svg, xs[1], permissao.base + 30)
    return svg.salvar("classes-tenant.svg"), svg.w, svg.h


def organizacao():
    controlador = classe("OrganizationController", "api", "«RestController»",
                         ["- provisioning: OrganizationProvisioningService"],
                         ["+ create(request: CreateOrganizationRequest): ResponseEntity<OrganizationResponse>",
                          "+ suspend(id: UUID): ResponseEntity<Void>", "+ publicConfig(host: String): TenantConfigResponse"])
    provisao = classe("OrganizationProvisioningService", "aplicacao", "«Service»",
                      ["- organizations: OrganizationRepository", "- invitations: InvitationService"],
                      ["+ provision(command: ProvisionCommand): Organization", "- copyRoleTemplates(org: Organization): void",
                       "- applyProfile(org: Organization): void"])
    repositorio = classe("OrganizationRepository", "infra", "«interface»", [],
                         ["+ findBySlug(slug: String): Optional<Organization>",
                          "+ findByDomainHost(host: String): Optional<Organization>", "+ existsBySlug(slug: String): boolean"])
    org = classe("Organization", "dominio", "«Entity»",
                 ["- id: UUID", "- partnerId: UUID", "- name: String", "- slug: String", "- profile: OrganizationProfile",
                  "- status: OrganizationStatus", "- createdAt: Instant"],
                 ["+ suspend(): void", "+ reactivate(): void", "+ isActive(): boolean"])
    ajuste = classe("OrganizationSetting", "dominio", "«Entity»",
                    ["- tenantId: UUID", "- displayName: String", "- logoKey: String", "- primaryColor: String",
                     "- vocabulary: Map<String, String>", "- rules: AcademicRules", "- updatedAt: Instant"], ["+ term(key: String): String"])
    regras = classe("AcademicRules", "dominio", "«Embeddable»",
                    ["- maxActiveEnrollments: Integer", "- minAttendancePct: BigDecimal", "- attendanceWindowDays: int"], [])
    convite = classe("InvitationService", "aplicacao", "«Service»", [],
                     ["+ invite(name: String, email: String, roleIds: Set<Long>): Invitation"])
    modulo = classe("OrganizationModule", "dominio", "«Entity»", ["- module: ModuleCode", "- active: boolean"], [])
    perfil = classe("OrganizationProfile", "dominio", "«enumeration»",
                    ["SOCIAL_PROJECT", "FREE_COURSES", "EARLY_EDUCATION", "K12"], [], enumeracao=True)
    xs, direita = empilha_colunas([[controlador, provisao, repositorio], [org, ajuste, regras], [convite, 60, modulo, perfil]], 110, 56)
    todas = [controlador, provisao, repositorio, org, ajuste, regras, convite, modulo, perfil]
    svg = Svg(int(direita + 24), int(max(c.base for c in todas) + 30))
    titulo(svg, "Diagrama de classes — Organização e configuração")
    relacao(svg, [controlador.pt("s", 120), (controlador.x + 120, provisao.y)], deco_fim="seta")
    relacao(svg, [provisao.pt("s", 120), (provisao.x + 120, repositorio.y)], deco_fim="seta")
    g1, g2 = xs[1] - 36, xs[1] - 18
    relacao(svg, [provisao.pt("e", 50), (g2, provisao.y + 50), (g2, 74), (convite.x + 120, 74), (convite.x + 120, convite.y)],
            deco_fim="seta")
    dep(svg, [repositorio.pt("e", 50), (g1, repositorio.y + 50), (g1, org.y + 60), org.pt("w", 60)])
    relacao(svg, [org.pt("s", 120), (org.x + 120, ajuste.y)], deco_ini="losango")
    rotulo(svg, org.x + 126, ajuste.y - 8, "1")
    relacao(svg, [ajuste.pt("s", 120), (ajuste.x + 120, regras.y)], deco_ini="losango")
    rotulo(svg, ajuste.x + 126, regras.y - 8, "1")
    g3, g4 = xs[2] - 36, xs[2] - 18
    relacao(svg, [org.pt("e", 70), (g3, org.y + 70), (g3, modulo.y + 34), modulo.pt("w", 34)], deco_ini="losango")
    rotulo(svg, modulo.x - 6, modulo.y + 26, "0..*", ancora="end")
    relacao(svg, [org.pt("e", 120), (g4, org.y + 120), (g4, perfil.y + 34), perfil.pt("w", 34)], deco_fim="seta")
    for item in todas:
        item.desenhar(svg)
    legenda_camadas(svg, xs[2], perfil.base + 30)
    return svg.salvar("classes-organizacao.svg"), svg.w, svg.h


def matricula():
    pedido = classe("EnrollmentRequest", "api", "«record»", ["+ studentId: Long", "+ schoolRoomId: Long"], [])
    controlador = classe("EnrollmentController", "api", "«RestController»", ["- enrollmentService: EnrollmentService"],
                         ["+ enroll(request: EnrollmentRequest): ResponseEntity<EnrollmentResponse>",
                          "+ cancel(id: Long): ResponseEntity<Void>",
                          "+ transfer(id: Long, request: TransferRequest): ResponseEntity<EnrollmentResponse>"])
    resposta = classe("EnrollmentResponse", "api", "«record»",
                      ["+ id: Long", "+ studentName: String", "+ schoolRoomName: String", "+ status: EnrollmentStatus"], [])
    politica = classe("EnrollmentPolicy", "aplicacao", "«Component»", ["- settings: OrganizationSettingService"],
                      ["+ validate(student: Student, room: SchoolRoom, current: List<Enrollment>): void",
                       "- checkLimit(current: List<Enrollment>, rules: AcademicRules): void",
                       "- checkScheduleConflict(room: SchoolRoom, current: List<Enrollment>): void"])
    servico = classe("EnrollmentService", "aplicacao", "«Service»",
                     ["- enrollments: EnrollmentRepository", "- students: StudentRepository", "- schoolRooms: SchoolRoomRepository",
                      "- policy: EnrollmentPolicy", "- outbox: OutboxPublisher"],
                     ["+ enroll(studentId: Long, schoolRoomId: Long): Enrollment", "+ cancel(enrollmentId: Long): void",
                      "+ transfer(enrollmentId: Long, targetRoomId: Long): Enrollment"])
    espera = classe("WaitingListService", "aplicacao", "«Service»", [],
                    ["+ add(schoolRoomId: Long, candidate: CandidateData): WaitingListEntry",
                     "+ summonNext(schoolRoomId: Long): Optional<WaitingListEntry>", "+ expireOverdue(): int"])
    outbox = classe("OutboxPublisher", "aplicacao", "«Component»", [], ["+ publish(event: DomainEvent): void"])
    rep_aluno = classe("StudentRepository", "infra", "«interface»", [], ["+ findById(id: Long): Optional<Student>"])
    rep_matricula = classe("EnrollmentRepository", "infra", "«interface»", [],
                           ["+ countActiveBySchoolRoom(roomId: Long): long",
                            "+ findActiveByStudent(studentId: Long): List<Enrollment>",
                            "+ existsActive(studentId: Long, roomId: Long): boolean"])
    rep_turma = classe("SchoolRoomRepository", "infra", "«interface»", [], ["+ findByIdForUpdate(id: Long): Optional<SchoolRoom>"])
    evento = classe("DomainEvent", "externo", "«interface»", [], [])
    aluno = classe("Student", "dominio", "«Entity»", ["- id: Long", "- name: String", "- cpf: String", "- birthDate: LocalDate",
                                                        "- registrationNumber: String", "- healthNotes: byte[]"],
                   ["+ isMinor(today: LocalDate): boolean"])
    matricula_ = classe("Enrollment", "dominio", "«Entity»",
                        ["- id: Long", "- enrollmentDate: LocalDate", "- status: EnrollmentStatus", "- endedAt: LocalDate"],
                        ["+ cancel(date: LocalDate): void", "+ isActive(): boolean"])
    turma = classe("SchoolRoom", "dominio", "«Entity»", ["- id: Long", "- unitId: Long", "- name: String", "- classroom: String",
                                                         "- capacity: int", "- teacherId: Long"],
                   ["+ hasVacancy(activeEnrollments: long): boolean", "+ conflictsWith(other: SchoolRoom): boolean"])
    publicado = classe("StudentEnrolled", "dominio", "«record»",
                       ["+ tenantId: UUID", "+ enrollmentId: Long", "+ studentId: Long", "+ schoolRoomId: Long",
                        "+ occurredAt: Instant"], [])
    situacao = classe("EnrollmentStatus", "dominio", "«enumeration»", ["ACTIVE", "CANCELED", "TRANSFERRED"], [], enumeracao=True)
    oficina = classe("Workshop", "dominio", "«Entity»", ["- id: Long", "- name: String"], [])
    entrada = classe("WaitingListEntry", "dominio", "«Entity»",
                     ["- id: Long", "- candidateName: String", "- guardianEmail: String", "- position: int", "- status: WaitingStatus",
                      "- summonedAt: Instant", "- expiresAt: Instant"],
                     ["+ summon(deadline: Instant): void", "+ confirm(): void"])
    linhas = [([pedido, controlador, resposta], [0, 1, 2], 56), ([politica, servico, espera, outbox], [0, 1, 2, 3], 96),
              ([rep_aluno, rep_matricula, rep_turma, evento], [0, 1, 2, 3], 56),
              ([aluno, matricula_, turma, publicado], [0, 1, 2, 3], 62), ([situacao, oficina, entrada], [1, 2, 3], 30)]
    larguras = [0, 0, 0, 0]
    for itens, colunas, _ in linhas:
        for item, coluna in zip(itens, colunas):
            larguras[coluna] = max(larguras[coluna], item.w)
    xs, x = [], 16
    for largura in larguras:
        xs.append(x)
        x += largura + 52
    global X_COL, MARGEM
    X_COL, MARGEM = xs, x - 52 + 22
    y = 100
    for itens, colunas, intervalo in linhas:
        for item, coluna in zip(itens, colunas):
            item.x, item.y, item.w = xs[coluna], y, larguras[coluna]
        y += max(item.h for item in itens) + intervalo
    svg = Svg(int(MARGEM + 16), int(y + 84))
    titulo(svg, "Diagrama de classes — Matrícula (núcleo acadêmico)")
    relacao(svg, [controlador.pt("s", 118), (controlador.x + 118, servico.y)], deco_fim="seta")
    dep(svg, [controlador.pt("w", 40), (pedido.direita, controlador.y + 40)], "«use»", (pedido.direita + 4, controlador.y + 32))
    dep(svg, [controlador.pt("e", 40), (resposta.x, controlador.y + 40)], "«use»", (controlador.direita + 4, controlador.y + 32))
    relacao(svg, [servico.pt("w", 40), (politica.direita, servico.y + 40)], deco_fim="seta")
    dep(svg, [servico.pt("e", 40), (espera.x, servico.y + 40)], "«use»", (servico.direita + 4, servico.y + 32))
    faixa = max(item.base for item in (politica, servico, espera, outbox))
    y1, y2, y4 = faixa + 20, faixa + 42, faixa + 64
    relacao(svg, [servico.pt("s", 40), (servico.x + 40, y2), (rep_aluno.x + 118, y2), (rep_aluno.x + 118, rep_aluno.y)], deco_fim="seta")
    relacao(svg, [servico.pt("s", 118), (servico.x + 118, rep_matricula.y)], deco_fim="seta")
    relacao(svg, [servico.pt("s", 160), (servico.x + 160, y2), (rep_turma.x + 118, y2), (rep_turma.x + 118, rep_turma.y)],
            deco_fim="seta")
    relacao(svg, [servico.pt("s", 196), (servico.x + 196, y1), (outbox.x + 60, y1), (outbox.x + 60, outbox.base)], deco_fim="seta")
    dep(svg, [outbox.pt("s", 150), (outbox.x + 150, evento.y)])
    dep(svg, [servico.pt("s", 220), (servico.x + 220, y4), (MARGEM, y4), (MARGEM, publicado.y + 30), publicado.pt("e", 30)],
        "«create»", (MARGEM - 64, y4 - 6))
    dep(svg, [publicado.pt("n", 118), (publicado.x + 118, evento.base)], deco="triangulo")
    dep(svg, [rep_aluno.pt("s", 118), (rep_aluno.x + 118, aluno.y)])
    dep(svg, [rep_matricula.pt("s", 118), (rep_matricula.x + 118, matricula_.y)])
    dep(svg, [rep_turma.pt("s", 118), (rep_turma.x + 118, turma.y)])
    relacao(svg, [matricula_.pt("w", 60), (aluno.direita, matricula_.y + 60)], deco_fim="seta", m_ini="0..*", m_fim="1")
    relacao(svg, [matricula_.pt("e", 60), (turma.x, matricula_.y + 60)], deco_fim="seta", m_ini="0..*", m_fim="1")
    relacao(svg, [matricula_.pt("s", 118), (matricula_.x + 118, situacao.y)], deco_fim="seta")
    relacao(svg, [turma.pt("s", 60), (turma.x + 60, oficina.y)], deco_fim="seta", m_ini="0..*", m_fim="1")
    relacao(svg, [entrada.pt("n", 118), (entrada.x + 118, turma.base + 22), (turma.x + 180, turma.base + 22), (turma.x + 180, turma.base)],
            deco_fim="seta", m_ini="0..*", m_fim="1")
    for itens, _, _ in linhas:
        for item in itens:
            item.desenhar(svg)
    nota(svg, X_COL[0], situacao.y, larguras[0],
         ["Student, Enrollment, SchoolRoom e", "WaitingListEntry estendem", "TenantScopedEntity (diagrama de",
          "contexto de tenant) e herdam tenantId."])
    legenda_camadas(svg, X_COL[0], situacao.y + 96)
    return svg.salvar("classes-matricula.svg"), svg.w, svg.h


if __name__ == "__main__":
    for funcao in (tenant, organizacao, matricula):
        caminho, largura, altura = funcao()
        print(caminho, largura, altura)
