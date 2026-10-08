import os
import sys
from xml.sax.saxutils import escape

from caminhos import DIAGRAMAS

AQUI = os.path.dirname(os.path.abspath(__file__))
SAIDA = DIAGRAMAS
LARGURA = 1060
FONTE = 14
CHAR = 6.7

SEQ = {
    "UC001": {
        "titulo": "Realizar login de usuário",
        "p": [("U", "Usuário", True), ("SPA", "SPA (React)", False), ("TF", "TenantResolverFilter", False),
              ("AC", "AuthController", False), ("AS", "AuthService", False), ("UR", "UserRepository", False),
              ("JWT", "JwtService", False)],
        "m": [
            ("U", "SPA", "acessa o endereço da escola", "s"),
            ("SPA", "TF", "GET /api/public/tenant (Host)", "s"),
            ("TF", "TF", "resolveByHost(host)", "s"),
            ("TF", "SPA", "marca, vocabulário e módulos", "r"),
            ("SPA", "U", "tela de login com a marca da escola", "r"),
            ("U", "SPA", "informa e-mail e senha; “Entrar”", "s"),
            ("SPA", "AC", "POST /api/v1/auth/login", "s"),
            ("AC", "AS", "authenticate(email, senha)", "s"),
            ("AS", "UR", "findByEmail(email)", "s"),
            ("UR", "AS", "Optional<User> da organização", "r"),
            ("alt", "credenciais válidas e conta desbloqueada"),
            ("AS", "JWT", "generateToken(user)", "s"),
            ("JWT", "AS", "token (tid, uid, roles)", "r"),
            ("AS", "AC", "token", "r"),
            ("AC", "SPA", "200 {token}", "r"),
            ("SPA", "U", "página inicial do papel", "r"),
            ("else", "credenciais inválidas"),
            ("AS", "AC", "BadCredentialsException (tentativa somada)", "r"),
            ("AC", "SPA", "401 “E-mail ou senha inválidos”", "r"),
            ("SPA", "U", "mensagem de erro", "r"),
            ("else", "conta bloqueada após 5 tentativas"),
            ("AS", "AC", "LockedException", "r"),
            ("AC", "SPA", "423 com o tempo restante de bloqueio", "r"),
            ("SPA", "U", "aviso de conta bloqueada", "r"),
            ("end",),
        ],
    },
    "UC002": {
        "titulo": "Provisionar organização",
        "p": [("U", "Operador da plataforma", True), ("SPA", "Console (React)", False),
              ("OC", "OrganizationController", False), ("PS", "OrganizationProvisioningService", False),
              ("OR", "OrganizationRepository", False), ("IS", "InvitationService", False),
              ("MAIL", "Serviço de e-mail", False)],
        "m": [
            ("U", "SPA", "preenche “Nova organização” e confirma", "s"),
            ("SPA", "OC", "POST /api/platform/organizations", "s"),
            ("OC", "PS", "provision(command)", "s"),
            ("PS", "OR", "existsBySlug(slug)", "s"),
            ("OR", "PS", "boolean", "r"),
            ("alt", "identificador livre"),
            ("PS", "OR", "save(organização, unidade, configurações do perfil, papéis)", "s"),
            ("OR", "PS", "Organization", "r"),
            ("PS", "IS", "invite(nome, email, [Administrador])", "s"),
            ("IS", "MAIL", "e-mail de convite com link (72 h)", "a"),
            ("IS", "PS", "Invitation", "r"),
            ("PS", "OC", "Organization", "r"),
            ("OC", "SPA", "201 Created", "r"),
            ("SPA", "U", "organização “Ativa” na lista", "r"),
            ("else", "identificador em uso"),
            ("PS", "OC", "SlugAlreadyInUseException", "r"),
            ("OC", "SPA", "409 “Identificador já está em uso”", "r"),
            ("SPA", "U", "erro no campo identificador", "r"),
            ("end",),
        ],
    },
    "UC003": {
        "titulo": "Restringir o acesso aos dados da própria organização",
        "p": [("U", "Usuário autenticado", True), ("SPA", "SPA (React)", False),
              ("TF", "TenantResolverFilter", False), ("TC", "TenantContext", False),
              ("SC", "StudentController", False), ("SR", "StudentRepository", False),
              ("DB", "PostgreSQL (RLS)", False)],
        "m": [
            ("U", "SPA", "abre a lista de alunos", "s"),
            ("SPA", "TF", "GET /api/v1/students (Host + JWT)", "s"),
            ("TF", "TF", "resolveByHost(host) e confere com o tid do token", "s"),
            ("alt", "tid do token = organização do endereço"),
            ("TF", "TC", "set(tenantId)", "s"),
            ("TF", "SC", "encaminha a requisição", "s"),
            ("SC", "SR", "findAll(pageable)", "s"),
            ("SR", "DB", "SELECT … com app.tenant_id = tid", "s"),
            ("DB", "SR", "somente linhas da organização", "r"),
            ("SR", "SC", "Page<Student>", "r"),
            ("SC", "TF", "200 lista de alunos", "r"),
            ("TF", "TC", "clear()", "s"),
            ("TF", "SPA", "200 lista de alunos", "r"),
            ("SPA", "U", "alunos da própria organização", "r"),
            ("else", "token emitido para outra organização"),
            ("TF", "SPA", "401 e registro na auditoria", "r"),
            ("SPA", "U", "tela de login da organização do endereço", "r"),
            ("end",),
        ],
    },
    "UC004": {
        "titulo": "Configurar a identidade visual da organização",
        "p": [("U", "Administrador da escola", True), ("SPA", "SPA (React)", False),
              ("C", "OrganizationSettingController", False), ("S", "OrganizationSettingService", False),
              ("DOC", "documents (serviço de arquivos)", False), ("R", "OrganizationSettingRepository", False)],
        "m": [
            ("U", "SPA", "envia o logo, escolhe a cor e aciona “Salvar”", "s"),
            ("SPA", "C", "PUT /api/v1/organization/branding", "s"),
            ("C", "S", "updateBranding(displayName, logo, primaryColor)", "s"),
            ("S", "S", "valida tipo e tamanho do logo e contraste ≥ 4,5:1", "s"),
            ("alt", "arquivo e contraste válidos"),
            ("S", "DOC", "POST /api/documents/assets (logo)", "s"),
            ("DOC", "S", "logoKey", "r"),
            ("S", "R", "save(setting)", "s"),
            ("R", "S", "OrganizationSetting", "r"),
            ("S", "C", "BrandingResponse", "r"),
            ("C", "SPA", "200", "r"),
            ("SPA", "U", "“Identidade visual atualizada” e nova pré-visualização", "r"),
            ("else", "arquivo inválido"),
            ("S", "C", "InvalidLogoException", "r"),
            ("C", "SPA", "422 com o motivo; logo anterior mantido", "r"),
            ("else", "contraste insuficiente"),
            ("S", "C", "LowContrastException", "r"),
            ("C", "SPA", "422 “A combinação prejudica a leitura”", "r"),
            ("end",),
        ],
    },
    "UC005": {
        "titulo": "Gerenciar usuários por convite",
        "p": [("U", "Administrador da escola", True), ("SPA", "SPA (React)", False),
              ("UC", "UserController", False), ("IS", "InvitationService", False),
              ("UR", "UserRepository", False), ("MAIL", "Serviço de e-mail", False),
              ("CV", "Usuário convidado", True)],
        "m": [
            ("U", "SPA", "informa nome, e-mail e papel; “Enviar convite”", "s"),
            ("SPA", "UC", "POST /api/v1/users/invitations", "s"),
            ("UC", "IS", "invite(nome, email, roleIds)", "s"),
            ("IS", "UR", "existsByEmail(email)", "s"),
            ("UR", "IS", "false", "r"),
            ("IS", "UR", "save(user PENDING, invitation 72 h)", "s"),
            ("IS", "MAIL", "e-mail com link de primeiro acesso", "a"),
            ("IS", "UC", "Invitation", "r"),
            ("UC", "SPA", "201 Created", "r"),
            ("SPA", "U", "usuário como “Convite pendente”", "r"),
            ("MAIL", "CV", "entrega o e-mail", "a"),
            ("CV", "SPA", "abre o link e define a senha", "s"),
            ("SPA", "UC", "POST /api/v1/users/invitations/{token}/accept", "s"),
            ("UC", "IS", "accept(token, senha)", "s"),
            ("alt", "convite válido e senha dentro da regra"),
            ("IS", "UR", "save(user ACTIVE, invitation usada)", "s"),
            ("IS", "UC", "User", "r"),
            ("UC", "SPA", "200", "r"),
            ("SPA", "CV", "tela de login da escola", "r"),
            ("else", "convite expirado ou já utilizado"),
            ("IS", "UC", "InvitationExpiredException", "r"),
            ("UC", "SPA", "410 “Convite expirado”", "r"),
            ("end",),
        ],
    },
    "UC006": {
        "titulo": "Manter alunos e responsáveis",
        "p": [("U", "Secretaria", True), ("SPA", "SPA (React)", False), ("SC", "StudentController", False),
              ("SS", "StudentService", False), ("SR", "StudentRepository", False),
              ("AL", "AuditLogService", False)],
        "m": [
            ("U", "SPA", "preenche o cadastro e aciona “Salvar”", "s"),
            ("SPA", "SC", "POST /api/v1/students", "s"),
            ("SC", "SS", "create(request)", "s"),
            ("SS", "SR", "existsByCpf(cpf)", "s"),
            ("SR", "SS", "boolean", "r"),
            ("alt", "CPF livre e, se menor, responsável vinculado"),
            ("SS", "SR", "save(student, guardians)", "s"),
            ("SR", "SS", "Student", "r"),
            ("SS", "AL", "record(CREATE, student)", "s"),
            ("SS", "SC", "StudentResponse", "r"),
            ("SC", "SPA", "201 Created", "r"),
            ("SPA", "U", "“Aluno cadastrado”", "r"),
            ("else", "CPF já cadastrado na organização"),
            ("SS", "SC", "DuplicateCpfException", "r"),
            ("SC", "SPA", "409 “CPF já cadastrado nesta escola”", "r"),
            ("SPA", "U", "erro no campo CPF", "r"),
            ("else", "aluno menor sem responsável"),
            ("SS", "SC", "ValidationException", "r"),
            ("SC", "SPA", "422 pede a inclusão de um responsável", "r"),
            ("end",),
        ],
    },
    "UC007": {
        "titulo": "Manter turmas e oficinas",
        "p": [("U", "Secretaria", True), ("SPA", "SPA (React)", False), ("C", "SchoolRoomController", False),
              ("S", "SchoolRoomService", False), ("WR", "WorkshopRepository", False),
              ("RR", "SchoolRoomRepository", False)],
        "m": [
            ("U", "SPA", "informa oficina, nome, capacidade, professor e horários; “Salvar”", "s"),
            ("SPA", "C", "POST /api/v1/schoolrooms", "s"),
            ("C", "S", "create(request)", "s"),
            ("S", "WR", "findById(workshopId)", "s"),
            ("WR", "S", "Workshop", "r"),
            ("S", "RR", "findByTeacherAndSlots(teacherId, horários)", "s"),
            ("RR", "S", "List<SchoolRoom>", "r"),
            ("alt", "professor sem turma no mesmo horário"),
            ("S", "RR", "save(schoolRoom, slots)", "s"),
            ("RR", "S", "SchoolRoom", "r"),
            ("S", "C", "SchoolRoomResponse", "r"),
            ("C", "SPA", "201 Created", "r"),
            ("SPA", "U", "turma na lista com 0 de 15 vagas ocupadas", "r"),
            ("else", "conflito de horário do professor"),
            ("S", "C", "ScheduleConflictException", "r"),
            ("C", "SPA", "409 com a turma e o horário em conflito", "r"),
            ("SPA", "U", "aviso de conflito", "r"),
            ("end",),
        ],
    },
    "UC008": {
        "titulo": "Matricular aluno em turma",
        "p": [("U", "Secretaria", True), ("SPA", "SPA (React)", False), ("EC", "EnrollmentController", False),
              ("ES", "EnrollmentService", False), ("EP", "EnrollmentPolicy", False),
              ("ER", "EnrollmentRepository", False), ("OP", "OutboxPublisher", False)],
        "m": [
            ("U", "SPA", "seleciona aluno e turma; “Confirmar matrícula”", "s"),
            ("SPA", "EC", "POST /api/v1/enrollments", "s"),
            ("EC", "ES", "enroll(studentId, schoolRoomId)", "s"),
            ("ES", "ER", "bloqueia a turma e countActiveBySchoolRoom(roomId)", "s"),
            ("ER", "ES", "ocupação atual", "r"),
            ("ES", "EP", "validate(student, room, matrículasAtuais)", "s"),
            ("EP", "ES", "regras da organização verificadas", "r"),
            ("alt", "há vaga e regras atendidas"),
            ("ES", "ER", "save(enrollment ACTIVE)", "s"),
            ("ES", "OP", "publish(StudentEnrolled)", "s"),
            ("ES", "EC", "Enrollment", "r"),
            ("EC", "SPA", "201 Created", "r"),
            ("SPA", "U", "“Matrícula efetuada”", "r"),
            ("else", "turma sem vaga"),
            ("ES", "EC", "NoVacancyException", "r"),
            ("EC", "SPA", "409 oferece a lista de espera", "r"),
            ("else", "limite de matrículas atingido"),
            ("ES", "EC", "EnrollmentLimitException", "r"),
            ("EC", "SPA", "422 informa o limite da escola", "r"),
            ("else", "conflito de horário"),
            ("ES", "EC", "ScheduleConflictException", "r"),
            ("EC", "SPA", "409 informa a turma em conflito", "r"),
            ("end",),
        ],
    },
    "UC009": {
        "titulo": "Configurar o vocabulário da organização",
        "p": [("U", "Administrador da escola", True), ("SPA", "SPA (React)", False),
              ("C", "OrganizationSettingController", False), ("S", "OrganizationSettingService", False),
              ("R", "OrganizationSettingRepository", False)],
        "m": [
            ("U", "SPA", "troca “Oficina” por “Modalidade”; “Salvar”", "s"),
            ("SPA", "C", "PUT /api/v1/organization/vocabulary", "s"),
            ("C", "S", "updateVocabulary(termos)", "s"),
            ("S", "S", "valida catálogo de termos e tamanho (2 a 30)", "s"),
            ("alt", "termos válidos"),
            ("S", "R", "save(setting)", "s"),
            ("R", "S", "OrganizationSetting", "r"),
            ("S", "C", "VocabularyResponse", "r"),
            ("C", "SPA", "200", "r"),
            ("SPA", "C", "GET /api/v1/organization/vocabulary", "s"),
            ("C", "SPA", "vocabulário atualizado", "r"),
            ("SPA", "U", "interface passa a exibir “Modalidade”", "r"),
            ("else", "termo fora do tamanho permitido"),
            ("S", "C", "ValidationException", "r"),
            ("C", "SPA", "422", "r"),
            ("SPA", "U", "termo destacado", "r"),
            ("end",),
        ],
    },
    "UC010": {
        "titulo": "Ativar e desativar módulos da organização",
        "p": [("U", "Administrador da escola", True), ("SPA", "SPA (React)", False),
              ("C", "OrganizationModuleController", False), ("S", "OrganizationModuleService", False),
              ("R", "OrganizationModuleRepository", False), ("MG", "ModuleGuardInterceptor", False)],
        "m": [
            ("U", "SPA", "desativa “Financeiro” e confirma", "s"),
            ("SPA", "C", "PATCH /api/v1/organization/modules/FINANCE", "s"),
            ("C", "S", "setActive(FINANCE, false)", "s"),
            ("S", "R", "findByModule(FINANCE)", "s"),
            ("R", "S", "OrganizationModule (plano, essencial)", "r"),
            ("alt", "módulo do plano e não essencial"),
            ("S", "R", "save(module inativo)", "s"),
            ("S", "C", "ModuleResponse", "r"),
            ("C", "SPA", "200", "r"),
            ("SPA", "U", "item Financeiro removido do menu", "r"),
            ("else", "módulo essencial ou fora do plano"),
            ("S", "C", "ModuleNotAllowedException", "r"),
            ("C", "SPA", "422 com a explicação", "r"),
            ("end",),
            ("U", "SPA", "acessa o endereço direto de uma tela do Financeiro", "s"),
            ("SPA", "MG", "GET /api/v1/finance/charges", "s"),
            ("MG", "R", "isActive(FINANCE)", "s"),
            ("R", "MG", "false", "r"),
            ("MG", "SPA", "404", "r"),
            ("SPA", "U", "“Página não encontrada”", "r"),
        ],
    },
    "UC011": {
        "titulo": "Configurar regras acadêmicas da organização",
        "p": [("U", "Administrador da escola", True), ("SPA", "SPA (React)", False),
              ("C", "OrganizationSettingController", False), ("S", "OrganizationSettingService", False),
              ("R", "OrganizationSettingRepository", False), ("AL", "AuditLogService", False)],
        "m": [
            ("U", "SPA", "altera o limite de matrículas para 4; “Salvar”", "s"),
            ("SPA", "C", "PUT /api/v1/organization/rules", "s"),
            ("C", "S", "updateRules(rules)", "s"),
            ("S", "S", "valida intervalos (1 a 20, 0 a 100 %, 1 a 90 dias)", "s"),
            ("alt", "valores dentro dos intervalos"),
            ("S", "R", "findByTenant()", "s"),
            ("R", "S", "OrganizationSetting (valores anteriores)", "r"),
            ("S", "R", "save(setting)", "s"),
            ("S", "AL", "record(UPDATE, anterior, novo)", "s"),
            ("S", "C", "RulesResponse", "r"),
            ("C", "SPA", "200", "r"),
            ("SPA", "U", "“Regras atualizadas”", "r"),
            ("else", "valor fora do intervalo"),
            ("S", "C", "ValidationException", "r"),
            ("C", "SPA", "422 com o intervalo aceito", "r"),
            ("SPA", "U", "campo destacado", "r"),
            ("end",),
        ],
    },
    "UC012": {
        "titulo": "Gerenciar papéis e permissões",
        "p": [("U", "Administrador da escola", True), ("SPA", "SPA (React)", False),
              ("RC", "RoleController", False), ("RS", "RoleService", False),
              ("RR", "RoleRepository", False), ("PR", "PermissionRepository", False)],
        "m": [
            ("U", "SPA", "informa “Coordenação”, marca as permissões; “Salvar”", "s"),
            ("SPA", "RC", "POST /api/v1/roles", "s"),
            ("RC", "RS", "create(name, permissionCodes)", "s"),
            ("RS", "RR", "existsByName(name)", "s"),
            ("RR", "RS", "boolean", "r"),
            ("RS", "PR", "findAllByCode(codes)", "s"),
            ("PR", "RS", "Set<Permission>", "r"),
            ("alt", "nome livre e permissões do catálogo"),
            ("RS", "RR", "save(role)", "s"),
            ("RR", "RS", "Role", "r"),
            ("RS", "RC", "RoleResponse", "r"),
            ("RC", "SPA", "201 Created", "r"),
            ("SPA", "U", "papel exibido na lista", "r"),
            ("else", "nome de papel duplicado"),
            ("RS", "RC", "DuplicateRoleException", "r"),
            ("RC", "SPA", "409 “Já existe um papel com esse nome”", "r"),
            ("else", "organização ficaria sem administrador"),
            ("RS", "RC", "LastAdministratorException", "r"),
            ("RC", "SPA", "422 com o motivo", "r"),
            ("end",),
        ],
    },
    "UC013": {
        "titulo": "Gerenciar lista de espera e convocação",
        "p": [("U", "Secretaria", True), ("SPA", "SPA (React)", False), ("ES", "EnrollmentService", False),
              ("WS", "WaitingListService", False), ("WR", "WaitingListRepository", False),
              ("MAIL", "Serviço de e-mail", False), ("RP", "Responsável", True)],
        "m": [
            ("U", "SPA", "cancela uma matrícula da turma lotada", "s"),
            ("SPA", "ES", "cancel(enrollmentId)", "s"),
            ("ES", "WS", "summonNext(schoolRoomId)", "s"),
            ("WS", "WR", "findFirstWaiting(schoolRoomId)", "s"),
            ("WR", "WS", "Optional<WaitingListEntry>", "r"),
            ("opt", "há interessado aguardando"),
            ("WS", "WR", "save(entry CONVOCADO, prazo de 3 dias, vaga reservada)", "s"),
            ("WS", "MAIL", "e-mail de convocação com link", "a"),
            ("MAIL", "RP", "entrega a convocação", "a"),
            ("end",),
            ("WS", "ES", "interessado convocado", "r"),
            ("ES", "SPA", "204 No Content", "r"),
            ("SPA", "U", "fila atualizada: primeiro como “Convocado”", "r"),
            ("alt", "responsável confirma dentro do prazo"),
            ("RP", "SPA", "abre o link e confirma o interesse", "s"),
            ("SPA", "WS", "confirm(token)", "s"),
            ("WS", "WR", "save(entry CONFIRMADO)", "s"),
            ("WS", "SPA", "200", "r"),
            ("SPA", "RP", "confirmação registrada", "r"),
            ("else", "prazo de 3 dias termina sem resposta"),
            ("WS", "WS", "expireOverdue() (rotina agendada)", "s"),
            ("WS", "WR", "save(entry NAO_RESPONDEU)", "s"),
            ("WS", "WS", "summonNext(schoolRoomId)", "s"),
            ("end",),
        ],
    },
    "UC014": {
        "titulo": "Registrar chamada",
        "p": [("U", "Professor", True), ("SPA", "SPA (React)", False), ("AC", "AttendanceController", False),
              ("AS", "AttendanceService", False), ("AZ", "AcademicAuthorizationService", False),
              ("AR", "AttendanceRepository", False), ("OP", "OutboxPublisher", False)],
        "m": [
            ("U", "SPA", "marca as faltas e aciona “Salvar chamada”", "s"),
            ("SPA", "AC", "PUT /api/v1/schoolrooms/{id}/lectures/{data}/attendance", "s"),
            ("AC", "AS", "register(schoolRoomId, data, presenças)", "s"),
            ("AS", "AZ", "assertTeacherOf(schoolRoomId)", "s"),
            ("AZ", "AS", "resultado da verificação", "r"),
            ("AS", "AS", "confere a janela de lançamento da organização", "s"),
            ("alt", "professor da turma e data dentro da janela"),
            ("AS", "AR", "saveAll(lecture, attendances)", "s"),
            ("AR", "AS", "List<Attendance>", "r"),
            ("AS", "OP", "publish(AttendanceRegistered)", "s"),
            ("AS", "AC", "AttendanceSummary", "r"),
            ("AC", "SPA", "200", "r"),
            ("SPA", "U", "“Chamada registrada”", "r"),
            ("else", "data fora da janela de lançamento"),
            ("AS", "AC", "BusinessRuleException", "r"),
            ("AC", "SPA", "422 informa a janela configurada", "r"),
            ("else", "professor não responde pela turma"),
            ("AS", "AC", "AccessDeniedException", "r"),
            ("AC", "SPA", "403", "r"),
            ("end",),
        ],
    },
    "UC015": {
        "titulo": "Consultar frequência e grade horária",
        "p": [("U", "Responsável", True), ("SPA", "SPA (React, celular)", False),
              ("C", "StudentPortalController", False), ("G", "GuardianshipService", False),
              ("S", "AttendanceSummaryService", False), ("AR", "AttendanceRepository", False)],
        "m": [
            ("U", "SPA", "escolhe o aluno e o período", "s"),
            ("SPA", "C", "GET /api/v1/portal/students/{id}/attendance?from&to", "s"),
            ("C", "G", "assertGuardianOf(studentId)", "s"),
            ("G", "C", "resultado da verificação", "r"),
            ("alt", "responsável vinculado ao aluno"),
            ("C", "S", "summarize(studentId, período)", "s"),
            ("S", "AR", "findByStudentAndPeriod(studentId, from, to)", "s"),
            ("AR", "S", "List<Attendance>", "r"),
            ("S", "S", "calcula o percentual e compara com a frequência mínima", "s"),
            ("S", "C", "AttendanceSummary", "r"),
            ("C", "SPA", "200", "r"),
            ("SPA", "U", "percentual, faltas e lista de aulas, com alerta se abaixo da mínima", "r"),
            ("U", "SPA", "aciona “Grade horária”", "s"),
            ("SPA", "C", "GET /api/v1/portal/students/{id}/schedule", "s"),
            ("C", "SPA", "200 grade semanal", "r"),
            ("SPA", "U", "grade das turmas com matrícula ativa", "r"),
            ("else", "sem vínculo com o aluno"),
            ("C", "SPA", "403", "r"),
            ("SPA", "U", "acesso negado", "r"),
            ("end",),
        ],
    },
    "UC016": {
        "titulo": "Consultar painel gerencial",
        "p": [("U", "Administrador da escola", True), ("SPA", "SPA (React)", False),
              ("C", "DashboardController", False), ("S", "DashboardService", False),
              ("R", "ReportJdbcRepository", False), ("DB", "PostgreSQL (RLS)", False)],
        "m": [
            ("U", "SPA", "escolhe “Últimos 6 meses” e a unidade", "s"),
            ("SPA", "C", "GET /api/v1/dashboard?from&to&unitId", "s"),
            ("C", "S", "load(período, unidade)", "s"),
            ("S", "S", "valida o período (máximo de 24 meses)", "s"),
            ("alt", "período válido"),
            ("S", "R", "queryIndicators(params)", "s"),
            ("R", "DB", "SELECT agregados com app.tenant_id = tid", "s"),
            ("DB", "R", "linhas da organização", "r"),
            ("R", "S", "indicadores", "r"),
            ("S", "R", "queryMonthlySeries(params)", "s"),
            ("R", "S", "séries mensais", "r"),
            ("S", "C", "DashboardResponse", "r"),
            ("C", "SPA", "200", "r"),
            ("SPA", "U", "cartões e gráficos recalculados, ou “Sem dados para o período”", "r"),
            ("else", "período maior que 24 meses"),
            ("S", "C", "ValidationException", "r"),
            ("C", "SPA", "422 informa o limite", "r"),
            ("SPA", "U", "aviso de período inválido", "r"),
            ("end",),
        ],
    },
}


