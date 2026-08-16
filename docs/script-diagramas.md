# Script para geração dos diagramas — SGS-CADI Microserviços

> **Como usar:** cole o conteúdo inteiro deste arquivo (da linha `---` em diante) numa
> conversa nova com o Claude. Ele contém todo o contexto necessário — schema real,
> regras de negócio, atores e a especificação de cada diagrama. Peça os diagramas em
> blocos (ex.: "gere o Bloco 1") para não estourar o limite de uma resposta.
>
> Gerado a partir do código real em `services/core` e `services/documents`.
> O schema abaixo foi extraído das migrations, não escrito à mão.

---

# CONTEXTO

Você vai produzir a documentação em diagramas de um sistema real, para apresentação a uma
**banca acadêmica**. Rigor de notação UML importa: multiplicidades, estereótipos,
direção de setas e legendas devem estar corretos.

## O sistema

**SGS-CADI** é um sistema de gestão para uma instituição educacional socioassistencial
(contraturno escolar). Atende quatro domínios: **acadêmico** (oficinas, turmas, matrículas,
frequência), **administrativo** (pessoas, documentos), **financeiro** (patrocinadores,
lançamentos) e **psicossocial** (prontuários). Trata dados de **menores de idade**, o que
torna controle de acesso e rastreabilidade requisitos, não detalhes.

O sistema nasceu como monolito Spring Boot e está sendo decomposto em **microserviços
poliglotas** (linguagens e bancos heterogêneos). Os diagramas devem retratar a
**arquitetura alvo completa**, não o estado intermediário atual.

## Arquitetura alvo — 6 serviços

| Serviço | Linguagem | Banco | Responsabilidade |
|---|---|---|---|
| **cadi-identity** | Java 21 / Spring Boot | PostgreSQL | Usuários, papéis, emissão de JWT (RS256 + JWKS) |
| **cadi-core** | Java 21 / Spring Boot | PostgreSQL | Pessoas, turmas, matrículas, frequência, pareceres, psicossocial |
| **cadi-documents** | Python 3.12 / FastAPI | PostgreSQL + MinIO (S3) | Upload de documentos, atestados e fluxo de aprovação |
| **cadi-communication** | Node 20 / NestJS | **MongoDB** | Notificações e mural de avisos |
| **cadi-finance** | C# / .NET 8 | **MySQL** | Patrocinadores, lançamentos, documentos fiscais |
| **cadi-reports** | Python 3.12 / FastAPI | PostgreSQL (read model) | Relatórios via CQRS, alimentado por eventos |

**Infraestrutura:** Traefik (API gateway, roteamento por path), RabbitMQ (exchange *topic*
`cadi.events`), MinIO (storage S3), Keycloak (opcional, IdP), Jaeger + Prometheus
(observabilidade). Tudo em Docker Compose.

## Princípios arquiteturais (devem transparecer nos diagramas)

1. **Database-per-service** — nenhum serviço lê a tabela de outro. Referências entre
   serviços são **IDs lógicos, sem chave estrangeira física**.
2. **Consistência forte fica no core** — matrícula/vaga, registro de chamada, declaração
   oficial e baixa financeira **não** são distribuídos.
3. **Consistência eventual** para notificação, mural, justificativa por atestado,
   relatórios e auditoria.
4. **Comunicação síncrona (HTTP)** quando o chamador precisa da resposta para decidir;
   **assíncrona (evento)** quando o fato já aconteceu e o efeito pode chegar depois.
5. **Degradação graciosa** — a indisponibilidade de um serviço auxiliar não pode derrubar
   a função principal.

---

# SCHEMA REAL (extraído das migrations)

Notação: `PK` chave primária · `FK` chave estrangeira · `IDENTITY` auto-incremento.
Colunas `created_at`, `created_by`, `is_active`, `deactivated_by`, `deactivated_at` são o
padrão de auditoria + *soft delete* herdado por quase todas as entidades — **inclua-as**
nos diagramas (decisão explícita: o ER deve ter todas as colunas).

