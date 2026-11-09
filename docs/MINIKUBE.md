# SING no minikube

Kubernetes na sua máquina, com os mesmos manifests do EKS (`k8s/`). Serve para ensaiar
a demo de pods, balanceamento e elasticidade sem gastar crédito do Learner Lab.

O overlay `deploy/minikube/` reaproveita a base inteira e muda só duas coisas:

| | EKS (`k8s/`) | minikube (`deploy/minikube/`) |
|---|---|---|
| Réplicas do core | 3 fixas | HPA entre 2 e 3 por CPU |
| Imagens | puxadas do GHCR | buildadas dentro do minikube a partir do código local |
| Entrada | NGINX Ingress + Load Balancer da AWS (ELB) | addon `ingress` (mesmo NGINX) + `minikube tunnel` em `127.0.0.1` |
| Nós | 2 `t3.medium` | 1 |

## 0. Memória

O minikube roda dentro da VM do Docker Desktop. A stack pede perto de 2 GB de reserva
com 2 réplicas do core, e cada réplica extra pede mais 384 Mi.

- Pare a stack do compose antes: `docker compose -p sgs-sing-ms stop` (os volumes ficam).
- Com 4 GB no Docker Desktop, use `--memory=3500mb`. Com 6 GB ou mais
  (Settings → Resources), use `--memory=5g` e o HPA chega às 3 réplicas com folga.

## 1. Subir o cluster

```bash
minikube start --driver=docker --cpus=4 --memory=3500mb
minikube addons enable ingress
minikube addons enable metrics-server
```

## 2. Imagens

Builda core, documents, communication e finance dentro do minikube, com o mesmo nome que
os Deployments usam (`ghcr.io/sing-platform/<servico>:latest`). Como os Deployments têm
`imagePullPolicy: IfNotPresent`, o cluster usa a imagem local e não vai ao GHCR.

```bash
./scripts/minikube-imagens.sh
```

Para rebuildar um serviço só: `./scripts/minikube-imagens.sh core`.
Depois de rebuildar, `kubectl rollout restart deployment/core -n sing`.

## 3. Aplicar

```bash
kubectl apply -k deploy/minikube
kubectl wait --for=condition=ready pod -l app=core -n sing --timeout=420s
kubectl get pods -n sing
```

O core demora: cada pod roda o Flyway e a carga inicial ao subir.

## 4. Acesso

Num terminal separado, deixe rodando (no Windows ele pede para continuar aberto):

```bash
minikube tunnel
```

O gateway fica em `http://127.0.0.1`. Mesmos caminhos do compose e do EKS:

| Serviço | Swagger |
|---|---|
| core | http://127.0.0.1/swagger-ui/index.html |
| finance | http://127.0.0.1/finance/swagger |
| documents | http://127.0.0.1/documents/docs |
| communication | http://127.0.0.1/comunicacao/docs |

Login de teste: `admin@sing.local` / `SING@123`.

## 5. Demo

### Pods e self-healing

```bash
kubectl get pods -n sing -o wide
kubectl delete pod -n sing -l app=core --wait=false
kubectl get pods -n sing -l app=core -w
```

O Deployment recria o pod apagado sozinho.

### Balanceamento

O header `X-Pod` traz o nome do pod que respondeu:

```bash
for i in 1 2 3 4 5 6; do curl -sI "http://127.0.0.1/actuator/health" | grep -i '^X-Pod'; done
```

A distribuição não é alternada à risca; o mesmo pod pode aparecer duas vezes seguidas.

### Elasticidade (HPA)

O overlay já cria o HPA do core: mínimo 2, máximo 3, alvo de 60% de CPU sobre o request
de 100m. Ele só sobe com CPU alta por 1 minuto e só desce depois de 2 minutos calmos,
para não reagir ao pico de CPU de cada JVM que sobe. Em dois terminais:

```bash
kubectl get hpa -n sing -w
```

```bash
for i in $(seq 1 30000); do curl -s -o /dev/null http://127.0.0.1/api/v1/instancia; done
```

Com a carga, a coluna `REPLICAS` vai de 2 para 3. Ao parar, volta para 2 depois de cerca de 2 minutos.
Pod novo leva de 1 a 3 minutos para ficar pronto; o HPA ignora a CPU da subida da JVM
nos primeiros minutos de cada pod.

Escala manual também funciona, mas o HPA corrige de volta para a faixa dele:

```bash
kubectl scale deployment/core -n sing --replicas=3
```

O minikube não cria nós: a elasticidade aqui é de pods. Se faltar memória, o pod novo fica
`Pending`, que é o limite de capacidade do único nó.

## 6. Derrubar

```bash
kubectl delete -k deploy/minikube
minikube stop
```

`minikube delete` apaga o cluster e as imagens buildadas nele. Os bancos usam `emptyDir`,
então os dados somem quando o pod morre, como no EKS.
