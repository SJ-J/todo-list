package com.todo.todolist.controller;

import com.todo.todolist.dto.HolidayResponse;
import com.todo.todolist.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공공데이터 Open API(공휴일) — Spring Boot에서 호출·파싱·DB 저장 후 Vue에 제공.
 */
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    /** 월별 공휴일 조회 (DB에 없으면 Open API 동기화) */
    @GetMapping
    public List<HolidayResponse> getHolidays(
            @RequestParam int year,
            @RequestParam int month) {
        return holidayService.getHolidays(year, month);
    }
}
