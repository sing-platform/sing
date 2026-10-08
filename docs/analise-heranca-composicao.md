# Análise: herança × composição no SING

> Levantamento de todas as hierarquias do código, com veredito e plano de refatoração.
> Todos os números foram **medidos no código**, não estimados.
>
> Base analisada: `services/core` (Java 21 / Spring Boot) e `services/documents` (Python / FastAPI).

## Sumário

| # | Hierarquia | Filhos | Veredito | Custo | Prioridade |
|---|---|---|---|---|---|
| 1 | `AbstractReport` | 17 | 🔴 composição | baixo | **1ª** |
| 2 | `ProfessorScheduleResponse ← CollaboratorResponse` | 1 | 🔴 composição | trivial | **2ª** |
| 3 | `AuditableSoftDeleteEntity` | **21** | 🟠 composição (`@Embeddable`) | médio | **3ª** |
| 4 | `Person → Student/Guardian/Collaborator` | 3 | 🟠 composição (Party-Role) | médio | 4ª |
| 5 | `NoOpJavaMailSender ← JavaMailSenderImpl` | 1 | 🟡 usar a interface | trivial | 5ª |
| 6 | `JwtAuthenticationFilter ← OncePerRequestFilter` | 1 | ✅ manter | — | — |
| 7 | 9 exceções `← RuntimeException` | 9 | ✅ manter | — | — |
| 8 | Python: `Base`, `_CamelModel`, `str+Enum` | vários | ✅ manter | — | — |

**Observação transversal:** toda herança legítima encontrada é **imposta por framework ou
linguagem**. Nenhuma herança de *domínio* escrita pela equipe passou nos critérios — o que
é comum, e é exatamente o que o princípio "composição sobre herança" prevê.

## Critérios de avaliação

1. **Teste do "é um" permanente** — se o subtipo pode mudar em tempo de execução, não é
   herança: é **estado**.
2. **Herança para reuso de código** — smell. Contrato pertence a interface; reuso, a
   composição ou função.
3. **A base carrega acoplamento que o filho não quer?**
4. **LSP (Liskov)** — o filho é substituível pelo pai em todo contexto?
5. **Template method de framework** — geralmente legítimo.

---

## 1. `AbstractReport` — herança para economizar três linhas

**Arquivo:** `services/core/.../reports/application/definitions/AbstractReport.java`
**Filhos:** 17 relatórios

### O que a base oferece

```java
abstract class AbstractReport {
    protected final ReportJdbcRepository jdbc;
    protected AbstractReport(ReportJdbcRepository jdbc) { this.jdbc = jdbc; }
    protected static List<ReportFilterSpec> periodFilters() { /* 2 filtros de data */ }
    protected Map<String,Object> params(Map<String,Object> f) { return new HashMap<>(f); }
}
```

Um campo, um helper estático e um método que faz `new HashMap<>()`.

### Por que é desnecessária

O próprio código prova. Cada filho faz:

```java
public StudentsAtRiskReport(ReportJdbcRepository jdbc) { super(jdbc); }
```

A dependência **já chega por injeção no construtor** — o `super(jdbc)` apenas empurra ao pai
o que o filho já tinha. O contrato real já existe e é a interface `ReportDefinition`
(`slug`, `displayName`, `filters`, `execute`). A classe abstrata não participa do contrato,
só do reuso.

### Argumento arquitetural adicional

No roadmap poliglota, os relatórios migram para **Python no E5**. Uma hierarquia Java não
sobrevive à migração; um **contrato** sobrevive. Manter `AbstractReport` é construir
acoplamento que já se sabe descartável.

### Refatoração

```java
@Component
public class StudentsAtRiskReport implements ReportDefinition {
    private final ReportJdbcRepository jdbc;
    public StudentsAtRiskReport(ReportJdbcRepository jdbc) { this.jdbc = jdbc; }
    // periodFilters() -> ReportFilterSpec.periodFilters()  (utilitário estático)
    // params(f)       -> new HashMap<>(f)                  (inline)
}
```

**Ganho:** elimina hierarquia de 17 classes; cada relatório fica autônomo e testável isolado.
**Custo:** mecânico — 17 arquivos, ~3 linhas cada.
**Risco:** baixo. Validável com o smoke test (14 relatórios devem seguir em 200).

---