⚠️ **Importante sobre as FKs abaixo:** elas refletem o schema do monolito. Na arquitetura
alvo, toda FK que **cruza a fronteira de um serviço** deixa de existir fisicamente e vira
referência lógica. Exemplos que você deve tratar assim:
`person.user_id` → users (core → identity), `user_documents.owner_person_id` → person
(documents → core), `attendance.justified_by_document_id` → user_documents (core →
documents), `sponsor.student_id` → student (finance → core), `mural_post.author_id` →
users (communication → identity).


### cadi-identity — PostgreSQL
```
TABELA role
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    name                       VARCHAR(100)           NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    UNIQUE: (name)
```
```
TABELA users
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    username                   VARCHAR(255)           NOT NULL
    password                   VARCHAR(255)           NOT NULL
    email                      VARCHAR(255)           NOT NULL
    role_id                    BIGINT                 NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: role_id -> role.id
    UNIQUE: (username)
    UNIQUE: (email)
```

### cadi-core — PostgreSQL
```
TABELA address
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    zip_code                   VARCHAR(10)            NOT NULL
    street                     VARCHAR(255)           NOT NULL
    number                     INT                    NOT NULL
    complement                 VARCHAR(255)           
    neighborhood               VARCHAR(255)           NOT NULL
    city                       VARCHAR(255)           NOT NULL
    state                      VARCHAR(25)            NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)
```
```
TABELA person
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    name                       VARCHAR(255)           NOT NULL
    cpf                        VARCHAR(14)            NOT NULL
    email                      VARCHAR(255)           NOT NULL
    phone                      VARCHAR(20)            NOT NULL
    birth_date                 DATE                   NOT NULL
    address_id                 BIGINT                 NOT NULL
    user_id                    BIGINT                 NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: address_id -> address.id
    FK: user_id -> users.id
    UNIQUE: (cpf)
    UNIQUE: (email)
```
```
TABELA student
    person_id                  BIGINT                 PK
    grade                      VARCHAR(50)            NOT NULL
    current_school             VARCHAR(255)           NOT NULL
    period_studies             VARCHAR(30)            NOT NULL
    chronic_illness            VARCHAR(2000)          
    allergy                    VARCHAR(2000)          
    medicine                   VARCHAR(2000)          
    deficiency                 VARCHAR(2000)          
    follow_up                  VARCHAR(2000)          
    surgery                    VARCHAR(2000)          
    FK: person_id -> person.id
```
```
TABELA guardian
    person_id                  BIGINT                 PK
    is_primary                 BOOLEAN                NOT NULL
    FK: person_id -> person.id
```
```
TABELA collaborator
    person_id                  BIGINT                 PK
    education_level            VARCHAR(255)           NOT NULL
    education_cert_type        VARCHAR(255)           
    institution_cert           VARCHAR(255)           
    year_conclusion            INT                    
    department                 VARCHAR(255)           
    type                       VARCHAR(255)           NOT NULL
    FK: person_id -> person.id
```
```
TABELA student_guardian
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    student_id                 BIGINT                 NOT NULL
    guardian_id                BIGINT                 NOT NULL
    family_tie                 VARCHAR(50)            NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: student_id -> student.person_id
    FK: guardian_id -> guardian.person_id
```
```
TABELA workshop
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    name                       VARCHAR(255)           NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)
```
```
TABELA schoolroom
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    name                       VARCHAR(255)           NOT NULL
    schedule                   VARCHAR(255)           
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    classroom                  VARCHAR(50)            NOT NULL
    workshop_id                BIGINT                 NOT NULL
    teacher_id                 BIGINT                 
    capacity                   INT                    
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: workshop_id -> workshop.id
    FK: teacher_id -> collaborator.person_id
```
```
TABELA schoolroom_schedule_slot
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    schoolroom_id              BIGINT                 NOT NULL
    day_of_week                VARCHAR(16)            NOT NULL
    start_time                 TIME                   NOT NULL
    end_time                   TIME                   NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: schoolroom_id -> schoolroom.id
```
```
TABELA lecture
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    schoolroom_id              BIGINT                 NOT NULL
    date_time                  TIMESTAMP              NOT NULL
    topic                      VARCHAR(500)           NOT NULL
    closed                     BOOLEAN                NOT NULL DEFAULT FALSE
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: schoolroom_id -> schoolroom.id
```
```
TABELA enrollment
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    student_id                 BIGINT                 NOT NULL
    schoolroom_id              BIGINT                 NOT NULL
    enrollment_date            DATE                   NOT NULL
    status                     BOOLEAN                NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: student_id -> student.person_id
    FK: schoolroom_id -> schoolroom.id
```
```
TABELA attendance
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    lecture_id                 BIGINT                 NOT NULL
    student_id                 BIGINT                 NOT NULL
    present                    BOOLEAN                NOT NULL
    observation                VARCHAR(500)           
    status                     VARCHAR(20)            NOT NULL DEFAULT 'PRESENT'
    justified_by_document_id   BIGINT                 
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: lecture_id -> lecture.id
    FK: student_id -> student.person_id
    FK: justified_by_document_id -> user_documents.id
```
```
TABELA descriptive_report
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    student_id                 BIGINT                 NOT NULL
    schoolroom_id              BIGINT                 NOT NULL
    author_id                  BIGINT                 NOT NULL
    content                    VARCHAR(4000)          NOT NULL
    status                     VARCHAR(20)            NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    updated_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    published_at               TIMESTAMP              
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: schoolroom_id -> schoolroom.id
    UNIQUE: (student_id, schoolroom_id)
```
```
TABELA descriptive_report_history
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    descriptive_report_id      BIGINT                 NOT NULL
    actor_id                   BIGINT                 NOT NULL
    action                     VARCHAR(30)            NOT NULL
    previous_content           VARCHAR(4000)          
    occurred_at                TIMESTAMP(3)           NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: descriptive_report_id -> descriptive_report.id
    FK: actor_id -> collaborator.person_id
```
```
TABELA student_pre_register
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    name                       VARCHAR(255)           NOT NULL
    email                      VARCHAR(255)           NOT NULL
    phone                      VARCHAR(50)            NOT NULL
    workshop_id                BIGINT                 NOT NULL
    shift                      VARCHAR(20)            NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    summoned_at                TIMESTAMP(3)           
    summoned_by                BIGINT                 
    FK: workshop_id -> workshop.id
    FK: summoned_by -> users.id
```
```
TABELA patient_record
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    notes                      TEXT                   NOT NULL
    confidential_code          VARCHAR(255)           NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    patient_id                 BIGINT                 NOT NULL
    psychologist_id            BIGINT                 NOT NULL
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: psychologist_id -> collaborator.person_id
```
```
TABELA patient_assignment
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    psychologist_id            BIGINT                 NOT NULL
    patient_id                 BIGINT                 NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: psychologist_id -> collaborator.person_id
```
```
TABELA audit_log
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    table_name                 VARCHAR(64)            NOT NULL
    record_id                  BIGINT                 NOT NULL
    action                     VARCHAR(10)            NOT NULL
    old_data                   TEXT                   
    new_data                   TEXT                   
    changed_by                 BIGINT                 
    changed_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
======================================================================
### documents(alembic) | 0001_initial_schema.py
======================================================================
    col id                       sa.BigInteger()
    col owner_person_id          sa.BigInteger()
    col file_name                sa.String(255)
    col original_file_name       sa.String(255)
    col mime_type                sa.String(100)
    col file_size                sa.BigInteger()
    col category                 sa.String(30)
    col scope                    sa.String(30)
    col storage_key              sa.String(512)
    col uploaded_by_user_id      sa.BigInteger()
    col uploaded_at              sa.DateTime(timezone=True)
    col description              sa.String(500)
    col active                   sa.Boolean()
    col status                   sa.String(20)
    col reference_date_start     sa.Date()
    col reference_date_end       sa.Date()
    col reviewed_by_user_id      sa.BigInteger()
    col reviewed_at              sa.DateTime(timezone=True)
    col rejection_reason         sa.String(500)

TABELA(alembic) user_documents
```

