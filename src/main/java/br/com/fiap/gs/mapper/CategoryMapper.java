package br.com.fiap.gs.mapper;

import br.com.fiap.gs.dto.CategoryRequest;
import br.com.fiap.gs.dto.CategoryResponse;
import br.com.fiap.gs.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        Category category = new Category();
        updateEntity(category, request);
        category.setActive(true);
        return category;
    }

    public void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setBaseWeight(request.baseWeight());
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getBaseWeight(),
                category.getActive()
        );
    }
}
