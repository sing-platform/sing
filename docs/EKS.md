# EKS no AWS Academy Learner Lab

Sobe o SING num cluster EKS pequeno e mostra a demo: pods, Services, balanceamento entre
réplicas do core, REST e RabbitMQ. Todos os comandos são para o **Git Bash**, no terminal
que tem as credenciais do lab.

**Apague tudo no final** (seção 9). Encerrar a sessão do lab para os EC2, mas o control plane,
o Load Balancer, os IPs públicos e os discos continuam cobrando. "End Lab" não zera a conta, e
o painel de crédito mostra o consumo com atraso.

## Sessão de 4 horas

A sessão do Learner Lab dura 4 horas. Quando ela acaba:

- os EC2 (os nós) param e a demo sai do ar;
- control plane, NLB, discos e IPs do NLB **continuam cobrando**, cerca de US$0,14/h sem nada funcionar;
- o `aws_session_token` expira: no próximo **Start Lab**, cole as credenciais novas antes de usar `aws` ou `kubectl`;
- ao religar os nós, os pods dos bancos voltam vazios (`emptyDir`) e o core refaz a carga inicial.

Por isso tudo cabe numa sessão só: criar (~30 min), validar (~20 min) e apagar (~20 min).
No dia da apresentação, clique em **Start Lab** cerca de 1h30 antes: o contador volta para 4h e
não acaba no meio da demo. Se a sessão acabar com o cluster criado, abra outra só para terminar
a limpeza.

## O que sobe

| Item | Escolha |
|---|---|
| Kubernetes | 1.36, política de suporte `STANDARD` (não cai no suporte estendido de US$0,60/h) |
| Cluster | modo padrão, sem Auto Mode, criado pela CLI |
| Nós | node group gerenciado, 3× `t3.medium` On-Demand, AL2023, disco de 20 GiB |
| Rede | VPC padrão, subnets públicas de us-east-1a, 1b e 1c, sem NAT e sem mudar rotas |
| Entrada | NGINX Ingress Controller da F5/NGINX (chart `nginx-ingress` 2.8.0) atrás de um NLB |
| Imagens | `ghcr.io/sing-platform/<servico>:<sha da main>`, públicas |
| Bancos | Postgres ×2, Mongo ×2, MySQL, RabbitMQ e MinIO em StatefulSets com `emptyDir` |

O controlador comunitário `ingress-nginx` foi aposentado em março de 2026 e não recebe mais
correção de segurança; por isso o Ingress usa o controlador da F5, que segue mantido. O
`k8s/ingress.yaml` traz as anotações dos dois, e cada controlador ignora as do outro.

O controlador da F5 tem quatro diferenças que já estão tratadas no repositório:

| Comportamento padrão da F5 | Ajuste |
|---|---|
| Recusa Ingress sem `host` (`spec.rules[0].host: Required value`) | `allowEmptyIngressHost: true` nos values: o SING é acessado pelo endereço do NLB, sem domínio |
| Service com `externalTrafficPolicy: Local`: só o nó do controlador passa no health check do NLB | `externalTrafficPolicy: Cluster` nos values: qualquer nó repassa ao controlador |
| Redireciona HTTP para HTTPS mesmo sem certificado | `nginx.org/ssl-redirect: "false"` no Ingress |
| Não grava o endereço do NLB no status do Ingress | o endereço é lido do Service `nginx-ingress-controller` |

## Custo

Preços sob demanda em us-east-1, conferidos em novembro de 2026.

| Recurso | Preço | Por hora |
|---|---|---|
| Control plane EKS (suporte padrão) | US$0,10/h | 0,100 |
| 3× EC2 `t3.medium` | US$0,0418/h cada | 0,125 |
| 3× EBS gp3 de 20 GiB | US$0,08/GB-mês | 0,007 |
| 5 IPv4 públicos (3 nós + 2 do NLB) | US$0,005/h cada | 0,025 |
| NLB | US$0,0225/h + ~US$0,006/NLCU-h | 0,029 |
| **Total** | | **≈ US$0,29/h** |

