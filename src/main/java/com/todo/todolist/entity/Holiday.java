package com.todo.todolist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 공공 API 특일 정보 저장 엔티티
@Entity
@Table(
        name = "holiday",
        // 날짜 중복 저장 방지
        uniqueConstraints = @UniqueConstraint(columnNames = "locdate")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공공 API locdate → LocalDate 변환값
    @Column(name = "locdate", nullable = false)
    private LocalDate locDate;

    // 공공 API dateName (예: 어린이날, 현충일)
    @Column(nullable = false, length = 50)
    private String dateName;

    // 공공 API isHoliday — Y: 법정공휴일, N: 기념일·대체공휴일 등
    @Column(nullable = false, length = 1)
    private String isHoliday;
}
