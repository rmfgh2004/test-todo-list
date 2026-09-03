# Timetable Todo Backend — Agent and SDLC Pod Contract

이 파일은 backend 저장소가 단독으로 분리된 뒤에도 Claude Code와 SDLC runner가 환경을 추측하지
않도록 하는 실행 계약이다. 버전의 최종 진실 원천은 `pom.xml`과
`.mvn/wrapper/maven-wrapper.properties`다.

## 프로젝트 성격

- 단일 사용자용 주간 타임테이블/할 일 REST API
- Spring Boot 단일 프로세스, 기본 in-memory H2, 외부 서비스 의존 없음
- 기본적으로 인증 없이 loopback에만 노출되는 local-only 애플리케이션
- 모든 날짜/시간은 Asia/Seoul, 15분 단위, 계획 가능 시간은 08:00~22:00
- OpenAPI 계약: `openapi/planning-api.yaml`

## 필수 런타임과 설치 도구

| 항목            | 최소/고정 버전                              | 비고                                          |
| --------------- | ------------------------------------------- | --------------------------------------------- |
| JDK             | 17                                          | 컴파일/테스트 때문에 JRE만 설치하면 안 됨     |
| Maven           | 3.9.11                                      | 전역 설치 금지; `./mvnw`가 다운로드           |
| Maven Wrapper   | 3.3.4                                       | wrapper SHA-256은 properties에 고정           |
| Shell/OS        | Linux + Bash                                | macOS에서도 개발 가능하지만 Pod 기준은 Linux  |
| Bootstrap tools | Git, CA certificates, curl 또는 wget, unzip | cold Maven wrapper/dependency 다운로드에 필요 |

검증된 환경은 Eclipse Temurin 17.0.18이다. Pod 이미지의 배포판은 강제하지 않지만 JDK 17과 위
bootstrap 도구가 있어야 한다. Maven Central에 접근하지 못하는 환경은 미리 채운 writable
`~/.m2/repository`를 제공한다.

Backend 전용 Pod는 Temurin/OpenJDK 17 JDK 계열 Linux image를 사용하고 조직의 SDLC manifest에서
digest로 고정한다. Maven과 Spring dependency는 image에 별도 설치하지 않고 `./mvnw`가 해석한다.

## Backend 기술 스택

### 런타임

- Java 17 (`maven.compiler.release=17`)
- Spring Boot 4.1.1 dependency-management BOM
- Spring MVC, Bean Validation, Spring Security, Actuator
- Spring Data JPA, Hibernate(Boot BOM 관리), Flyway(Boot BOM 관리)
- H2 2.3.232
- Swagger UI WebJar 5.29.4

### 테스트, 품질, 보안

- Spring Boot/JUnit test starters: Spring Boot 4.1.1 BOM 관리
- jqwik 1.9.3
- ArchUnit 1.4.2
- JaCoCo 0.8.15
- Spotless Maven 3.9.0
- CycloneDX Maven 2.9.1
- OWASP Dependency-Check Maven 12.1.8

Spring starter와 전이 의존성의 실제 해석 버전은 임의로 문서에 복사하지 말고 다음으로 확인한다.

```bash
./mvnw dependency:tree
./mvnw help:effective-pom
```

## 새 Pod 부트스트랩

```bash
set -eu
java -version
chmod +x mvnw
./mvnw -version
./mvnw verify
```

표준 `verify`는 dependency resolution, Java compile, 150개 표준 테스트, JaCoCo 임계값, Spotless,
OpenAPI drift, executable JAR와 CycloneDX SBOM 생성을 수행한다. 성공 산출물은
`target/todo-backend-0.0.1-SNAPSHOT.jar`이다.

## 실행 방법

### 기본 in-memory 실행

```bash
./mvnw spring-boot:run
```

- 주소: `http://127.0.0.1:8080`
- Health: `GET http://127.0.0.1:8080/actuator/health`
- Swagger UI: `http://127.0.0.1:8080/docs/index.html`
- OpenAPI: `http://127.0.0.1:8080/openapi/planning-api.yaml`
- 데이터 수명: 프로세스 종료까지
- 종료: `SIGTERM` 또는 `Ctrl-C`

