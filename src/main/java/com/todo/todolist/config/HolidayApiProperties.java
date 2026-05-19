package com.todo.todolist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yaml holiday.api.* 바인딩
@ConfigurationProperties(prefix = "holiday.api")
public record HolidayApiProperties(
        String serviceKey, // 공공데이터포털 인증키 (환경변수 HOLIDAY_SERVICE_KEY)
        String url         // 특일정보 API 엔드포인트
) {
}
