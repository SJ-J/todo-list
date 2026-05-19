package com.todo.todolist.controller;

import com.todo.todolist.dto.HolidayResponse;
import com.todo.todolist.service.HolidayService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공공데이터 Open API 공휴일 정보 제공 컨트롤러
 * DB에 동기화 이력이 없는 월은 공공 API를 호출해 자동 동기화 후 반환
 * 1일 1,000회 호출 제한 대응을 위해 {@code HolidaySyncLog} 테이블로 중복 호출 방지
 */
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@Validated
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * 월별 공휴일 목록 조회
     * 해당 월의 동기화 이력이 없으면 공공 API를 호출하고, 이력이 있으면 DB에서 바로 반환
     *
     * @param year  조회 연도 (1900~2100)
     * @param month 조회 월 (1~12)
     * @return {@code isHoliday = "Y"}인 공휴일 목록 (date, name)
     */
    @GetMapping
    public List<HolidayResponse> getHolidays(
            @RequestParam @Min(1900) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return holidayService.getHolidays(year, month);
    }

    /**
     * 특정 월 동기화 이력 초기화 (강제 재동기화)
     * 대체공휴일 등 공공 API 데이터 변경 시 해당 월의 sync_log를 삭제
     * 이후 첫 조회 요청에서 공공 API를 다시 호출해 최신 데이터로 갱신
     *
     * @param year  대상 연도 (1900~2100)
     * @param month 대상 월 (1~12)
     */
    @DeleteMapping("/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetSync(
            @RequestParam @Min(1900) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        holidayService.resetSync(year, month);
    }
}
