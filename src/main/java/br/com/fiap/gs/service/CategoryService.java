package br.com.fiap.gs.service;

import br.com.fiap.gs.dto.CategoryRequest;
import br.com.fiap.gs.dto.CategoryResponse;
import br.com.fiap.gs.entity.Category;
import br.com.fiap.gs.exception.ConflictException;
import br.com.fiap.gs.exception.ResourceNotFoundException;
import br.com.fiap.gs.mapper.CategoryMapper;
import br.com.fiap.gs.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public Page<CategoryResponse> findAll(Pageable pageable) {
        return repository.findByActiveTrue(pageable)
                .map(mapper::toResponse);
    }

    public CategoryResponse findById(Long id) {
        return mapper.toResponse(findActiveCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();

        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Categoria já cadastrada");
        }

        Category category = mapper.toEntity(request);

        return mapper.toResponse(repository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findActiveCategory(id);

        String name = request.name().trim();

        repository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Categoria já cadastrada");
                });

        mapper.updateEntity(category, request);

        return mapper.toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findActiveCategory(id);
        category.setActive(false);
        repository.save(category);
    }

    private Category findActiveCategory(Long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada"
                ));

        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new ResourceNotFoundException("Categoria não encontrada");
        }

        return category;
    }
}
