package com.todo.todolist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "schedule_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer priority;

    @Column(length = 20)
    private String priorityLabel;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean completed;

    private Integer completedOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // 반복 기능 추가 :: 260521
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPEAT_RULE_ID")
    private RepeatRule repeatRule;

    // Lombok boolean + is 접두어 충돌 방지로 repeatOrigin 사용
    @Column(name = "IS_REPEAT_ORIGIN", nullable = false)
    private boolean repeatOrigin;

    @Column(name = "REPEAT_SEQ")
    private Integer repeatSeq;
}