### cadi-documents — PostgreSQL + MinIO
```
TABELA user_documents
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    owner_person_id            BIGINT                 NOT NULL
    file_name                  VARCHAR(255)           NOT NULL
    original_file_name         VARCHAR(255)           NOT NULL
    mime_type                  VARCHAR(100)           NOT NULL
    file_size                  BIGINT                 NOT NULL
    category                   VARCHAR(30)            NOT NULL
    scope                      VARCHAR(30)            NOT NULL
    storage_key                VARCHAR(512)           NOT NULL
    uploaded_by_user_id        BIGINT                 NOT NULL
    uploaded_at                TIMESTAMP              NOT NULL
    description                VARCHAR(500)           
    active                     BOOLEAN                NOT NULL DEFAULT TRUE
    status                     VARCHAR(20)            NOT NULL DEFAULT 'PENDING'
    reference_date_start       DATE                   
    reference_date_end         DATE                   
    reviewed_by_user_id        BIGINT                 
    reviewed_at                TIMESTAMP              
    rejection_reason           VARCHAR(500)           
    FK: uploaded_by_user_id -> users.id
    FK: reviewed_by_user_id -> users.id
```

### cadi-communication — MongoDB
```
TABELA notification
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    title                      VARCHAR(255)           NOT NULL
    message                    TEXT                   NOT NULL
    type                       VARCHAR(50)            NOT NULL
    user_id                    BIGINT                 
    read_at                    TIMESTAMP(3)           
    action_url                 VARCHAR(500)           
    reference_key              VARCHAR(100)           
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: user_id -> users.id
```
```
TABELA mural_post
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    title                      VARCHAR(255)           NOT NULL
    content                    TEXT                   NOT NULL
    author_id                  BIGINT                 NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    updated_at                 TIMESTAMP(3)           
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: author_id -> users.id
```
```
TABELA mural_post_audience
    post_id                    BIGINT                 NOT NULL
    role_name                  VARCHAR(100)           NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: post_id -> mural_post.id
```
```
TABELA mural_post_audience_schoolroom
    post_id                    BIGINT                 NOT NULL
    schoolroom_id              BIGINT                 NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: schoolroom_id -> schoolroom.id
```
```
TABELA mural_post_view
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    post_id                    BIGINT                 NOT NULL
    user_id                    BIGINT                 NOT NULL
    viewed_at                  TIMESTAMP              
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: post_id -> mural_post.id
    FK: user_id -> users.id
    UNIQUE: (post_id, user_id)
```
```
TABELA message_outbox
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    exchange                   VARCHAR(255)           NOT NULL
    routing_key                VARCHAR(255)           NOT NULL
    payload                    TEXT                   NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    processed                  BOOLEAN                NOT NULL DEFAULT FALSE
    processed_at               TIMESTAMP              
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)
```

