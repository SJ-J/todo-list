package com.todo.todolist.repository;

import com.todo.todolist.entity.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    @Query("SELECT s FROM ScheduleItem s WHERE s.startDate <= :date AND s.endDate >= :date ORDER BY s.sortOrder")
    List<ScheduleItem> findByDate(@Param("date") LocalDate date);

    @Query("SELECT MAX(s.sortOrder) FROM ScheduleItem s")
    Integer findMaxSortOrder();

    @Query("SELECT MAX(s.completedOrder) FROM ScheduleItem s")
    Integer findMaxCompletedOrder();
}