Teste de 3 h ≈ US$0,90. Deixar ligado por 40 h ≈ US$11,60. O mais barato é subir para testar,
apagar e subir de novo no dia da apresentação (≈ US$0,90 cada vez).

## 0. Pré-requisitos

- AWS CLI v2, `kubectl` e `helm` instalados.
- Lab iniciado e credenciais coladas em `~/.aws/credentials` (com `aws_session_token`).
- Região us-east-1.

```bash
aws sts get-caller-identity
aws eks list-clusters --region us-east-1
aws eks describe-cluster-versions --region us-east-1 --query "clusterVersions[?versionStatus=='STANDARD_SUPPORT'].clusterVersion"
```

Se `list-clusters` já mostrar um `sing`, não crie outro: siga do passo que faltar.

As roles e subnets abaixo são as da conta do lab usada no projeto. Em outra conta, troque
pelos valores dela (`aws iam list-roles --query "Roles[?contains(RoleName,'LabEks')].Arn"` e
`aws ec2 describe-subnets --filters Name=default-for-az,Values=true`). Nos values do
controlador (`deploy/eks/nginx-ingress-values.yaml`) as subnets do NLB também são dessa conta.

## 1. Imagens

O workflow **imagens** builda core, documents, communication e finance em `linux/amd64` e
publica com a tag `latest` e com o SHA do commit:

```bash
gh workflow run imagens --repo sing-platform/sing --ref main
gh run watch --repo sing-platform/sing
```

No GitHub, deixe os quatro pacotes **públicos** (org → Packages → pacote → Package settings →
Change visibility). Sem isso o nó recebe 403 e o pod fica em `ImagePullBackOff`.

`deploy/eks/kustomization.yaml` fixa as imagens no SHA `d6c15375…`. Se publicar de outro
commit, troque o `newTag` das quatro imagens.

## 2. Cluster

Sem `--compute-config`: o cluster sobe no modo padrão, não no Auto Mode.

```bash
aws eks create-cluster --region us-east-1 --name sing --kubernetes-version 1.36 \
  --role-arn arn:aws:iam::730335192519:role/c222296a5607975l18258519t1w730335-LabEksClusterRole-6YCj8cOuj23M \
  --resources-vpc-config subnetIds=subnet-02e136981a6c0f04c,subnet-0352c1463d6ddde74,subnet-0f516cbe6290922a2,endpointPublicAccess=true,endpointPrivateAccess=true \
  --access-config authenticationMode=API_AND_CONFIG_MAP,bootstrapClusterCreatorAdminPermissions=true \
  --upgrade-policy supportType=STANDARD
aws eks wait cluster-active --region us-east-1 --name sing
```

Leva de 10 a 15 minutos. Se der `AccessDenied` em `iam:PassRole`, pare: o lab não deixa usar
a role, e não há como contornar sem IAM novo.

## 3. Nós

```bash
aws eks create-nodegroup --region us-east-1 --cluster-name sing --nodegroup-name sing-nodes \
  --node-role arn:aws:iam::730335192519:role/c222296a5607975l18258519t1w730335192-LabEksNodeRole-AkX6QL6O0w3u \
  --subnets subnet-02e136981a6c0f04c subnet-0352c1463d6ddde74 subnet-0f516cbe6290922a2 \
  --instance-types t3.medium --ami-type AL2023_x86_64_STANDARD --capacity-type ON_DEMAND \
  --disk-size 20 --scaling-config minSize=3,maxSize=3,desiredSize=3
aws eks wait nodegroup-active --region us-east-1 --cluster-name sing --nodegroup-name sing-nodes
aws eks update-kubeconfig --region us-east-1 --name sing
kubectl get nodes -o wide
```

Três nós `Ready`.

## 4. NGINX Ingress Controller

```bash
helm install nginx-ingress oci://ghcr.io/nginx/charts/nginx-ingress --version 2.8.0 \
  -n nginx-ingress --create-namespace -f deploy/eks/nginx-ingress-values.yaml
kubectl get svc -n nginx-ingress -w
```