### cadi-finance — MySQL
```
TABELA financial_transaction
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    type                       VARCHAR(30)            NOT NULL
    datetime                   TIMESTAMP              NOT NULL
    value_transaction          REAL                   NOT NULL
    description                VARCHAR(500)           
    receipt_url                VARCHAR(2048)          
    user_id                    BIGINT                 NOT NULL
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: user_id -> users.id
```
```
TABELA fiscal_document
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    document_type              VARCHAR(30)            NOT NULL
    document_number            VARCHAR(100)           NOT NULL
    issue_date                 DATE                   NOT NULL
    total_amount               DECIMAL(12,2)          NOT NULL
    supplier_name              VARCHAR(255)           
    supplier_doc               VARCHAR(30)            
    file_url                   VARCHAR(2048)          
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: deactivated_by -> users.id
```
```
TABELA sponsor
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    student_id                 BIGINT                 NOT NULL
    name                       VARCHAR(255)           NOT NULL
    email                      VARCHAR(255)           
    phone                      VARCHAR(50)            
    amount                     DECIMAL(12,2)          NOT NULL
    frequency                  VARCHAR(20)            NOT NULL
    start_date                 DATE                   NOT NULL
    last_generated_date        DATE                   
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: deactivated_by -> users.id
```
```
TABELA financial_entry
    id                         BIGINT                 PK IDENTITY DEFAULT AS IDENTITY PRIMARY KEY
    direction                  VARCHAR(20)            NOT NULL
    status                     VARCHAR(20)            NOT NULL
    due_date                   DATE                   NOT NULL
    paid_at                    DATE                   
    amount                     DECIMAL(12,2)          NOT NULL
    category                   VARCHAR(100)           NOT NULL
    description                VARCHAR(500)           
    sponsor_id                 BIGINT                 
    fiscal_document_id         BIGINT                 
    created_at                 TIMESTAMP(3)           NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_by                 BIGINT                 
    is_active                  BOOLEAN                NOT NULL DEFAULT TRUE
    deactivated_by             BIGINT                 
    deactivated_at             TIMESTAMP(3)           
    FK: fiscal_document_id -> fiscal_document.id
```


