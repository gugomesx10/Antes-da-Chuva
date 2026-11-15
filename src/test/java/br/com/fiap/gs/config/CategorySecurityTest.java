package br.com.fiap.gs.config;

import br.com.fiap.gs.controller.CategoryController;
import br.com.fiap.gs.service.CategoryService;
import br.com.fiap.gs.dto.CategoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = CategoryController.class,
        properties = {
                "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "security.jwt.issuer=https://antes-da-chuva.example",
                "security.jwt.expiration-seconds=3600"
        }
)
@Import(SecurityConfig.class)
class CategorySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    private static final String CATEGORY_JSON = """
            {
                "name": "Bueiro entupido",
                "description": "Obstrução na drenagem urbana",
                "baseWeight": 4
            }
            """;

    @Test
    void shouldRejectUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldRejectCitizenCreatingCategory() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_CIDADAO")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CATEGORY_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldAllowManagerCreatingCategory() throws Exception {

        CategoryResponse response = new CategoryResponse(
                100L,
                "Bueiro entupido",
                "Obstrução na drenagem urbana",
                (short) 4,
                true
        );


        when(categoryService.create(any()))
                .thenReturn(response);

        mockMvc.perform(post("/categories")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_GESTOR")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CATEGORY_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));

        verify(categoryService).create(any());
    }
}