Espere a coluna `EXTERNAL-IP` mostrar o DNS do NLB. O NLB leva de 2 a 3 minutos para marcar
os nós como saudáveis.

## 5. Segredos e deploy

`scripts/eks-segredos.sh` gera `deploy/eks/secrets.local.yaml` com senhas e `JWT_SECRET`
aleatórios. O arquivo fica fora do git e o script não imprime os valores. O `JWT_SECRET` sai
com 56 caracteres hex (42 bytes): com esse tamanho o core assina em HS256, o único algoritmo
que o documents aceita.

```bash
./scripts/eks-segredos.sh
kubectl apply -k deploy/eks
kubectl get pods -n sing -w
```

O core demora (Flyway e carga inicial em cada pod). Tudo pronto:

```bash
kubectl wait --for=condition=ready pod --all -n sing --timeout=600s
```

Endereço público:

```bash
ELB=$(kubectl get svc -n nginx-ingress nginx-ingress-controller -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "$ELB"
curl -sI "http://${ELB}/actuator/health" | head -1
```

O DNS do NLB pode levar alguns minutos para resolver.

## 6. Validação

### Pods, Services e discovery

```bash
kubectl get pods,svc,ingress -n sing -o wide
kubectl run dns-teste -n sing --rm -it --restart=Never --image=busybox:1.36 -- \
  sh -c 'for s in core finance documents communication rabbitmq postgres-core mysql-finance; do nslookup $s.sing.svc.cluster.local | tail -2; done'
```

Todo Service é `ClusterIP`: bancos, RabbitMQ e MinIO não têm endereço externo. A única
entrada é o NLB do controlador NGINX.

### APIs e documentação

| Serviço | Swagger |
|---|---|
| core | `http://${ELB}/swagger-ui/index.html` |
| finance | `http://${ELB}/finance/swagger` |
| documents | `http://${ELB}/documents/docs` |
| communication | `http://${ELB}/comunicacao/docs` |

A documentação é pública; as rotas de negócio não:

```bash
curl -s -o /dev/null -w "sem token: %{http_code}\n" "http://${ELB}/api/v1/finance/entries"
```

Deve responder `401`.

### Réplicas e balanceamento

```bash
kubectl get pods -n sing -l app=core -o wide
for i in $(seq 1 9); do curl -s "http://${ELB}/api/v1/instancia"; echo; done
for i in $(seq 1 6); do curl -sI "http://${ELB}/actuator/health" | grep -i '^X-Pod'; done
```

Os nomes vêm do Downward API (`POD_NAME`) e são os mesmos de `kubectl get pods`. Repetir o
mesmo pod em sequência é normal: o balanceamento não é round-robin estrito.

### Matrícula: REST + RabbitMQ

Login de teste da carga inicial: `admin@sing.local` / `SING@123`.

```bash
TOKEN=$(curl -s -X POST "http://${ELB}/api/v1/auth/authenticate" -H 'Content-Type: application/json' \
  -d '{"email":"admin@sing.local","password":"SING@123"}' | python -c 'import json,sys; print(json.load(sys.stdin)["token"])')

SALA=$(curl -s "http://${ELB}/api/v1/schoolrooms" -H "Authorization: Bearer $TOKEN" \
  | python -c 'import json,sys; print(next(r["id"] for r in json.load(sys.stdin) if r["activeEnrollmentCount"]>0))')

ALUNO=$(curl -s "http://${ELB}/api/v1/schoolrooms/${SALA}/enrollments?size=1" -H "Authorization: Bearer $TOKEN" \
  | python -c 'import json,sys; print(json.load(sys.stdin)["content"][0]["studentId"])')

curl -s -o /dev/null -w "cancela: %{http_code}\n" -X DELETE \
  "http://${ELB}/api/v1/enrollments/student/${ALUNO}/room/${SALA}" -H "Authorization: Bearer $TOKEN"

curl -s -X POST "http://${ELB}/api/v1/enrollments" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d "{\"studentId\": ${ALUNO}, \"schoolRoomIds\": [${SALA}]}"
```