### cadi-reports — PostgreSQL (read model, CQRS)

Não existe ainda; projete-o. É um **modelo de leitura desnormalizado**, alimentado por
eventos dos demais serviços, com tabelas por relatório (ex.: `rm_frequencia_turma`,
`rm_aluno_risco`, `rm_matricula`). Deve conter cópia dos dados necessários para os JOINs
que hoje cruzam domínios. Inclua uma tabela de controle de idempotência
(ex.: `processed_events(event_id, processed_at)`).

---

# ENUMS

```
RolesEnum         : ROLE_ADMIN, ROLE_SECRETARY, ROLE_MANAGER, ROLE_PROFESSOR,
                    ROLE_STUDENT, ROLE_PARENT, ROLE_PSYCHOLOGIST, ROLE_STAFF, ROLE_VOLUNTEER
AttendanceStatus  : PRESENT, ABSENT, JUSTIFIED
DescriptiveReportStatus : DRAFT, PUBLISHED, CLOSED
CollaboratorType  : TEACHER, VOLUNTEER, STAFF, SECRETARY, DIRECTOR, PSYCHOLOGIST
FamilyTie         : FATHER, MOTHER, ANOTHER_FAMILY_TIE
Shift             : MORNING, AFTERNOON, EVENING
DocumentCategory  : ATESTADO_MEDICO, JUSTIFICATIVA_FALTA, COMPROVANTE_RESIDENCIA, RG, CPF,
                    COMPROVANTE_MATRICULA, NOTA_FISCAL, ANOTACAO_TERAPEUTICA, OUTROS
DocumentScope     : STUDENT_PERSONAL, INSTITUTIONAL, THERAPEUTIC_PRIVATE
DocumentStatus    : PENDING, APPROVED, REJECTED
```

# REGRAS DE NEGÓCIO (verificadas no código)

**Matrícula**
- Um aluno pode ter no **máximo 2 matrículas ativas** simultâneas.
- A turma tem `capacity`; sem vaga, o interessado entra em **fila de espera**
  (`student_pre_register`, que guarda nome/e-mail/telefone — é uma *pré-pessoa*, ainda
  não existe `person`).
- Ao liberar vaga, o sistema emite evento e a secretaria **convoca** o primeiro da fila
  (registra `summoned_at`/`summoned_by`).