Probe 예시:

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
```

### 암호화 file profile

```bash
export PLANNING_DB_PASSWORD='<file-encryption-key> <database-user-password>'
./mvnw spring-boot:run -Dspring-boot.run.profiles=file
```

`PLANNING_DB_PASSWORD`는 공백으로 구분된 H2 composite password이며 기본값이 없다. Kubernetes
Secret으로 주입하고 로그, manifest, shell history, Git에 기록하지 않는다. `data/`에 writable PVC 또는
ephemeral volume이 필요하며 H2 URL의 `CIPHER=AES`를 제거하면 시작이 거부된다.

## 설정과 네트워크

| 설정                                       | 기본값                                           | 설명                              |
| ------------------------------------------ | ------------------------------------------------ | --------------------------------- |
| `server.address`                           | `127.0.0.1`                                      | Backend bind address              |
| `server.port`                              | `8080`                                           | HTTP port                         |
| `planning.platform.allowed-origins`        | `http://127.0.0.1:5173`, `http://localhost:5173` | exact CORS Origins; wildcard 금지 |
| `planning.platform.max-request-body-bytes` | `65536`                                          | 요청 본문 상한                    |
| rate limit                                 | 120 capacity / 분당 120 refill / 1,000 clients   | in-memory token bucket            |
| `PLANNING_DB_PASSWORD`                     | 없음                                             | file profile에서만 필수           |

같은 Pod의 frontend와는 loopback을 공유하므로 기본값을 사용한다. 별도 Pod/Service에서 접근시키는
경우 격리된 테스트 네임스페이스에서만 다음처럼 명시한다.

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--server.address=0.0.0.0 --server.port=8080 --planning.platform.allowed-origins=http://frontend.example.test"
```

Origin에는 경로가 아닌 scheme/host/port 전체를 정확히 넣는다. wildcard는 애플리케이션이 거부한다.
이 서비스는 인증이 없으므로 외부 인터넷에 공개하지 않는다.

## 테스트 명령

```bash
./mvnw test                    # 빠른 표준 테스트 루프
./mvnw verify                  # 필수 전체 gate
./mvnw -Pcapacity verify       # 표준 + 1,000/10,000 task capacity
./mvnw -Prestore verify        # 암호화 DB stop-copy-start restore
./mvnw -Psecurity-scan verify  # CVSS 7+ 차단; 취약점 DB 네트워크/cache 필요
```

품질 기준은 bundle line/branch 80% 이상과 `SchedulePolicy` branch 90% 이상이다. 표준 실행은
capacity와 restore tag를 제외하며 각 Maven profile이 필요한 fixture를 다시 포함한다.

## Pod writable 경로, 캐시, artifact

| 경로               | 용도                                 | 처리                              |
| ------------------ | ------------------------------------ | --------------------------------- |
| `target/`          | classes, JAR, Surefire, JaCoCo, SBOM | writable; CI artifact로 선택 보존 |
| `data/`            | 암호화 file H2                       | file profile만 volume 필요        |
| `~/.m2/repository` | Maven dependency/wrapper cache       | 재사용 가능 writable cache        |

전체 backend gate만 실행하는 Pod의 시작 권장치는 1~2 vCPU, 2GiB RAM, 4GiB ephemeral storage다.
capacity/security scan을 함께 수행하면 2 vCPU, 4GiB RAM, 8GiB storage부터 시작한다. 이는 코드가
강제하는 값이 아니라 SDLC runner 튜닝의 출발점이다.

## 에이전트 변경 규칙

- 도메인 → 애플리케이션 → adapter 방향의 hexagonal 경계를 유지한다.
- Flyway migration은 forward-only이며 Hibernate `ddl-auto=validate`를 유지한다.
- 모든 mutation은 `expectedVersion`을 받고 서버가 최종 권위다.
- OpenAPI 변경은 API 구현, 계약 테스트와 frontend 생성 타입을 함께 갱신한다.
- wildcard CORS, H2 console/TCP, 평문 file DB, task 내용/비밀 로그를 추가하지 않는다.
- 변경 후 최소 `./mvnw verify`를 실행하고 결과를 보고한다.

## 분리 저장소 체크리스트

1. `pom.xml`, `.mvn/`, `mvnw`, `mvnw.cmd`, `openapi/`, `src/`가 함께 이동해야 한다.
2. 실행 bit가 보존되지 않으면 `chmod +x mvnw`를 수행한다.
3. frontend 저장소/Pod에는 이 저장소의 OpenAPI 파일 위치와 접근 방법을 제공한다.
4. CI가 Maven Central 또는 내부 mirror와 CA trust를 사용할 수 있는지 확인한다.
5. runtime Pod는 기본 loopback 정책을 유지하거나, 외부 bind 시 격리/Origin 정책을 명시적으로 검토한다.
