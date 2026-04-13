NAMESPACE   := event-flow-market
CLUSTER     := event-flow-market
PF_PID_DIR  := /tmp/efm-pf

# ── Port-Forward ──────────────────────────────────────────────────────────────

.PHONY: pf-up
pf-up: ## 관측성 도구 port-forward 시작 (Zipkin :9411, Prometheus :9090, Grafana :3000)
	@mkdir -p $(PF_PID_DIR)
	@$(MAKE) _pf-start SVC=zipkin     LOCAL=9411 REMOTE=9411
	@$(MAKE) _pf-start SVC=prometheus LOCAL=9090 REMOTE=9090
	@$(MAKE) _pf-start SVC=grafana    LOCAL=3000 REMOTE=3000
	@echo ""
	@echo "  Zipkin     → http://localhost:9411"
	@echo "  Prometheus → http://localhost:9090"
	@echo "  Grafana    → http://localhost:3000  (admin / admin)"

.PHONY: pf-down
pf-down: ## 모든 port-forward 종료
	@for f in $(PF_PID_DIR)/*.pid; do \
		[ -f "$$f" ] || continue; \
		pid=$$(cat $$f); \
		svc=$$(basename $$f .pid); \
		if kill -0 $$pid 2>/dev/null; then \
			kill $$pid && echo "stopped: $$svc (PID $$pid)"; \
		fi; \
		rm -f $$f; \
	done

.PHONY: pf-status
pf-status: ## port-forward 실행 상태 확인
	@echo "SERVICE       PID    STATUS"
	@echo "──────────────────────────"
	@for f in $(PF_PID_DIR)/*.pid; do \
		[ -f "$$f" ] || { echo "(실행 중인 port-forward 없음)"; break; }; \
		pid=$$(cat $$f); \
		svc=$$(basename $$f .pid); \
		if kill -0 $$pid 2>/dev/null; then \
			printf "%-14s%-7s%s\n" "$$svc" "$$pid" "running"; \
		else \
			printf "%-14s%-7s%s\n" "$$svc" "$$pid" "stopped"; \
		fi; \
	done

.PHONY: _pf-start
_pf-start:
	@pid_file=$(PF_PID_DIR)/$(SVC).pid; \
	if [ -f $$pid_file ] && kill -0 $$(cat $$pid_file) 2>/dev/null; then \
		echo "already running: $(SVC) (PID $$(cat $$pid_file))"; \
	else \
		kubectl port-forward svc/$(SVC) $(LOCAL):$(REMOTE) -n $(NAMESPACE) \
			> /tmp/efm-pf/$(SVC).log 2>&1 & \
		echo $$! > $$pid_file; \
		echo "started: $(SVC)  localhost:$(LOCAL)"; \
	fi

# ── Kind 클러스터 ─────────────────────────────────────────────────────────────

.PHONY: cluster-up
cluster-up: ## kind 클러스터 생성
	kind create cluster --name $(CLUSTER) --config k8s/kind-config.yaml

.PHONY: cluster-down
cluster-down: ## kind 클러스터 삭제
	kind delete cluster --name $(CLUSTER)

# ── 이미지 빌드 & 로드 ─────────────────────────────────────────────────────────

.PHONY: build
build: ## 전체 서비스 Docker 이미지 빌드
	docker compose build gateway member-service order-service stock-service

.PHONY: load
load: ## 빌드된 이미지를 kind 클러스터에 로드
	@for svc in gateway member-service order-service stock-service; do \
		docker tag event-flow-market-$$svc:latest event-flow-market/$$svc:latest; \
		kind load docker-image event-flow-market/$$svc:latest --name $(CLUSTER); \
		echo "loaded: $$svc"; \
	done

.PHONY: deploy
deploy: ## K8s 매니페스트 전체 적용
	kubectl apply -f k8s/namespace.yaml
	kubectl apply -f k8s/secret.yaml
	kubectl apply -f k8s/configmap.yaml
	kubectl apply -f k8s/observability.yaml
	kubectl apply -f k8s/member-service.yaml
	kubectl apply -f k8s/order-service.yaml
	kubectl apply -f k8s/stock-service.yaml
	kubectl apply -f k8s/gateway.yaml
	kubectl apply -f k8s/keda-scaledobject.yaml

.PHONY: rollout
rollout: build load ## 이미지 재빌드 후 Deployment rolling restart
	kubectl rollout restart deployment/gateway deployment/member-service \
		deployment/order-service deployment/stock-service -n $(NAMESPACE)
	kubectl rollout status deployment/gateway deployment/member-service \
		deployment/order-service deployment/stock-service -n $(NAMESPACE)

# ── 상태 확인 ─────────────────────────────────────────────────────────────────

.PHONY: status
status: ## 전체 Pod/Service 상태 확인
	kubectl get pods,svc -n $(NAMESPACE)

.PHONY: logs
logs: ## 서비스 로그 확인 (SVC=gateway|member-service|... 지정)
	kubectl logs -n $(NAMESPACE) -l app=$(SVC) --tail=100 -f

# ── Help ──────────────────────────────────────────────────────────────────────

.PHONY: help
help: ## 사용 가능한 명령 목록
	@grep -E '^[a-zA-Z_-]+:.*##' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*##"}; {printf "  %-14s %s\n", $$1, $$2}'

.DEFAULT_GOAL := help