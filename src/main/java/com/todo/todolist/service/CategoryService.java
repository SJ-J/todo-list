package com.todo.todolist.service;

import com.todo.todolist.dto.CategoryRequest;
import com.todo.todolist.entity.Category;
import com.todo.todolist.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private static final int MAX_CATEGORIES = 8;
    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category create(CategoryRequest request) {
        if (categoryRepository.count() >= MAX_CATEGORIES) {
            throw new IllegalStateException("카테고리는 최대 8개까지 생성할 수 있습니다.");
        }
        if (categoryRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 카테고리 이름입니다.");
        }
        Category category = Category.builder()
                .name(request.name())
                .color(request.color())
                .bgColor(request.bgColor())
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
        category.setName(request.name());
        category.setColor(request.color());
        category.setBgColor(request.bgColor());
        return category;
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
