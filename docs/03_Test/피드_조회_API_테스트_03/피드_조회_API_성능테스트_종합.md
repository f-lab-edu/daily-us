# 피드 조회 API 성능 테스트

## 1. 개요

피드 조회 API의 `fan-out 적용 후` 부하 테스트 결과를 `fan-out 적용 전` 결과와 비교

- 비교 기준 문서: [`../피드_조회_API_테스트_01/피드_조회_API_성능테스트_종합.md`](../피드_조회_API_테스트_01/피드_조회_API_성능테스트_종합.md)

## 2. 테스트 대상

- API: `GET /api/v1/posts`
- 인증 API: `POST /api/v1/auth/signin`
- 부하 도구: `nGrinder`
- 모니터링: `Grafana`, `Prometheus`
- 실행 시간: 각 시나리오당 약 `1분`

## 3. 비교 범위

- 동시 사용자 수: `50`, `148`, `1000`
- 시나리오: `NORMAL`, `HEAVY`, `MIXED`

## 4. 테스트 시나리오

### 4.1 NORMAL

- 일반 사용자만으로 부하 발생

### 4.2 HEAVY

- 팔로우 수가 많은 고비용 사용자만으로 부하 발생

### 4.3 MIXED

- 일반 사용자 `80%` + 고비용 사용자 `20%`

## 5. nGrinder 결과 비교

| vUsers | 시나리오 | 적용 전 TPS | 적용 후 TPS | 변화율 | 적용 전 Mean | 적용 후 Mean | 변화율 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 50 | NORMAL | 1,484.9 | 546.6 | -63.2% | 33.76ms | 93.95ms | +178.3% |
| 50 | HEAVY | 63.0 | 567.0 | +800.0% | 791.79ms | 89.35ms | -88.7% |
| 50 | MIXED | 732.8 | 554.7 | -24.3% | 68.44ms | 91.44ms | +33.6% |
| 148 | NORMAL | 1,819.4 | 562.6 | -69.1% | 79.40ms | 268.27ms | +237.9% |
| 148 | HEAVY | 63.7 | 609.5 | +856.8% | 2,273.01ms | 245.96ms | -89.2% |
| 148 | MIXED | 463.5 | 603.1 | +30.1% | 318.79ms | 250.15ms | -21.5% |
| 1000 | NORMAL | 1,731.5 | 409.5 | -76.3% | 516.34ms | 1,758.22ms | +240.5% |
| 1000 | HEAVY | 86.0 | 433.6 | +404.2% | 9,047.01ms | 1,838.24ms | -79.7% |
| 1000 | MIXED | 321.6 | 415.2 | +29.1% | 2,821.46ms | 1,824.66ms | -35.3% |

## 6. 결과 분석

### 6.1 HEAVY 유저

기존에는 `HEAVY` 사용자가 포함될 때 낮은 TPS와 평균 응답 시간이 초 단위까지 증가

fan-out 적용 후 

- `vUsers 50`: TPS `63.0 -> 567.0`, Mean `791.79ms -> 89.35ms`
- `vUsers 148`: TPS `63.7 -> 609.5`, Mean `2,273.01ms -> 245.96ms`
- `vUsers 1000`: TPS `86.0 -> 433.6`, Mean `9,047.01ms -> 1,838.24ms`

### 6.2 MIXED 유저

기존 구조에서는 고비용 사용자 포함 시 전체 처리량 저하

fan-out 적용 후

- `vUsers 50`: TPS `732.8 -> 554.7`, Mean `68.44ms -> 91.44ms`
- `vUsers 148`: TPS `463.5 -> 603.1`, Mean `318.79ms -> 250.15ms`
- `vUsers 1000`: TPS `321.6 -> 415.2`, Mean `2,821.46ms -> 1,824.66ms`

### 6.3 NORMAL 유저

`NORMAL` 시나리오는 적용 전보다 TPS가 감소 및 평균 응답 시간 증가

- `vUsers 50`: TPS `1,484.9 -> 546.6`, Mean `33.76ms -> 93.95ms`
- `vUsers 148`: TPS `1,819.4 -> 562.6`, Mean `79.40ms -> 268.27ms`
- `vUsers 1000`: TPS `1,731.5 -> 409.5`, Mean `516.34ms -> 1,758.22ms`

- 변경 후 레디스에서 게시물 ID 조회 후 게시물 정보 조회를 하기 때문에 TPS 및 평균 응답 시간이 증가 

## 7. 결론

### 개선 효과
- `HEAVY` 및 `MIXED` 유저 성능 개선
- 사용자 유형에 따른 편차가 감소

### 성능 저하
- `NORMAL` 유저의 TPS는 기존보다 저하

## 8. 첨부 자료

### nGrinder 결과

- [result/ngrinder_vUsers50_normal.jpg](result/ngrinder_vUsers50_normal.jpg)
- [result/ngrinder_vUsers50_heavy.jpg](result/ngrinder_vUsers50_heavy.jpg)
- [result/ngrinder_vUsers50_mix.jpg](result/ngrinder_vUsers50_mix.jpg)
- [result/ngrinder_vUsers148_normal.jpg](result/ngrinder_vUsers148_normal.jpg)
- [result/ngrinder_vUsers148_heavy.jpg](result/ngrinder_vUsers148_heavy.jpg)
- [result/ngrinder_vUsers148_mix.jpg](result/ngrinder_vUsers148_mix.jpg)
- [result/ngrinder_vUsers1000_normal.jpg](result/ngrinder_vUsers1000_normal.jpg)
- [result/ngrinder_vUsers1000_heavy.jpg](result/ngrinder_vUsers1000_heavy.jpg)
- [result/ngrinder_vUsers1000_mix.jpg](result/ngrinder_vUsers1000_mix.jpg)

### 모니터링 캡처

- [result/monitor_vUsers50.jpg](result/monitor_vUsers50.jpg)
- [result/monitor_vUsers148.jpg](result/monitor_vUsers148.jpg)
- [result/monitor_vUsers1000.png](result/monitor_vUsers1000.png)
