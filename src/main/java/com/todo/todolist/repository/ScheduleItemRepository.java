package com.todo.todolist.repository;

import com.todo.todolist.entity.Category;
import com.todo.todolist.entity.RepeatRule;
import com.todo.todolist.entity.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    // 해당 날짜를 포함하는 일정 조회(정렬 순서 기준 오름차순)
    @Query("SELECT s FROM ScheduleItem s WHERE s.startDate <= :date AND s.endDate >= :date ORDER BY s.sortOrder")
    List<ScheduleItem> findByDate(@Param("date") LocalDate date);

    // 현재 최대 정렬 순서 조회(신규 일정 순서 부여 용)
    @Query("SELECT MAX(s.sortOrder) FROM ScheduleItem s")
    Integer findMaxSortOrder();

    // 현재 최대 완료 순서 조회(완료 처리 순서 부여 용)
    @Query("SELECT MAX(s.completedOrder) FROM ScheduleItem s")
    Integer findMaxCompletedOrder();

    void deleteByCategory(Category category);

    // 반복 일정 전체 조회
    List<ScheduleItem> findByRepeatRule(RepeatRule repeatRule);

    // 이후 일정 조회(이후 일정 수정/삭제 용)
    List<ScheduleItem> findByRepeatRuleAndRepeatSeqGreaterThanEqual(RepeatRule repeatRule, int seq);
}
