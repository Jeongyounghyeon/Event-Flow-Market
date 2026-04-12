import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const orderSuccessCount = new Counter('order_success_count');
const orderFailCount    = new Counter('order_fail_count');
const sagaConfirmedRate = new Rate('saga_confirmed_rate');
const orderDuration     = new Trend('order_duration_ms', true);

export const options = {
  scenarios: {
    // 선착순 시나리오: 동시 100명이 재고 1개에 주문
    flash_sale: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 100 }, // 10초 동안 100명까지 증가
        { duration: '30s', target: 100 }, // 30초 유지
        { duration: '10s', target: 0   }, // 10초 동안 감소
      ],
    },
  },
  thresholds: {
    http_req_duration:  ['p(99)<2000'], // 99%ile 응답시간 2초 이하
    http_req_failed:    ['rate<0.05'],  // HTTP 에러율 5% 미만 (재고 소진은 201로 주문 생성 후 CANCELLED → 에러 아님)
    order_duration_ms:  ['p(95)<500'],  // 주문 생성 p95 500ms 이하
  },
};

const BASE_URL  = __ENV.BASE_URL  || 'http://localhost:8080';
const PRODUCT_ID = __ENV.PRODUCT_ID || '1';

// 테스트 전 1회 실행: 로그인하여 토큰 획득
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/members/login`,
    JSON.stringify({ email: __ENV.TEST_EMAIL || 'load@test.com', password: __ENV.TEST_PASSWORD || 'Test1234!' }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  check(loginRes, { 'login success': (r) => r.status === 200 });
  return { accessToken: loginRes.json('data.accessToken') };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.accessToken}`,
  };

  // 주문 생성
  const start = Date.now();
  const orderRes = http.post(
    `${BASE_URL}/api/orders`,
    JSON.stringify({ productId: Number(PRODUCT_ID), quantity: 1 }),
    { headers },
  );
  const orderDurationMs = Date.now() - start;
  orderDuration.add(orderDurationMs);

  const orderCreated = check(orderRes, {
    'order created (201)': (r) => r.status === 201,
  });

  if (!orderCreated) {
    orderFailCount.add(1);
    sleep(0.5);
    return;
  }

  orderSuccessCount.add(1);
  const orderId = orderRes.json('data.orderId');

  // Saga 완료 대기 (최대 3초 폴링)
  let confirmed = false;
  for (let i = 0; i < 6; i++) {
    sleep(0.5);
    const statusRes = http.get(`${BASE_URL}/api/orders/${orderId}`, { headers });
    const status = statusRes.json('data.status');
    if (status === 'CONFIRMED' || status === 'CANCELLED') {
      confirmed = status === 'CONFIRMED';
      break;
    }
  }

  sagaConfirmedRate.add(confirmed);
  sleep(1);
}