def quebra(texto, largura):
    limite = max(10, int(largura / CHAR))
    linhas, atual = [], ""
    palavras = texto.split(" ")
    if len(palavras) > 1 and palavras[0].endswith(":"):
        palavras = [palavras[0] + " " + palavras[1]] + palavras[2:]
    for palavra in palavras:
        candidato = (atual + " " + palavra).strip()
        if len(candidato) <= limite or not atual:
            atual = candidato
        else:
            linhas.append(atual)
            atual = palavra
    if atual:
        linhas.append(atual)
    return linhas


def quebra_nome(nome, largura):
    if len(nome) * 7.6 <= largura or " " in nome:
        return quebra(nome, largura)
    partes, atual = [], ""
    for i, letra in enumerate(nome):
        if letra.isupper() and atual and len(atual) * 7.6 > largura * 0.45:
            partes.append(atual)
            atual = ""
        atual += letra
    partes.append(atual)
    return partes


def texto(x, y, linhas, ancora="middle", peso="normal", tamanho=FONTE, cor="#111827"):
    partes = []
    for i, linha in enumerate(linhas):
        partes.append(
            f'<text x="{x:.1f}" y="{y + i * (tamanho + 3):.1f}" text-anchor="{ancora}" '
            f'font-size="{tamanho}" font-weight="{peso}" fill="{cor}" paint-order="stroke" stroke="#fff" '
            f'stroke-width="4" stroke-linejoin="round">{escape(linha)}</text>'
        )
    return "".join(partes)


