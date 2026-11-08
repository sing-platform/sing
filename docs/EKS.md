# EKS no AWS Academy Learner Lab

Sobe o SING num cluster pequeno (2 nós `t3.medium`) e mostra a demo da matrícula.
O passo a passo cabe numa sessão do lab. **Apague o cluster no final.** Nó e
Load Balancer ligados continuam gastando crédito mesmo depois da apresentação.

O Keycloak fica de fora: o login da demo usa o JWT do próprio core (`JWT_SECRET`).
O finance (Matheus, .NET + MySQL) também fica de fora. O manifesto comentado está
em `k8s/finance.yaml`. Sem ele a cobrança fica `pendente`; a matrícula, o
comprovante e a notificação não dependem disso.

## 0. Imagens no GHCR

Os Deployments usam `ghcr.io/sing-platform/core`, `documents` e `communication`,
tag `latest`, arquitetura **linux/amd64**.

Na sua máquina, com Docker:

```bash
export GHCR_USER=seu-usuario-github
export GHCR_TOKEN=seu-token-com-write-packages
./scripts/build-push.sh
```

Depois que esta branch estiver na `main`, o mesmo script roda em
Actions → **imagens** → Run workflow.

No pacote do GHCR, deixe a visibilidade **pública** para o nó puxar sem senha.
Se ficar privado, crie o secret no cluster (depois do `apply` do namespace) e
coloque `imagePullSecrets` nos três Deployments:

```bash
kubectl create secret docker-registry ghcr -n sing \
  --docker-server=ghcr.io \
  --docker-username="$GHCR_USER" \
  --docker-password="$GHCR_TOKEN"
```

```yaml
imagePullSecrets:
  - name: ghcr
```

## 1. Start Lab

Abra o Learner Lab e clique em **Start Lab**. Espere o sinal verde.
Em **AWS Details**, copie a credencial de CLI (access key, secret e session token).

```bash
aws configure
# região: us-east-1
# cole também o aws_session_token no arquivo ~/.aws/credentials
```

Confira:

```bash
aws sts get-caller-identity
```

A região da demo é **us-east-1**. No canto do console, troque para `US East (N. Virginia)`.

## 2. Cluster

EKS → **Create cluster**.

| Campo | Valor |
|---|---|
| Name | `sing` |
| Kubernetes version | a padrão do console |
| Cluster service role | `LabEksClusterRole` |

Networking:

- VPC **default** da conta do lab
- Subnets **públicas** (pelo menos duas, em zonas diferentes)
- Cluster endpoint: **Public**
- Não crie role nova. O lab não deixa.

Create e espere o status **Active** (uns 10 minutos).

## 3. Node group

Dentro do cluster, **Compute** → **Add node group**.

| Campo | Valor |
|---|---|
| Name | `sing-nodes` |
| Node IAM role | `LabEksNodeRole` |
| AMI | Amazon Linux, a padrão |
| Capacity | On-Demand |
| Instance type | `t3.medium` |
| Desired / Min / Max | `2` / `2` / `2` |
| Disk | 20 GiB |

Sem SSH key. Sem escalar além de 2: cada nó a mais gasta crédito.

Espere os dois nós **Ready**:

```bash
aws eks update-kubeconfig --region us-east-1 --name sing
kubectl get nodes
```

## 4. Ingress NGINX

