# 피드 조회 API 성능 테스트 종합 보고서

## 1. 개요

본 문서는 피드 조회 API에 대해 다중 사용자 환경에서 수행한 성능 테스트 결과를 정리한다.
테스트는 `nGrinder`를 이용해 부하를 발생시켰고, 시스템 상태 및 응답 시간 추이는 `Grafana + Prometheus`로 모니터링했다.

이번 테스트는 현재 피드 조회 구조가 사용자 유형과 동시 접속자 수 증가에 따라 어떤 성능 저하를 보이는지 확인한다.

## 2. 테스트 대상

- API: `GET /api/v1/posts?page=0&size=10`
- 인증 API: `POST /api/v1/auth/signin`
- 부하 스크립트: `script/login_and_get_feed.groovy`

### 사용자군

- `NORMAL`: 일반 사용자 4명
- `HEAVY`: 팔로우 수가 많은 고비용 사용자 2명
- `MIXED`: 일반 사용자 80% + 고비용 사용자 20%

## 3. 테스트 환경

- 부하 도구: `nGrinder`
- 모니터링: `Grafana`, `Prometheus`
- 실행 시간: 각 시나리오당 약 `1분`
- 동시 사용자 수: `10`, `30`, `50`, `148`, `1000`

기존 문서인 [`피드_조회_API_성능테스트_싱글.md`](docs/03_Test/피드_조회_API_테스트_01/피드_조회_API_성능테스트_싱글.md)에서 확인한 바와 같이, 고비용 사용자는 팔로우 관계 집합과 게시물 후보군이 매우 커서 조회 시점에 큰 비용이 발생한다.  
이번 테스트는 그 비용이 실제 부하 상황에서 어떻게 증폭되는지를 확인한다.

## 4. 테스트 시나리오

사용자는 본인과 관계된 피드를 조회한다.

### 4.1 NORMAL

- 일반 사용자만으로 부하 발생
- 상대적으로 작은 팔로우 집합 기준 조회 성능 확인

### 4.2 HEAVY

- 팔로우 수가 많은 사용자만으로 부하 발생
- 최악 조건에서 현재 구조의 한계 확인

### 4.3 MIXED

- 일반 사용자 80%, 고비용 사용자 20%로 가정

## 5. nGrinder 결과 요약

| vUsers | 시나리오 | TPS | Peak TPS | Mean Test Time |
| --- | --- | ---: | ---: | ---: |
| 10 | NORMAL | 250.4 | 268.5 | 39.96ms |
| 10 | HEAVY | 61.8 | 78.0 | 162.11ms |
| 10 | MIXED | 248.3 | 310.0 | 40.30ms |
| 30 | NORMAL | 940.5 | 1,160.0 | 31.90ms |
| 30 | HEAVY | 62.9 | 69.5 | 477.57ms |
| 30 | MIXED | 804.0 | 987.0 | 37.39ms |
| 50 | NORMAL | 1,484.9 | 1,534.0 | 33.76ms |
| 50 | HEAVY | 63.0 | 97.0 | 791.79ms |
| 50 | MIXED | 732.8 | 992.0 | 68.44ms |
| 148 | NORMAL | 1,819.4 | 2,000.5 | 79.40ms |
| 148 | HEAVY | 63.7 | 116.0 | 2,273.01ms |
| 148 | MIXED | 463.5 | 499.5 | 318.79ms |
| 1000 | NORMAL | 1,731.5 | 1,993.0 | 516.34ms |
| 1000 | HEAVY | 86.0 | 276.0 | 9,047.01ms |
| 1000 | MIXED | 321.6 | 530.0 | 2,821.46ms |

## 6. 결과 분석

### 6.1 NORMAL 시나리오는 비교적 안정적

일반 사용자만 존재하는 경우 `vUsers 50`까지는 평균 응답 시간이 `31~34ms` 수준으로 안정적이었다.  
`vUsers 148`에서도 TPS는 `1,819.4`로 유지되었고, 평균 응답 시간은 `79.40ms` 수준이다.

다만 `vUsers 1000`에서는 평균 응답 시간이 `516.34ms`까지 상승했다.  
일반 사용자 중심 구조에서는 일정 수준까지 확장 가능하지만, 매우 큰 동시 접속에서는 점차 한계가 드러난다.

### 6.2 HEAVY 시나리오는 구조적 한계

고비용 사용자만으로 구성된 `HEAVY` 시나리오는 동시 사용자 수가 증가해도 TPS가 거의 늘지 않았다.

- `vUsers 10`: TPS `61.8`, 평균 `162.11ms`
- `vUsers 30`: TPS `62.9`, 평균 `477.57ms`
- `vUsers 50`: TPS `63.0`, 평균 `791.79ms`
- `vUsers 148`: TPS `63.7`, 평균 `2,273.01ms`
- `vUsers 1000`: TPS `86.0`, 평균 `9,047.01ms`