**Chamada (frequência)**
- Só é permitida em dia que bate com a **grade da turma** (`schoolroom_schedule_slot`).
- **Não** pode ser lançada em data futura nem anterior a **35 dias**.
- Uma chamada por turma por dia; após lançada, fecha (`lecture.closed`) e só
  **SECRETARY/ADMIN/MANAGER** podem reabrir.
- Professor só acessa as **próprias turmas**; gestão acessa todas.
- Ao gravar `ABSENT`, o core consulta o `cadi-documents`: havendo atestado **APPROVED**
  cobrindo a data, a falta nasce `JUSTIFIED` com `justified_by_document_id` preenchido.

**Atestado**
- Categorias sujeitas a aprovação: `ATESTADO_MEDICO` e `JUSTIFICATIVA_FALTA`.
- Aluno envia o próprio; **responsável** pode enviar pelo aluno (exige verificação de
  vínculo, que vive no core); psicólogo envia `ANOTACAO_TERAPEUTICA` (escopo
  `THERAPEUTIC_PRIVATE`, visível só a quem enviou).
- Secretaria aprova informando **período de referência** (início/fim) ou rejeita com
  **motivo obrigatório**.
- **Reaprovação** revoga o efeito anterior e reaplica com o novo período.
- Excluir documento aprovado **reverte** as justificativas.
- Upload: máx. **10 MB**; tipos aceitos `application/pdf`, `image/jpeg`, `image/png`,
  `image/webp`; MIME detectado por **magic bytes** no servidor (o header do cliente é
  ignorado, para evitar *spoofing*).

**Parecer descritivo**
- Um por aluno/turma; exige matrícula ativa.
- `DRAFT` → `PUBLISHED` → `CLOSED`. Publicado/fechado não aceita edição.

**Documentos e privacidade**
- `THERAPEUTIC_PRIVATE`: só o psicólogo que enviou acessa.
- Demais escopos: o dono (comparado por `person.id`) ou ADMIN/SECRETARY.
- Exclusão é *soft delete* + remoção do arquivo no storage.

# ATORES E PERMISSÕES

| Ator | Papel | Principais capacidades |
|---|---|---|
| **Administrador** | ROLE_ADMIN | Acesso total; gestão de usuários e papéis |
| **Secretaria** | ROLE_SECRETARY | Cadastros, matrículas, aprovação de atestados, reabertura de chamada, relatórios |
| **Gestor** | ROLE_MANAGER | Financeiro, relatórios, reabertura de chamada |
| **Professor** | ROLE_PROFESSOR | Chamada e pareceres das **próprias** turmas |
| **Aluno** | ROLE_STUDENT | Consulta própria (turmas, frequência, mural), envio de atestado |
| **Responsável** | ROLE_PARENT | Consulta e envio de documentos **dos alunos sob sua responsabilidade** |
| **Psicólogo** | ROLE_PSYCHOLOGIST | Pacientes vinculados, prontuários, anotações terapêuticas |

Atores de sistema: **Serviço de E-mail (SMTP)**, **RabbitMQ**, **MinIO**.

# EVENTOS (RabbitMQ — exchange topic `cadi.events`)

| Evento | Routing key | Produtor | Consumidores | Efeito |
|---|---|---|---|---|
| `AtestadoApproved` | `atestado.approved` | documents | core, reports | Faltas do período viram JUSTIFIED |
| `AtestadoRevoked` | `atestado.revoked` | documents | core, reports | Justificativas revertidas para ABSENT |
| `SchoolRoomVacancyOpened` | `schoolroom.vacancy` | core | communication | Notifica fila de espera |
| `StudentEnrolled` | `student.enrolled` | core | reports, communication | Atualiza projeção |
| `AttendanceRegistered` | `attendance.registered` | core | reports | Atualiza projeção de frequência |
| `PaymentOverdue` | `payment.overdue` | finance | communication | Dispara cobrança |

**Envelope padrão de todo evento:**
```json
{ "eventId": "uuid", "eventType": "AtestadoApproved", "version": 1,
  "occurredAt": "ISO-8601", "aggregateId": "123", "payload": { } }
```
Entrega é *at-least-once* → **consumidores são idempotentes** e cada fila tem
*dead letter queue*.

