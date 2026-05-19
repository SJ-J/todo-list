package com.todo.todolist.dto;

// 공휴일 조회 응답 DTO (date: "yyyy-MM-dd", name: 공휴일명)
public record HolidayResponse(String date, String name) {
}
