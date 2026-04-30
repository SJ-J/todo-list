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

### 실행

IntelliJ에서 `TodoListApplication.java` 실행
또는

```bash
./gradlew bootRun
```

서버가 `http://localhost:8080` 에서 실행됩니다.

## 📡 API

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/items` | 전체 일정 조회 |
| GET | `/api/items?date=2024-01-15` | 날짜별 일정 조회 |
| POST | `/api/items` | 일정 추가 |
| PUT | `/api/items/{id}` | 일정 수정 |
| PATCH | `/api/items/{id}/complete` | 완료 토글 |
| DELETE | `/api/items/{id}` | 일정 삭제 |
| GET | `/api/categories` | 카테고리 목록 |
| POST | `/api/categories` | 카테고리 추가 |
| PUT | `/api/categories/{id}` | 카테고리 수정 |
| DELETE | `/api/categories/{id}` | 카테고리 삭제 |

## 👥 팀원

| 이름 | 역할 |
|------|------|
| [@yujin149](https://github.com/yujin149) | 🎨 Design / Publishing / Frontend |
| [@SJ-J](https://github.com/SJ-J) | 🔧 Backend / DB |
