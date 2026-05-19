package com.todo.todolist.repository;

import com.todo.todolist.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    // 기간 내 전체 특일 조회 (isHoliday Y/N 구분 없음)
    List<Holiday> findByLocDateBetween(LocalDate start, LocalDate end);

    // 재동기화 시 해당 월 기존 데이터 일괄 삭제, clearAutomatically로 1차 캐시 동기화
    @Modifying(clearAutomatically = true)
    @Query("delete from Holiday h where h.locDate between :start and :end")
    void deleteByLocDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