## 2. `ProfessorScheduleResponse extends CollaboratorResponse` — a herança se autodenuncia

**Arquivo:** `services/core/.../people/api/dto/response/ProfessorScheduleResponse.java`

```java
@EqualsAndHashCode(callSuper = false)          // ← a confissão
public class ProfessorScheduleResponse extends CollaboratorResponse {
    List<FullSchedulesProfessorResponse> infos;
}
```

### O defeito

`callSuper = false` significa: *"ao comparar dois objetos, ignore todos os campos herdados"*
— nome, e-mail, CPF, telefone, data de nascimento, endereço. **Dois professores diferentes
com a mesma agenda são considerados iguais.**

Quando é preciso desativar o comportamento do pai para o filho funcionar, a herança está
errada.

### Defeito secundário

Herança em DTO **acopla contratos de API**: alterar `CollaboratorResponse` (usado em vários
endpoints) muda silenciosamente o payload deste.

### Refatoração

```java
public record ProfessorScheduleResponse(
    CollaboratorResponse colaborador,
    List<FullSchedulesProfessorResponse> infos
) {}
```

**Atenção:** muda o formato do JSON (passa a aninhar). Se o frontend consome esse endpoint,
alinhe antes — ou mantenha o formato plano copiando os campos.

---

## 3. `AuditableSoftDeleteEntity` — o de maior impacto arquitetural

**Arquivo:** `services/core/.../common/domain/AuditableSoftDeleteEntity.java`
**Filhos:** **21 entidades, em 7 módulos**

```java
@MappedSuperclass
public abstract class AuditableSoftDeleteEntity {
    private LocalDateTime createdAt;      // ─┐ auditoria
    @ManyToOne private User createdBy;    // ─┘
    private boolean isActive = true;      // ─┐
    @ManyToOne private User deactivatedBy;//  │ soft delete
    private LocalDateTime deactivatedAt;  // ─┘
}
```

### Problema (a) — duas responsabilidades

Auditoria e exclusão lógica são conceitos distintos. Nem toda entidade auditável precisa de
soft delete, e vice-versa. Hoje, herdar uma obriga a herdar a outra (violação de SRP).

### Problema (b) — acopla 21 entidades ao `iam` · **crítico para microserviços**

`@ManyToOne User` × 2 × 21 entidades ≈ **42 chaves estrangeiras** para `users`.

O impacto é concreto no roadmap: `financial_entry` vai para **MySQL** (E4) e `users` para o
serviço de **identity**. **FK entre bancos diferentes não existe.** A herança atual torna a
decomposição impossível sem alterar as 21 entidades.

### Refatoração (composição por `@Embeddable`)

```java
@Embeddable
public class AuditInfo {
    private LocalDateTime createdAt;
    private Long createdByUserId;          // referência lógica, não @ManyToOne
}

@Embeddable
public class SoftDelete {
    private boolean active = true;
    private Long deactivatedByUserId;
    private LocalDateTime deactivatedAt;
}

@Entity
public class Enrollment {
    @Embedded private AuditInfo audit;
    @Embedded private SoftDelete exclusao;   // só quem realmente precisa
}
```

**O que barateia:** `@Embedded` mapeia para **as mesmas colunas**. `created_at`,
`created_by`, `is_active` permanecem onde estão — a migration apenas **dropa as FKs**, sem
mover dado.

### Caminho intermediário (recomendado se o prazo apertar)

Manter o `@MappedSuperclass` e trocar **apenas** `@ManyToOne User` por `Long`.
Resolve (b), adia (a). Foi o que o plano de fundação registrou como item F0.2.

---

## 4. `Person → Student / Guardian / Collaborator`

**Arquivo:** `services/core/.../people/domain/entities/Person.java`
`@Inheritance(strategy = InheritanceType.JOINED)`

### Medições

| Métrica | Valor |
|---|---|
| `instanceof` nos subtipos | **6** |
| Chamadas a getters **herdados** de `Person` | **~128** |
| Arquivos usando os repositórios de subtipo | ~29 |

O custo real **não** está nos `instanceof` (são 6, triviais) — está nos ~128 pontos que
fazem `student.getName()`.

### Por que herança é o modelo errado aqui

> **Se o subtipo pode mudar em tempo de execução, não é herança — é estado.**

