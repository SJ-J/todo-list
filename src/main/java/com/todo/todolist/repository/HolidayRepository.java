package com.todo.todolist.repository;

import com.todo.todolist.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByLocDateBetween(LocalDate start, LocalDate end);

    long countByLocDateBetween(LocalDate start, LocalDate end);

    @Modifying(clearAutomatically = true)
    @Query("delete from Holiday h where h.locDate between :start and :end")
    void deleteByLocDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
