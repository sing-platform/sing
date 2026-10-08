# Analytics — painel gerencial do SING

Complementa a especificação (RF21, US016, UC016) com o que a rubrica de TCC 1, Sprint 2, pede para *analytics*: objetivo do painel, dados coletados para séries temporais e protótipo com filtros de período.

## Objetivo

Dar ao administrador da escola uma visão diária da saúde da organização, para decidir três coisas:

| Decisão | Indicador que a sustenta |
|---|---|
| Abrir, juntar ou fechar turmas | Ocupação por turma e oficina; tamanho da lista de espera |
| Agir antes que o aluno abandone | Frequência média e alunos abaixo da frequência mínima |
| Cobrar e planejar o caixa | Inadimplência do mês e valor a receber (com o módulo Financeiro ativo) |

Todo indicador considera somente a organização do usuário (RF3) e tem no máximo 1 dia de defasagem.

## Indicadores e gráficos

| Elemento | Tipo | Cálculo |
|---|---|---|
| Alunos ativos | cartão + variação no período | alunos com ao menos uma matrícula ativa no fim do período |
| Ocupação das turmas | cartão | matrículas ativas ÷ capacidade somada das turmas ativas |
| Frequência média | cartão + linha mensal | (presenças + faltas justificadas) ÷ aulas dadas |
| Alunos em risco | cartão + lista | alunos com frequência no período abaixo da mínima configurada (RF7) |
| Matrículas novas e cancelamentos | barras agrupadas por mês | contagem por `enrollment_date` e por `ended_at` |
| Ocupação por oficina | barras horizontais | ocupação agrupada por oficina, destacando as lotadas |
| Lista de espera | cartão + tempo médio | interessados aguardando; dias entre inclusão e matrícula |
| Inadimplência | cartão + linha mensal | valor vencido e não pago ÷ valor vencido no mês |

## Dados coletados (extração)

Séries temporais montadas a partir das tabelas do modelo de dados (seção 12 da especificação), sempre com a data do fato:

| Fonte | Campos de tempo | Campos de medida |
|---|---|---|
| `enrollment` | `enrollment_date`, `ended_at` | `status`, `schoolroom_id`, `student_id` |
| `lecture` + `attendance` | `lecture_date` | `status` (presente, falta, justificada) |
| `schoolroom` | — | `capacity`, `workshop_id`, `unit_id` |
| `waiting_list_entry` | `summoned_at`, `expires_at`, data de inclusão | `status`, `position` |
| `charge` | `due_date`, `paid_at` | `amount`, `status` |

**Transformação:** agregação diária por organização, unidade, oficina e turma, gravada em visões materializadas com `tenant_id` e a mesma política de segurança em nível de linha das tabelas de origem.
**Carga:** atualização diária de madrugada; o painel lê somente as visões, sem consultar as tabelas transacionais.

## Leitura das séries

- **Tendências cíclicas:** picos de matrícula no início de cada semestre; queda de frequência em julho e dezembro.
- **Projeção:** média móvel de 3 meses para ocupação e frequência, exibida como linha tracejada à frente do último mês.
- **Comparação:** cada cartão mostra a variação contra o período anterior de mesmo tamanho.

## Filtros

- **Período:** semana, mês, trimestre, semestre, ano ou intervalo personalizado de até 24 meses (padrão: mês corrente).
- **Unidade** e **oficina/turma.**
- **Exportar:** CSV com os dados do recorte exibido.

## Protótipo

![Painel gerencial](prototipos/P016.png)

Protótipo P016: filtros de período e unidade no topo, quatro cartões de indicador e três gráficos (frequência média por mês, matrículas novas e cancelamentos, ocupação por oficina).
