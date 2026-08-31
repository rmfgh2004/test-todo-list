# Requirements Verification Questions

프로젝트 방향을 확정하기 위한 질문입니다. 각 `[Answer]:` 뒤에 선택지 문자와 판단
근거를 함께 적어 주세요. 권장안은 현재 요청, 디자인 레퍼런스, 구현 복잡도를 기준으로
선정했습니다.

## Question 1
첫 번째 릴리스의 사용자 범위는 어디까지로 할까요?

A) 인증 없는 단일 사용자 로컬 플래너. 개인 일정과 할 일을 한 브라우저에서 관리한다. (권장: 핵심 타임테이블 경험과 풀스택 품질에 집중 가능)

B) 로그인하는 단일 사용자 서비스. 계정별 데이터 격리를 포함한다.

C) 팀 협업 서비스. 계정, 담당자 배정, 댓글과 권한까지 포함한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 핵심 타임테이블 경험과 프론트엔드·백엔드 품질을 첫 릴리스 안에서 모두 확보하기 위해 인증과 협업 기능을 제외한다.

## Question 2
첫 번째 릴리스에 포함할 화면과 사용자 흐름은 무엇인가요?

A) 주간 타임테이블, 미배치 할 일 백로그, 할 일 생성·수정·완료, 드래그 배치, 목록 보기, 반응형 모바일 화면을 포함한다. 보드·댓글·첨부파일은 제외한다. (권장)

B) A에 칸반 보드와 상세 하위 작업을 추가한다.

C) 디자인 레퍼런스의 전체 범위인 주간·목록·보드, 하위 작업, 댓글, 첨부파일, 담당자, 반복 일정을 모두 포함한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 단순 컴포넌트가 아닌 생성부터 배치·완료까지의 실제 사용자 흐름을 제공하면서도 보드·댓글·첨부파일로 범위가 분산되는 것을 막는다.

## Question 3
할 일을 타임테이블에 배치할 때 시간 충돌을 어떻게 처리할까요?

A) 사용자가 직접 드래그하거나 시간을 입력한다. 충돌을 사전 탐지하고 저장 전에 기존 일정 유지, 새 일정 이동, 또는 취소를 선택하게 한다. (권장: 예측 가능성과 데이터 보호)

B) 충돌하지 않는 가장 가까운 시간으로 시스템이 자동 이동한다.

C) 겹치는 일정을 허용하고 경고만 표시한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 시스템의 조용한 자동 이동이나 일정 중첩보다 사용자가 충돌을 인지하고 결과를 직접 선택하는 방식이 예측 가능하고 안전하다.

## Question 4
날짜·시간의 기본 정책은 무엇으로 할까요?

A) Asia/Seoul 고정, 월요일 시작 주간, 15분 단위, 기본 표시 시간 08:00~22:00으로 한다. (권장)

B) 브라우저 시간대를 사용하고 일요일 시작 주간, 30분 단위로 한다.

C) 사용자가 시간대, 주 시작일, 시간 단위를 설정할 수 있게 한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 대상 사용 환경을 한국 로컬 플래너로 한정하고 15분 단위의 세밀한 배치와 일반적인 업무·생활 시간대를 일관되게 제공한다.

## Question 5
Java 백엔드 구성은 무엇으로 할까요?

A) Spring Boot 기반 REST API, Spring Data JPA, Bean Validation, H2 파일 모드 개발 프로필과 인메모리 테스트 프로필을 사용한다. (권장)

B) Jakarta REST 기반 REST API와 직접 JDBC 접근을 사용한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: REST, 검증, 트랜잭션, H2 프로필 분리와 테스트 지원이 성숙한 Spring Boot 구성이 풀스택 개발의 안정성과 유지보수성에 적합하다.

## Question 6
보안 확장 규칙을 이 프로젝트의 차단 조건으로 적용할까요?

A) 예. 모든 SECURITY 규칙과 사용자 요청의 코드별 보안 리뷰 체크리스트를 차단 조건으로 적용한다. (권장)

B) 아니요. 사용자 요청의 자체 보안 체크리스트만 적용하고 확장 규칙은 사용하지 않는다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 사용자가 매 코드 작성 시 보안 리뷰를 필수 규칙으로 요청했으므로 Security Baseline 전체를 차단 조건으로 적용한다.

## Question 7
복원력 기준선 확장 규칙을 적용할까요?

A) 아니요. 로컬 중심 첫 릴리스에서는 복잡도를 줄이고 API 오류 처리, 트랜잭션, 백업 가능한 파일 DB에 집중한다. (권장)

B) 예. 가용성, 관측성, 복구 목표 등 전체 복원력 기준선을 설계 지침으로 적용한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 첫 릴리스는 로컬 단일 인스턴스이므로 클라우드 고가용성보다 트랜잭션, 오류 처리, 파일 DB 복구 가능성을 우선한다.

## Question 8
속성 기반 테스트 규칙을 적용할까요?

A) 부분 적용. 시간 범위, 충돌 판정, 반복 일정 계산 같은 순수 도메인 로직에만 적용한다. (권장)

B) 전체 적용. 직렬화와 상태 전이를 포함한 모든 적용 가능한 영역에서 차단 조건으로 사용한다.

C) 적용하지 않는다. 예제 기반 단위·통합·E2E 테스트만 사용한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 시간 범위와 충돌 계산에는 생성형 검증의 효과가 크지만 단순 CRUD와 UI 전반에 적용하면 비용 대비 효용이 낮으므로 핵심 순수 로직에 한정한다.
