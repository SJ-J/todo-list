package com.todo.todolist.repository;

import com.todo.todolist.entity.HolidaySyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HolidaySyncLogRepository extends JpaRepository<HolidaySyncLog, Long> {

    // 해당 연월 동기화 이력 존재 여부 - API 호출 스킵 판단에 사용
    boolean existsByYearAndMonth(int year, int month);

    // 강제 재동기화 시 해당 월 sync_log 삭제 - 다음 조회에서 API 재호출 유도
    @Modifying(clearAutomatically = true)
    @Query("delete from HolidaySyncLog s where s.year = :year and s.month = :month")
    void deleteByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