Um aluno se forma e vira voluntário; um responsável também pode ser colaborador. O "tipo"
**muda e acumula** ao longo da vida. Herança modela como imutável algo que é temporal — e,
em Java, um objeto tem um tipo só.

### O sintoma no código

`config/DataInicializer.java:278`

```java
if (existing instanceof Collaborator collaborator)  { collaboratorRepository.save(...); }
else if (existing instanceof Guardian guardian)     { guardianRepository.save(...); }
else if (existing instanceof Student student)       { studentRepository.save(...); }
```

`instanceof` em cascata para escolher repositório é o smell canônico de "herança onde
deveria haver papel".

### Padrão alvo

**Party-Role** (Silverston) / **Accountability** (Fowler), com **Role Object** na camada de
objeto: `pessoa` + `vinculo(pessoa × unidade × papel × período)`.

### Três estratégias de migração

| | Estratégia | Bloqueia? | Custo |
|---|---|---|---|
| **A** | Party-Role completo, de uma vez | **sim** | alto |
| **B** | *Expand/Contract* — `person_role` coexiste com a herança; migra incremental; *Branch by Abstraction* nos 6 `instanceof` | não | médio |
| **C** | **Aditivo** — mantém a herança intacta e usa `person_role` **só para papéis novos** (APADRINHADO, ACOLHIDO, PADRINHO, VOLUNTARIO) | não | ~zero |

**C é o passo "Expand" de B** — não é beco sem saída. Para validação de produto (protótipo /
incubadora), C entrega o requisito de múltiplos papéis com refatoração zero do existente.

### Técnica que preserva os ~128 call sites

Se/quando a herança for quebrada, usar **delegação**:

```java
@Entity
public class Student {
    @OneToOne @JoinColumn(name = "person_id")
    private Person person;

    private String grade;                                   // campos próprios

    public String getName()  { return person.getName(); }   // call sites seguem compilando
    public Long   getId()    { return person.getId(); }
    public String getEmail() { return person.getEmail(); }
}
```

As tabelas `student`, `guardian` e `collaborator` **não precisam ser removidas**: deixam de
ser subtipos e viram *atributos do papel*. Os dados (`grade`, `education_level`, `allergy`)
permanecem onde estão.

---

## 5. `NoOpJavaMailSender extends JavaMailSenderImpl`

Estende a **implementação concreta** do Spring para anular comportamento, herdando estado que
não usa (host, porta, pool de conexão). O correto é implementar a **interface**
`JavaMailSender` — objeto nulo sem bagagem.

Diferença conceitual: *"herdo de quem faz"* × *"cumpro o contrato"*.

---

## 6–8. Onde a herança está correta

| Caso | Por quê |
|---|---|
| `JwtAuthenticationFilter ← OncePerRequestFilter` | *Template method* de framework; o Spring exige essa forma |
| 9 exceções `← RuntimeException` | Mecanismo da linguagem para exceções |
| `Base(DeclarativeBase)` — SQLAlchemy | Idiomático do ORM |
| `_CamelModel(BaseModel)` — Pydantic | Idiomático; centraliza a config de serialização |
| `class DocumentStatus(str, Enum)` | Herança múltipla de mixin; idiomática em Python |

---

## Plano de execução

| Ordem | Item | Custo | Por quê nesta posição |
|---|---|---|---|
| 1º | `AbstractReport` → interface | baixo | Ganho claro e **destrava a migração do E5 para Python** |
| 2º | `ProfessorScheduleResponse` | trivial | Corrige um `equals` quebrado hoje |
| 3º | `@ManyToOne User` → `Long` | médio | **Remove ~42 FKs**; destrava a decomposição de finance e communication |
| 4º | `Person` → papéis (estratégia **C**) | baixo | É requisito de produto, não dívida técnica |
| 5º | `AuditInfo` / `SoftDelete` `@Embeddable` | médio | Ganho de design; pode aguardar |

Os itens 1 e 2 são mecânicos, sem risco, e verificáveis com a stack de pé (os 14 relatórios
devem continuar retornando 200). O item 3 é o de maior retorno arquitetural.

## Verificação

```bash
cd deploy && docker compose up -d
curl -fsS http://localhost:8080/actuator/health          # {"status":"UP"}
# smoke test: 14 de 14 relatorios em 200
```