동시 사용자가 늘어도 처리량이 선형적으로 증가하지 않는다.
TPS는 `60~80` 수준에 머물고, 평균 응답 시간은 가파르게 증가했다. 
-> 조회 1건당 처리해야 하는 데이터량이 너무 커서 `읽기 구조 자체가 병목`

### 6.3 MIXED 시나리오, 고비용 사용자의 영향

혼합 시나리오는 초기에는 양호했지만, 부하가 커질수록 급격히 악화되었다.

- `vUsers 10`: TPS `248.3`, 평균 `40.30ms`
- `vUsers 30`: TPS `804.0`, 평균 `37.39ms`
- `vUsers 50`: TPS `732.8`, 평균 `68.44ms`
- `vUsers 148`: TPS `463.5`, 평균 `318.79ms`
- `vUsers 1000`: TPS `321.6`, 평균 `2,821.46ms`

전체 요청 중 고비용 사용자의 비중이 `20%`에 불과해도 동시 사용자가 커지는 구간에서 전체 API 성능이 급격히 하락했다.  
일부 고비용 요청이 전체 처리량에 영향을 주는 것을 알 수 있다.

## 7. 모니터링 관찰 결과

Grafana + Prometheus 모니터링 결과에서도 동일한 패턴이 확인됐다.

- `vUsers 10`에서는 피드 조회 평균 응답 시간이 약 `62.7ms`
- `vUsers 30`에서는 약 `155ms`
- `vUsers 50`에서는 약 `255ms`
- `vUsers 148`에서는 약 `894ms`
- `vUsers 1000`에서는 약 `1.40s`

모니터링 수치는 nGrinder 평균값과 완전히 동일하지는 않지만, 부하 증가에 따라 응답 시간이 상승한다.

## 8. 원인 해석

- 일반 사용자는 관련 사용자 집합과 게시물 후보군이 작아 조회 비용이 제한적이다.
- 고비용 사용자는 조회 시점마다 대규모 팔로우 집합을 만들고, 각 사용자의 게시물을 읽어온 뒤, 이를 다시 정렬해서 상위 10건만 반환한다.
- 요청이 들어올 때마다 `관계 집합 계산 -> 게시물 수집 -> 정렬 -> 이미지 조회`가 반복된다.

## 9. 결론

이번 테스트를 통해 현재 피드 조회 API는 `일반 사용자 위주 소규모 부하`에서는 동작 가능하지만, `팔로우 수가 많은 사용자`가 포함되는 순간 구조적 한계가 드러난다.

- 동시 사용자 수 증가 대비 TPS가 증가하지 않음
- 평균 응답 시간이 급격히 증가함
- 일부 고비용 요청이 전체 성능을 저하시킴

따라서 구조적 개선을 고려해볼 수 있다.

## 10. 첨부 자료

### nGrinder 결과

- [result/ngrinder_vUsers10_normal.png](result/ngrinder_vUsers10_normal.png)
- [result/ngrinder_vUsers10_heavy.png](result/ngrinder_vUsers10_heavy.png)
- [result/ngrinder_vUsers10_mix.png](result/ngrinder_vUsers10_mix.png)
- [result/ngrinder_vUsers30_normal.png](result/ngrinder_vUsers30_normal.png)
- [result/ngrinder_vUsers30_heavy.png](result/ngrinder_vUsers30_heavy.png)
- [result/ngrinder_vUsers30_mix.png](result/ngrinder_vUsers30_mix.png)
- [result/ngrinder_vUsers50_normal.png](result/ngrinder_vUsers50_normal.png)
- [result/ngrinder_vUsers50_heavy.png](result/ngrinder_vUsers50_heavy.png)
- [result/ngrinder_vUsers50_mix.png](result/ngrinder_vUsers50_mix.png)
- [result/ngrinder_vUsers148_noraml.png](result/ngrinder_vUsers148_noraml.png)
- [result/ngrinder_vUsers148_heavy.png](result/ngrinder_vUsers148_heavy.png)
- [result/ngrinder_vUsers148_mix.png](result/ngrinder_vUsers148_mix.png)
- [result/ngrinder_vUsers1000_noraml.png](result/ngrinder_vUsers1000_noraml.png)
- [result/ngrinder_vUsers1000_heavy.png](result/ngrinder_vUsers1000_heavy.png)
- [result/ngrinder_vUsers1000_mix.png](result/ngrinder_vUsers1000_mix.png)

### 모니터링 캡처

- [result/monitor_vUsers10.png](result/monitor_vUsers10.png)
- [result/monitor_vUsers30.png](result/monitor_vUsers30.png)
- [result/monitor_vUsers50.png](result/monitor_vUsers50.png)
- [result/monitor_vUsers148.png](result/monitor_vUsers148.png)
- [result/monitor_vUsers1000.png](result/monitor_vUsers1000.png)