O acesso de fora é por este Ingress. Ele abre um Load Balancer na AWS.

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/aws/deploy.yaml
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=180s
```

## 5. Aplicar o SING

As senhas estão em `k8s/secrets.example.yaml` e são as mesmas de
`deploy/.env.example` (`trocar-esta-senha`, etc.). Servem só para o lab.
Não commite senha real. Se for trocar, copie para `k8s/secret.yaml`
(esse nome está no `.gitignore`), ajuste o `kustomization.yaml` na sua máquina
e não faça push desse arquivo.

```bash
kubectl apply -k k8s/
kubectl get pods -n sing
kubectl wait --for=condition=ready pod -l app=core -n sing --timeout=360s
kubectl wait --for=condition=ready pod -l app=documents -n sing --timeout=180s
kubectl wait --for=condition=ready pod -l app=communication -n sing --timeout=180s
```

O core demora porque roda o Flyway e a carga inicial. Três pods `core` têm que
ficar `Running`. Os bancos (`postgres-core`, `postgres-documents`,
`mongo-documents`, `mongo-communication`), o `rabbitmq` e o `minio` também.
O MinIO usa `bitnamilegacy/minio:2024`: a imagem `minio/minio` saiu do Docker Hub.

Os volumes são `emptyDir`. No Learner Lab o driver de disco (EBS CSI) em geral
não está instalado, e um PVC ficaria `Pending` para sempre. Se o pod do banco
morrer, o dado da demo some. Para a apresentação isso basta.

O endereço externo:

```bash
kubectl get ingress -n sing
```

Espere a coluna ADDRESS sair de vazia. É o DNS do Load Balancer.

```bash
ELB=$(kubectl get ingress -n sing sing -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "$ELB"
curl -sI "http://${ELB}/actuator/health" | head
```

O health tem que responder 200.

As UIs de documentação usam os mesmos paths do Traefik. Troque o host pelo do Load Balancer:

| Serviço | UI | Schema |
|---|---|---|
| core | `http://${ELB}/swagger-ui/index.html` | `http://${ELB}/v3/api-docs` |
| finance | `http://${ELB}/finance/swagger` | `http://${ELB}/finance/swagger/v1/swagger.json` |
| documents | `http://${ELB}/documents/docs` | `http://${ELB}/documents/openapi.json` |
| communication | `http://${ELB}/comunicacao/docs` | `http://${ELB}/comunicacao/openapi.json` |

A página é pública. Try it out chama o Ingress. Core, documents e `/api/v1/finance` seguem com JWT. O finance no cluster ainda depende do Deployment dele (o placeholder em `k8s/finance.yaml`).

## 6. Demo de 5 minutos

### O que está no ar

```bash
kubectl get pods,svc,ingress -n sing
kubectl get pods -n sing -l app=core -o wide
```

Três pods do core, Services com DNS interno (`core`, `documents`,
`communication`, `rabbitmq`, `postgres-core`, …) e o Ingress na frente.
O documents acha o core em `http://core:8080`. O core acha o Rabbit em
`rabbitmq`. Isso é o Service Discovery.

### Balanceamento

O Deployment do core já pede 3 réplicas. Se estiver com menos:

```bash
kubectl scale deployment/core -n sing --replicas=3
kubectl rollout status deployment/core -n sing
```

Seis chamadas. O header `X-Pod` e o JSON dizem qual pod respondeu.
O Service não garante round-robin: duas chamadas seguidas podem cair no mesmo pod.

```bash
for i in 1 2 3 4 5 6; do
  curl -sD - "http://${ELB}/api/v1/instancia" | grep -E -i '^(X-Pod|HTTP)|"instancia"'
  echo
done
```

`GET /api/v1/instancia` é público e só devolve `servico` e `instancia` (o nome do pod).
Não traz IP, env nem segredo. No compose existe uma instância só: o valor é o
hostname daquele container, e isso não prova três réplicas.

### Matrícula (REST + RabbitMQ)

A semente já matricula os alunos. A demo cancela um vínculo e cria de novo,
para nascer um `matricula.criada` novo. Login da semente: `admin@sing.local` /
`SING@123`.

```bash
TOKEN=$(curl -s -X POST "http://${ELB}/api/v1/auth/authenticate" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@sing.local","password":"SING@123"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')

# A listagem /api/v1/students com filtro vazio quebra no PostgreSQL
# (o banco não infere o tipo do parâmetro nulo). A turma devolve o aluno.
SALA=$(curl -s "http://${ELB}/api/v1/schoolrooms" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import json,sys; rooms=json.load(sys.stdin); print(next(r["id"] for r in rooms if r["activeEnrollmentCount"]>0))')

ALUNO=$(curl -s "http://${ELB}/api/v1/schoolrooms/${SALA}/enrollments?size=1" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["content"][0]["studentId"])')

curl -s -o /dev/null -w "delete %{http_code}\n" \
  -X DELETE "http://${ELB}/api/v1/enrollments/student/${ALUNO}/room/${SALA}" \
  -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://${ELB}/api/v1/enrollments" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"studentId\": ${ALUNO}, \"schoolRoomIds\": [${SALA}]}"
```

A resposta 201 traz `matriculaId` e `eventId`. A publicação no Rabbit é logo
depois. A cobrança continua `pendente` (não há finance).

```bash
# troque pelos valores impressos acima
MATRICULA=1
EVENTO=00000000-0000-4000-8000-000000000000

curl -s "http://${ELB}/api/v1/enrollments/${MATRICULA}/integracoes" \
  -H "Authorization: Bearer $TOKEN"
```

Quando `publicacao` for `concluida`, os dois consumidores já podem ter gravado.

Notificação (communication, pela Ingress, path `/api/v1/comunicacao`):

```bash
curl -s "http://${ELB}/api/v1/comunicacao/notificacoes/${EVENTO}"
```

Comprovante (documents). Esse recorte não tem GET: ele só consome a fila e
grava no Mongo dele. A consulta é no pod:

```bash
kubectl exec -n sing mongo-documents-0 -- \
  mongosh -u documents -p 'trocar-esta-senha' --authenticationDatabase admin \
  sing_documents_comprovantes --quiet \
  --eval "db.comprovantes.find({eventId: '${EVENTO}'}, {arquivoBase64: 0}).toArray()"
```

Tem que aparecer um documento `tipo: comprovante` com o `nomeArquivo`
`comprovante-matricula-<id>.pdf`. A notificação e o comprovante saem da mesma
mensagem na exchange `sing.events`, cada um na sua fila.

Fila, se quiser mostrar na UI (não fica aberta na internet):

```bash
kubectl port-forward -n sing svc/rabbitmq 15672:15672
```

Usuário `sing`, senha `trocar-esta-senha`. Exchange `sing.events`.

## 7. Derrubar tudo

Faça isso **antes** de End Lab. A ordem importa: o Load Balancer some junto
com o Service do ingress. Se apagar o cluster primeiro, o balanceador pode
ficar órfão e continuar na conta.

```bash
kubectl delete -k k8s/
kubectl delete namespace ingress-nginx
```

No console EC2 → Load Balancers, espere a lista ficar vazia.
No EKS:

1. Apague o node group `sing-nodes` e espere sumir.
2. Apague o cluster `sing` e espere sumir.
3. EC2 → Volumes: apague volume `available` que tenha sobrado.
4. Só então **End Lab**.

Se o `kubectl delete` falhar porque o lab já expirou, apague o Load Balancer,
o node group e o cluster pelo console mesmo assim.
