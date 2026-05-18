package com.todo.todolist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "holiday.api")
public record HolidayApiProperties(
        String serviceKey,
        String url
) {
}