---

# O QUE VOCÊ DEVE PRODUZIR

## Convenções obrigatórias

- **Idioma:** tudo em **português**, inclusive nomes de tabelas, colunas e serviços
  (ex.: `pessoa`, `matricula`, `documento_usuario`). Ao traduzir, **inclua o nome
  original do código entre parênteses** na primeira ocorrência de cada diagrama, para
  manter rastreabilidade (ex.: `matricula (enrollment)`).
- **Notação:** conforme indicado em cada bloco — **Mermaid** ou **PlantUML**.
- **Legenda:** todo diagrama começa com um parágrafo curto explicando o que ele mostra e
  termina com as convenções usadas (cores, estereótipos, o que uma linha tracejada
  significa).
- **Fronteira de serviço:** sempre visível — use `package`/`namespace` (PlantUML) ou
  `subgraph` (Mermaid), com o banco de cada serviço explícito.
- **FK lógica vs física:** linha **cheia** para FK real dentro do mesmo banco; linha
  **tracejada** para referência lógica que cruza serviço. Explique isso na legenda.
- Cada diagrama vem em **bloco de código próprio**, precedido de um título `##`.

## Bloco 1 — Diagramas relacionais (Mermaid `erDiagram`)

1.1. **ER geral do sistema** — todas as entidades, agrupadas visualmente por serviço,
com as referências lógicas entre serviços tracejadas. Este é o panorâmico.

1.2 a 1.6. **Um ER por serviço** (identity, core, documents, finance, reports), com
**todas as colunas**, tipos e restrições. No ER do core, represente corretamente a
**herança JOINED**: `pessoa` é a raiz e `aluno`/`responsavel`/`colaborador` compartilham
a mesma PK (`person_id`), com relacionamento 1:1 identificante.

1.7. **cadi-communication em MongoDB** — **não** use `erDiagram`. Modele como
**coleções com documentos aninhados**, mostrando JSON de exemplo comentado. O ponto a
demonstrar: `mural_post` tinha a audiência em **duas tabelas auxiliares**
(`mural_post_audience` por papel e `mural_post_audience_schoolroom` por turma) e no
modelo de documento vira **um array embutido** no próprio post. Inclua um parágrafo
justificando por que esse domínio motivou a escolha de MongoDB.

## Bloco 2 — Diagramas de atividade (PlantUML `@startuml` + `start`/`stop`)

Use *swimlanes* por ator. Marque decisões com `if/else`, paralelismo com `fork`, e
eventos publicados com uma nota lateral.

2.1. **Aprovação de atestado** — secretaria analisa; aprova (com período) ou rejeita (com
motivo). Cubra: **reaprovação** (revoga o efeito anterior e reaplica), **exclusão de
documento aprovado** (reverte) e a publicação dos eventos `AtestadoApproved` /
`AtestadoRevoked`.

2.2. **Matrícula com vaga e fila de espera** — verifica capacidade da turma e o limite de
2 matrículas ativas; havendo vaga, matricula; senão, entra na fila. Inclua o ramo em que
uma vaga é liberada depois, o evento `SchoolRoomVacancyOpened` e a convocação.

2.3. **Ciclo da chamada** — abrir (validando grade horária e a janela de 35 dias),
registrar presenças (com a consulta de atestado ao serviço de documentos, incluindo o
caminho de falha em que o serviço está indisponível), fechar e reabrir (restrito a
secretaria/gestão).

2.4. **Parecer descritivo** — modele como **diagrama de estados** (`@startuml` com
`[*] --> DRAFT`), mostrando quem pode transicionar cada estado.

## Bloco 3 — Diagramas de sequência (Mermaid `sequenceDiagram`)

Inclua a infraestrutura como participante: **Traefik**, **RabbitMQ**, **MinIO** e os
bancos. Use `alt`/`opt`/`par` para caminhos alternativos e `Note over` para invariantes.

