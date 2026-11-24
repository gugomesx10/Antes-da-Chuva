package br.com.fiap.gs.config;

import br.com.fiap.gs.controller.CategoryController;
import br.com.fiap.gs.controller.FrontendController;
import br.com.fiap.gs.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = {FrontendController.class, CategoryController.class},
        properties = {
                "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "security.jwt.issuer=https://antes-da-chuva.example",
                "security.jwt.expiration-seconds=3600"
        }
)
@Import(SecurityConfig.class)
class FrontendRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void shouldServeAppRoutesWithoutAuthentication() throws Exception {
        for (String route : FrontendController.ROUTES) {
            String url = route.replace("{id}", "10");

            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    void shouldKeepApiProtected() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());
    }
}
