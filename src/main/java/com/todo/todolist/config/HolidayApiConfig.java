package com.todo.todolist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class HolidayApiConfig {

    // 공공 API 전용 RestClient - timeout 설정으로 무응답 시 스레드 점유 방지
    @Bean
    public RestClient holidayRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));  // 연결 타임아웃
        factory.setReadTimeout(Duration.ofSeconds(10));    // 응답 읽기 타임아웃
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
