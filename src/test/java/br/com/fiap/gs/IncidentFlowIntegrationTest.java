package br.com.fiap.gs;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo pela API, com banco H2 criado pelas migrations Flyway (V1 a V3).
 * Usa o GESTOR do seed (V3) e um CIDADAO cadastrado no próprio teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentFlowIntegrationTest {

    private static final String GESTOR_EMAIL = "gestor@antesdachuva.com";
    private static final String GESTOR_PASSWORD = "Gestor@2026";
    private static final long BUEIRO_ENTUPIDO = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterTriageAndTrackIncident() throws Exception {
        String gestor = login(GESTOR_EMAIL, GESTOR_PASSWORD);
        String cidadao = registerCitizen();

        Long incidentId = createIncident(cidadao);

        // Gestor move para análise e registra o andamento
        mockMvc.perform(post("/incidents/{id}/updates", incidentId)
                        .header("Authorization", bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus": "IN_REVIEW", "note": "Equipe de limpeza acionada"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.previousStatus").value("OPEN"))
                .andExpect(jsonPath("$.newStatus").value("IN_REVIEW"));

        // Transição inválida: não pode pular de IN_REVIEW direto para RESOLVED
        mockMvc.perform(post("/incidents/{id}/updates", incidentId)
                        .header("Authorization", bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus": "RESOLVED", "note": "Tentativa inválida"}
                                """))
                .andExpect(status().isConflict());

        // O morador acompanha a linha do tempo
        mockMvc.perform(get("/incidents/{id}/updates", incidentId)
                        .header("Authorization", bearer(cidadao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Depois de sair de OPEN, o morador não pode mais cancelar
        mockMvc.perform(delete("/incidents/{id}", incidentId)
                        .header("Authorization", bearer(cidadao)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldLetOwnerCancelOpenIncident() throws Exception {
        String cidadao = registerCitizen();
        Long incidentId = createIncident(cidadao);

        mockMvc.perform(delete("/incidents/{id}", incidentId)
                        .header("Authorization", bearer(cidadao)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/incidents/{id}", incidentId)
                        .header("Authorization", bearer(cidadao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldEnforceRolesAndAuthentication() throws Exception {
        String cidadao = registerCitizen();

        mockMvc.perform(get("/incidents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/categories")
                        .header("Authorization", bearer(cidadao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Categoria indevida", "baseWeight": 3}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "senha-errada"}
                                """.formatted(GESTOR_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidIncident() throws Exception {
        String cidadao = registerCitizen();

        mockMvc.perform(post("/incidents")
                        .header("Authorization", bearer(cidadao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId": 1, "title": "", "description": "", "address": "", "neighborhood": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    private Long createIncident(String token) throws Exception {
        String body = mockMvc.perform(post("/incidents")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "Bueiro entupido na esquina",
                                  "description": "Boca de lobo coberta de folhas e lixo; a rua alagou na última chuva.",
                                  "address": "Rua das Flores, 120",
                                  "neighborhood": "Jardim Pantanal",
                                  "latitude": -23.498000,
                                  "longitude": -46.402000
                                }
                                """.formatted(BUEIRO_ENTUPIDO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.triageStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String registerCitizen() throws Exception {
        String email = "morador-" + UUID.randomUUID() + "@teste.com";

        String body = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Morador Teste", "email": "%s", "password": "Senha@2026", "neighborhood": "Jardim Pantanal"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CIDADAO"))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
