package com.todo.todolist.controller;

import com.todo.todolist.dto.CategoryRequest;
import com.todo.todolist.entity.Category;
import com.todo.todolist.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 전체 카테고리 목록 조회
    @GetMapping
    public List<Category> getAll() {
        return categoryService.findAll();
    }

    // 카테고리 생성
    @PostMapping
    public Category create(@RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    // ID로 카테고리 수정
    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    // ID로 카테고리 삭제(연관 일정 포함)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}