A resposta traz `matriculaId` e `eventId`. Troque os valores abaixo pelos impressos:

```bash
MATRICULA=1
EVENTO=00000000-0000-4000-8000-000000000000
curl -s "http://${ELB}/api/v1/enrollments/${MATRICULA}/integracoes" -H "Authorization: Bearer $TOKEN"
```

Com `publicacao` e `cobranca` em `concluida`:

- **Cobrança** (REST core → finance, MySQL):
  ```bash
  kubectl exec -n sing mysql-finance-0 -- sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" sing_finance -e "select id, matricula_id, valor_centavos, status from cobranca"'
  ```
- **Notificação** (RabbitMQ → communication, Mongo próprio):
  ```bash
  curl -s "http://${ELB}/api/v1/comunicacao/notificacoes/${EVENTO}"
  ```
- **Comprovante** (RabbitMQ → documents, Mongo próprio). Não há endpoint de leitura; a consulta é no pod:
  ```bash
  kubectl exec -n sing mongo-documents-0 -- sh -c "mongosh -u \"\$MONGO_INITDB_ROOT_USERNAME\" -p \"\$MONGO_INITDB_ROOT_PASSWORD\" --authenticationDatabase admin sing_documents_comprovantes --quiet --eval \"db.comprovantes.find({eventId: '${EVENTO}'}, {arquivoBase64: 0}).toArray()\""
  ```

Notificação e comprovante saem da mesma mensagem na exchange `sing.events`, cada um na sua fila.

### Isolamento dos bancos

Cada serviço tem banco, Service e credencial próprios: core → `postgres-core`, documents →
`postgres-documents` + `mongo-documents` + MinIO, communication → `mongo-communication`,
finance → `mysql-finance`. Quando um precisa de dado do outro, chama a API (finance pede o
aluno ao core por REST) ou consome evento.

## 7. Roteiro de 5 minutos

1. `kubectl get nodes,pods,svc,ingress -n sing -o wide`: 3 nós, 3 réplicas do core em nós diferentes, Services sem IP externo.
2. Loop de `/api/v1/instancia`: nomes de pod diferentes respondendo.
3. Um Swagger público e o `401` sem token.
4. Matrícula pelo `curl` e `integracoes` em `concluida`.
5. Cobrança no MySQL, notificação pela API e comprovante no Mongo: um REST e dois consumidores RabbitMQ.

## 8. Limitações

- **Persistência:** os bancos usam `emptyDir`. Se um pod de banco reinicia ou muda de nó, os dados somem. Um volume EBS exigiria o driver EBS CSI com role IAM própria, que o lab não permite criar.
- **Login de teste:** a senha da carga inicial (`SING@123`) é pública. Enquanto o NLB existir, qualquer um entra como admin; subir só perto da apresentação reduz a janela.
- **NLB:** cada subnet listada nos values ganha um IP público cobrado.

## 9. Limpeza

Nesta ordem: o `helm uninstall` remove o NLB antes do cluster, para ele não ficar órfão.

```bash
kubectl delete -k deploy/eks
helm uninstall nginx-ingress -n nginx-ingress
aws elbv2 describe-load-balancers --region us-east-1 --query "LoadBalancers[].LoadBalancerName"
aws eks delete-nodegroup --region us-east-1 --cluster-name sing --nodegroup-name sing-nodes
aws eks wait nodegroup-deleted --region us-east-1 --cluster-name sing --nodegroup-name sing-nodes
aws eks delete-cluster --region us-east-1 --name sing
aws eks wait cluster-deleted --region us-east-1 --name sing
```

Depois confira que não sobrou nada cobrando:

```bash
aws ec2 describe-volumes --region us-east-1 --filters Name=status,Values=available --query "Volumes[].VolumeId"
aws ec2 describe-security-groups --region us-east-1 --filters Name=tag-key,Values=kubernetes.io/cluster/sing --query "SecurityGroups[].GroupId"
aws ec2 describe-addresses --region us-east-1 --query "Addresses[].PublicIp"
```

Só então **End Lab**. Se o lab expirar antes, apague pelo console na mesma ordem.