3.1. **Login e propagação do JWT** — autenticação no identity, emissão do token com as
claims `sub`, `uid` (id do usuário), `pid` (id da pessoa), `roles`; requisição seguinte
atravessando Traefik até um serviço que valida o token **localmente** pela chave pública
(JWKS), sem consultar o identity. Deixe explícito o ganho: nenhuma consulta a banco por
requisição para autenticar.

3.2. **Upload e aprovação de atestado (cross-service)** — o mais completo:
responsável envia arquivo → Traefik → documents valida MIME por magic bytes → grava no
MinIO → persiste metadados → secretaria aprova → documents publica `AtestadoApproved` no
RabbitMQ → core consome e justifica as faltas do período no **seu próprio** banco.
Mostre os **dois bancos separados** e destaque que o core **não** escreve em
`documento_usuario` nem o documents em `frequencia`.

3.3. **Registro de chamada com consulta de atestado** — professor lança faltas; para cada
`ABSENT` o core pergunta ao documents se há atestado cobrindo a data. Use `alt` com dois
caminhos: **(a)** documents responde e a falta nasce JUSTIFIED; **(b)** documents está
**indisponível** — a falta é gravada como ABSENT e o sistema segue funcionando
(degradação graciosa), sendo corrigida depois pelo evento. Este é o diagrama que
demonstra resiliência.

3.4. **Pré-cadastro, vaga e convocação** — interessado se inscreve sem ter cadastro;
quando um aluno sai da turma, o core publica `SchoolRoomVacancyOpened`; o communication
consome, notifica e dispara e-mail; a secretaria convoca. Mostre o padrão **outbox**
(persistir o evento na mesma transação e publicar depois) e a fila de espera.

## Bloco 4 — Diagramas de caso de uso (PlantUML `@startuml` + `left to right direction`)

Use `actor`, `usecase`, `package` para os subsistemas, e os estereótipos `<<include>>` e
`<<extend>>` corretamente. Modele **generalização entre atores** onde couber (ex.:
Administrador herda de Secretaria).

4.1. **Diagrama geral** — os 7 atores humanos + atores de sistema, com os casos de uso
agrupados em `package` por serviço. Visão panorâmica.

4.2 a 4.6. **Um diagrama por subsistema** (core, documents, communication, finance,
reports), detalhando os casos de uso daquele serviço e apenas os atores que interagem
com ele. Exemplos de `<<include>>`: "Registrar chamada" inclui "Verificar atestado";
"Aprovar atestado" inclui "Justificar faltas". Exemplo de `<<extend>>`: "Matricular
aluno" é estendido por "Entrar na fila de espera" (quando não há vaga).

## Bloco 5 — Diagramas complementares

5.1. **Diagrama de componentes** (PlantUML `component`) — os 6 serviços, seus bancos, o
gateway, o broker, o storage e as **interfaces** entre eles. Diferencie visualmente
chamada síncrona (HTTP) de publicação/consumo de evento. Mostre as interfaces providas e
requeridas (`lollipop`/`socket`).

5.2. **Diagrama de implantação** (PlantUML `deployment`) — nós de execução: máquina
hospedeira (AWS EC2) contendo os containers Docker, com portas expostas, volumes
persistentes e a rede interna. Inclua Traefik na borda e indique quais portas são
públicas e quais são internas.

---

# ORDEM E FORMA DE ENTREGA

Gere **um bloco por vez**, nesta ordem: 1 → 2 → 3 → 4 → 5. Ao final de cada bloco,
pare e pergunte se deve seguir para o próximo. Antes de cada diagrama, escreva um
parágrafo curto explicando **o que ele demonstra e por que essa escolha de modelagem**.

Se identificar contradição ou lacuna no material acima, **aponte antes de desenhar** em
vez de preencher com suposição — a fidelidade ao sistema real vale mais que completude.
