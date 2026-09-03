# Swagger Runtime 401 Clarification Questions

직접 실행 중인 백엔드에서 아래 네 경로는 모두 HTTP 200으로 확인됐다.

- `http://127.0.0.1:8080/docs/index.html`
- `http://127.0.0.1:8080/openapi/planning-api.yaml`
- `http://127.0.0.1:8080/docs/swagger-initializer.js`
- `http://127.0.0.1:8080/webjars/swagger-ui/5.29.4/swagger-ui-bundle.js`

저장소와 OpenAPI 계약에도 `tempo.app` 주소는 없다. 잘못된 보안 변경을 피하려면 401이 나타난
위치를 확정해야 한다. 답변은 아래 `[Answer]:` 뒤에 선택지 문자를 입력한다.

## Question 1

`authentication-required` JSON이 나타난 상황은 어느 것인가?

A) 주소창에 정확히 `http://127.0.0.1:8080/docs/index.html`을 입력하자 페이지 대신 JSON이
나왔다. 가장 직접적인 재현 경로이므로 권장한다. 사용한 전체 URL도 함께 알려준다.

B) Swagger 문서 화면은 정상적으로 열렸고, 특정 API에서 **Try it out → Execute**를 누른 뒤
응답 영역에 JSON이 나왔다. 실행한 operation 이름도 함께 알려준다.

C) `127.0.0.1:8080`이 아닌 Tempo 미리보기·공유 URL 또는 다른 프록시 URL로 Swagger에
접속하자 JSON이 나왔다. 사용한 전체 URL도 함께 알려준다.

D) Other (please describe after [Answer]: tag below)

[Answer]: 직접 로컬 URL은 정상 동작하며 Swagger 문서가 표시된다.

**Decision:** `http://127.0.0.1:8080/docs/index.html`의 직접 접속이 정상임을 사용자가 확인했다.
앞서 보인 `tempo.app` 401은 현재 로컬 백엔드에서 재현되지 않으며 이 저장소의 인증 설정 결함으로
판정하지 않는다. 공개 경로나 보안 설정을 변경하지 않고 이 런타임 이슈를 종료한다.
