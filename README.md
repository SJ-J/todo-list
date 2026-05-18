# 📝 Todo List - Backend

일정을 카테고리별로 관리하고 캘린더로 한눈에 확인할 수 있는 투두리스트 앱의 백엔드 서버입니다.

## 🛠 기술 스택

- Java 21
- Spring Boot 4.0
- Spring Data JPA
- Spring Security
- MySQL 8.0
- Gradle

## 🚀 시작하기

### 사전 준비
- JDK 21 이상
- MySQL 8.0 이상
- Frontend → [todo-list-frontend](https://github.com/SJ-J/todoList)

### 환경 설정

`src/main/resources/application.yaml` 수정

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/todolist
    username: root
    password: 본인_비밀번호
```

공휴일 연동을 위해 [공공데이터포털](https://www.data.go.kr/)에서 **한국천문연구원_특일 정보** API 활용 신청 후 인증키를 설정합니다.

| 방식 | 설명 |
|------|------|
| 환경 변수 | `HOLIDAY_SERVICE_KEY=발급받은_인증키` |
| 로컬 프로필 | `src/main/resources/application-local.yaml` 생성 (`.gitignore` 대상) |

`application-local.yaml` 예시:

```yaml
holiday:
  api:
    service-key: 발급받은_인증키
```

기본 API URL은 `application.yaml`에 정의되어 있으며, 필요 시 `holiday.api.url`로 변경할 수 있습니다.

### 실행

IntelliJ에서 `TodoListApplication.java` 실행
또는

```bash
./gradlew bootRun
```

서버가 `http://localhost:8080` 에서 실행됩니다.

## 📡 API

| Method | URL                               | 설명 |
|--------|-----------------------------------|------|
| GET | `/api/items`                      | 전체 일정 조회 |
| GET | `/api/items?date=2024-01-15`      | 날짜별 일정 조회 |
| POST | `/api/items`                      | 일정 추가 |
| PUT | `/api/items/{id}`                 | 일정 수정 |
| PATCH | `/api/items/{id}/complete`        | 완료 토글 |
| DELETE | `/api/items/{id}`                 | 일정 삭제 |
| GET | `/api/categories`                 | 카테고리 목록 |
| POST | `/api/categories`                 | 카테고리 추가 |
| PUT | `/api/categories/{id}`            | 카테고리 수정 |
| DELETE | `/api/categories/{id}`            | 카테고리 삭제 |
| GET | `/api/holidays?year=2026&month=5` | 월별 공휴일 조회 |

**공휴일 응답 예시** (`GET /api/holidays?year=2026&month=5`)

```json
[
  { "date": "2026-05-05", "name": "어린이날" }
]
```

## 🗓 공공데이터 API

백엔드가 [한국천문연구원 특일 정보](https://www.data.go.kr/data/15012690/openapi.do) Open API를 호출해 DB에 캐시한 뒤, 프론트엔드에는 자체 REST API(`/api/holidays`)로 제공합니다.

| 항목 | 내용 |
|------|------|
| 제공 기관 | 한국천문연구원 |
| API 명 | 특일 정보 (`getRestDeInfo`) |
| Base URL | `https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo` |
| 응답 형식 | JSON (`_type=json`) |

**Open API 요청 파라미터**

| 파라미터 | 설명 | 예시 |
|----------|------|------|
| `ServiceKey` | 공공데이터포털 인증키 | (발급 키) |
| `solYear` | 연도 | `2025` |
| `solMonth` | 월 (2자리) | `05` |
| `numOfRows` | 조회 건수 | `100` |
| `_type` | 응답 형식 | `json` |

**동작 요약**

1. `GET /api/holidays` 요청 시 해당 월 데이터가 DB(`holiday` 테이블)에 없으면 Open API를 호출해 동기화합니다.
2. 동기화된 데이터 중 `isHoliday=Y`인 날짜만 `date`, `name` 형태로 반환합니다.
3. 이미 DB에 해당 월 데이터가 있으면 Open API를 다시 호출하지 않고 DB에서 조회합니다.

## 👥 팀원

| 이름 | 역할 |
|------|------|
| [@yujin149](https://github.com/yujin149) | 🎨 Design / Publishing / Frontend |
| [@SJ-J](https://github.com/SJ-J) | 🔧 Backend / DB |