def boneco(x, y):
    return (
        f'<circle cx="{x}" cy="{y}" r="9" fill="#fff" stroke="#111827" stroke-width="1.5"/>'
        f'<path d="M{x} {y + 9} V{y + 34} M{x - 15} {y + 18} H{x + 15} M{x} {y + 34} L{x - 12} {y + 52} '
        f'M{x} {y + 34} L{x + 12} {y + 52}" stroke="#111827" stroke-width="1.5" fill="none"/>'
    )


def gera(codigo, dados):
    partes = dados["p"]
    n = len(partes)
    margem = 86
    passo = (LARGURA - 2 * margem) / (n - 1)
    xs = {chave: margem + i * passo for i, (chave, _, _) in enumerate(partes)}
    ordem = {chave: i for i, (chave, _, _) in enumerate(partes)}
    fundo, meio, frente = [], [], []
    topo = 52
    caixa_l = min(170, passo - 10)
    for chave, nome, ator in partes:
        x = xs[chave]
        if ator:
            frente.append(boneco(x, topo - 4))
            frente.append(texto(x, topo + 66, quebra(nome, caixa_l), peso="bold"))
        else:
            linhas = quebra_nome(nome, caixa_l - 12)
            frente.append(
                f'<rect x="{x - caixa_l / 2:.1f}" y="{topo + 14}" width="{caixa_l:.1f}" height="50" rx="4" '
                f'fill="#EEF2FF" stroke="#3730A3" stroke-width="1.3"/>'
            )
            inicio = topo + 14 + 25 + 4 - (len(linhas) - 1) * (FONTE + 3) / 2
            frente.append(texto(x, inicio, linhas, peso="bold"))
    y = topo + 112
    y_vida = topo + 92
    pilhas = {chave: [] for chave in xs}
    ativacoes = {chave: [] for chave in xs}
    quadros = []
    numero = 0
    for item in dados["m"]:
        tipo = item[0]
        if tipo in ("alt", "opt", "loop"):
            quadros.append({"tipo": tipo, "y0": y - 14, "guarda": item[1], "idx": set(), "seps": []})
            y += 32
            continue
        if tipo == "else":
            quadros[-1]["seps"].append((y - 16, item[1]))
            y += 28
            continue
        if tipo == "end":
            q = quadros.pop()
            esquerda = min(q["idx"]) if q["idx"] else 0
            direita = max(q["idx"]) if q["idx"] else n - 1
            x0 = max(8, margem + esquerda * passo - 84)
            x1 = min(LARGURA - 8, margem + direita * passo + 84)
            fundo.append(
                f'<rect x="{x0:.1f}" y="{q["y0"]}" width="{x1 - x0:.1f}" height="{y - 14 - q["y0"]:.1f}" '
                f'fill="none" stroke="#6B7280" stroke-width="1.2"/>'
            )
            fundo.append(
                f'<path d="M{x0:.1f} {q["y0"]} h46 v14 l-8 8 h-38 z" fill="#E5E7EB" stroke="#6B7280" stroke-width="1.2"/>'
            )
            frente.append(texto(x0 + 20, q["y0"] + 16, [q["tipo"]], peso="bold", tamanho=12.5))
            frente.append(texto(x0 + 56, q["y0"] + 16, [f'[{q["guarda"]}]'], ancora="start", tamanho=12.5, cor="#374151"))
            for ys, guarda in q["seps"]:
                fundo.append(
                    f'<line x1="{x0:.1f}" y1="{ys}" x2="{x1:.1f}" y2="{ys}" stroke="#6B7280" stroke-width="1.1" stroke-dasharray="6 4"/>'
                )
                frente.append(texto(x0 + 10, ys + 16, [f"[{guarda}]"], ancora="start", tamanho=12.5, cor="#374151"))
            if quadros:
                quadros[-1]["idx"] |= q["idx"]
            y += 6
            continue
        origem, destino, rotulo, modo = item
        numero += 1
        rotulo = f"{numero}: {rotulo}"
        for q in quadros:
            q["idx"] |= {ordem[origem], ordem[destino]}
        if origem == destino:
            x = xs[origem]
            linhas = quebra(rotulo, min(250, LARGURA - x - 70))
            altura = max(26, len(linhas) * (FONTE + 3))
            meio.append(
                f'<path d="M{x + 5} {y} h42 v{altura - 6} h-36" fill="none" stroke="#111827" stroke-width="1.4" '
                f'marker-end="url(#cheia)"/>'
            )
            frente.append(texto(x + 56, y + 6, linhas, ancora="start"))
            y += altura + 14
            continue
        x1, x2 = xs[origem], xs[destino]
        sentido = 1 if x2 > x1 else -1
        linhas = quebra(rotulo, abs(x2 - x1) - 14)
        y += (len(linhas) - 1) * (FONTE + 3)
        base = y
        traco = ' stroke-dasharray="7 4"' if modo == "r" else ""
        ponta = "cheia" if modo == "s" else "aberta"
        meio.append(
            f'<line x1="{x1 + 6 * sentido:.1f}" y1="{base}" x2="{x2 - 7 * sentido:.1f}" y2="{base}" '
            f'stroke="#111827" stroke-width="1.4"{traco} marker-end="url(#{ponta})"/>'
        )
        primeira = base - 7 - (len(linhas) - 1) * (FONTE + 3)
        frente.append(texto((x1 + x2) / 2, primeira, linhas))
        if modo == "s":
            pilhas[destino].append(base)
        elif modo == "r":
            if pilhas[origem]:
                ativacoes[origem].append([pilhas[origem].pop(), base])
            elif ativacoes[origem]:
                ativacoes[origem][-1][1] = base
        y += 32
    fim = y - 8
    for chave, pilha in pilhas.items():
        for inicio in pilha:
            ativacoes[chave].append([inicio, inicio + 20])
    for chave, _, ator in partes:
        x = xs[chave]
        fundo.append(
            f'<line x1="{x}" y1="{y_vida if not ator else y_vida + 4}" x2="{x}" y2="{fim}" stroke="#6B7280" '
            f'stroke-width="1.1" stroke-dasharray="5 5"/>'
        )
        if ator:
            continue
        for inicio, final in ativacoes[chave]:
            meio.insert(
                0,
                f'<rect x="{x - 5}" y="{inicio}" width="10" height="{max(10, final - inicio):.1f}" fill="#fff" '
                f'stroke="#111827" stroke-width="1.2"/>',
            )
    altura_total = int(fim + 22)
    titulo = f"Diagrama de sequência — {codigo} {dados['titulo']}"
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{LARGURA}" height="{altura_total}" '
        f'viewBox="0 0 {LARGURA} {altura_total}" font-family="Arial, Helvetica, sans-serif">'
        '<defs>'
        '<marker id="cheia" markerWidth="11" markerHeight="9" refX="10" refY="4.5" orient="auto">'
        '<path d="M0 0 L11 4.5 L0 9 z" fill="#111827"/></marker>'
        '<marker id="aberta" markerWidth="11" markerHeight="9" refX="10" refY="4.5" orient="auto">'
        '<path d="M0 0 L10 4.5 L0 9" fill="none" stroke="#111827" stroke-width="1.4"/></marker>'
        '</defs>'
        f'<rect width="{LARGURA}" height="{altura_total}" fill="#fff"/>'
        + texto(LARGURA / 2, 30, [titulo], peso="bold", tamanho=16)
        + "".join(fundo) + "".join(meio) + "".join(frente) + "</svg>"
    )
    caminho = os.path.join(SAIDA, f"sequencia-{codigo}.svg")
    with open(caminho, "w", encoding="utf-8") as arquivo:
        arquivo.write(svg)
    return caminho, LARGURA, altura_total


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    for codigo, dados in SEQ.items():
        caminho, largura, altura = gera(codigo, dados)
        print(os.path.basename(caminho), largura, altura)
