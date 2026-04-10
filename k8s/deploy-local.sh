#!/usr/bin/env bash
set -e

CLUSTER_NAME="event-flow-market"
NAMESPACE="event-flow-market"

echo "=== [1/6] kind 클러스터 확인 ==="
if ! kind get clusters | grep -q "$CLUSTER_NAME"; then
  kind create cluster --name "$CLUSTER_NAME"
else
  echo "클러스터 '$CLUSTER_NAME' 이미 존재합니다."
fi
kubectl cluster-info --context "kind-$CLUSTER_NAME"

echo ""
echo "=== [2/6] Namespace 및 공통 리소스 생성 ==="
kubectl apply -f k8s/namespace.yaml

# DB 초기화 ConfigMap — schema.sql을 단일 소스로 사용
kubectl create configmap member-db-init \
  --from-file=schema.sql=member-service/src/main/resources/db/schema.sql \
  -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap order-db-init \
  --from-file=schema.sql=order-service/src/main/resources/db/schema.sql \
  -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

kubectl create configmap stock-db-init \
  --from-file=schema.sql=stock-service/src/main/resources/db/schema.sql \
  -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

echo ""
echo "=== [3/6] Helm 인프라 설치 ==="
helm repo add bitnami https://charts.bitnami.com/bitnami 2>/dev/null || true
helm repo update

# PostgreSQL x3
for svc in "member member member_db" "order order order_db" "stock stock stock_db"; do
  read -r name user db <<< "$svc"
  if ! helm status "${name}-db" -n "$NAMESPACE" &>/dev/null; then
    helm install "${name}-db" bitnami/postgresql \
      -n "$NAMESPACE" \
      --set auth.username="$user" \
      --set auth.password="${user}1234" \
      --set auth.database="$db" \
      --set primary.initdb.scriptsConfigMap="${name}-db-init" \
      --wait --timeout 3m
  else
    echo "${name}-db 이미 설치됨, 건너뜁니다."
  fi
done

# Redis
if ! helm status redis -n "$NAMESPACE" &>/dev/null; then
  helm install redis bitnami/redis \
    -n "$NAMESPACE" \
    --set auth.enabled=false \
    --set architecture=standalone \
    --wait --timeout 3m
else
  echo "redis 이미 설치됨, 건너뜁니다."
fi

# Kafka
if ! helm status kafka -n "$NAMESPACE" &>/dev/null; then
  helm install kafka bitnami/kafka \
    -n "$NAMESPACE" \
    --set controller.replicaCount=1 \
    --set listeners.client.protocol=PLAINTEXT \
    --set listeners.controller.protocol=PLAINTEXT \
    --wait --timeout 5m
else
  echo "kafka 이미 설치됨, 건너뜁니다."
fi

echo ""
echo "=== [4/6] Docker 이미지 빌드 ==="
docker build -f gateway/Dockerfile        -t event-flow-market/gateway:latest        .
docker build -f member-service/Dockerfile -t event-flow-market/member-service:latest .
docker build -f order-service/Dockerfile  -t event-flow-market/order-service:latest  .
docker build -f stock-service/Dockerfile  -t event-flow-market/stock-service:latest  .

echo ""
echo "=== [5/6] kind에 이미지 로드 ==="
kind load docker-image event-flow-market/gateway:latest        --name "$CLUSTER_NAME"
kind load docker-image event-flow-market/member-service:latest --name "$CLUSTER_NAME"
kind load docker-image event-flow-market/order-service:latest  --name "$CLUSTER_NAME"
kind load docker-image event-flow-market/stock-service:latest  --name "$CLUSTER_NAME"

echo ""
echo "=== [6/6] 앱 서비스 배포 ==="
kubectl apply -f k8s/member-service.yaml
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/stock-service.yaml
kubectl apply -f k8s/gateway.yaml

echo ""
echo "=== 배포 완료! 파드 상태 확인 중... ==="
kubectl rollout status deployment/member-service -n "$NAMESPACE" --timeout=3m
kubectl rollout status deployment/order-service  -n "$NAMESPACE" --timeout=3m
kubectl rollout status deployment/stock-service  -n "$NAMESPACE" --timeout=3m
kubectl rollout status deployment/gateway        -n "$NAMESPACE" --timeout=3m

echo ""
echo "=== 전체 파드 상태 ==="
kubectl get pods -n "$NAMESPACE"

echo ""
echo "=== gateway 포트 포워딩 (백그라운드) ==="
kubectl port-forward svc/gateway 8080:80 -n "$NAMESPACE" &
echo "포트 포워딩 PID: $!"
echo ""
echo "테스트: curl http://localhost:8080/actuator/health"