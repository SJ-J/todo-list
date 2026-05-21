package com.todo.todolist.repository;

import com.todo.todolist.entity.RepeatRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepeatRuleRepository extends JpaRepository<RepeatRule, Long> {
}
