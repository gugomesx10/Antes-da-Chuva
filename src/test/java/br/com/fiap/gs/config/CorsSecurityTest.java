package br.com.fiap.gs.config;

import br.com.fiap.gs.controller.CategoryController;
import br.com.fiap.gs.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CategoryController.class,
        properties = {
                "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "security.jwt.issuer=https://antes-da-chuva.example",
                "security.jwt.expiration-seconds=3600",
                "app.cors.allowed-origins=http://localhost:8099"
        }
)
@Import(SecurityConfig.class)
class CorsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void shouldAllowPreflightFromFrontendOrigin() throws Exception {
        mockMvc.perform(options("/categories")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8099")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:8099"
                ));
    }

    @Test
    void shouldRejectPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/categories")
                        .header(HttpHeaders.ORIGIN, "https://site-desconhecido.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
