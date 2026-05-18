package com.todo.todolist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "holiday",
        uniqueConstraints = @UniqueConstraint(columnNames = "locdate")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 날짜 (공공 API locdate → LocalDate, DB DATE) */
    @Column(name = "locdate", nullable = false)
    private LocalDate locDate;

    /** 공휴일·특일 이름 (공공 API dateName) */
    @Column(nullable = false, length = 50)
    private String dateName;

    /** 공휴일 여부 (공공 API isHoliday, Y/N) */
    @Column(nullable = false, length = 1)
    private String isHoliday;
}
