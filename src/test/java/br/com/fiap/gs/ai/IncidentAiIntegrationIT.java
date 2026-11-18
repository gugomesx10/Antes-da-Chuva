package br.com.fiap.gs.ai;

import br.com.fiap.gs.entity.Category;
import br.com.fiap.gs.entity.Incident;
import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

import static org.junit.jupiter.api.Assertions.*;

class IncidentAiIntegrationIT {

    @Test
    void shouldAnalyzeIncidentUsingRealGemini() {
        String apiKey = System.getenv("GEMINI_API_KEY");

        assertNotNull(apiKey, "GEMINI_API_KEY não configurada");
        assertFalse(apiKey.isBlank());

        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .options(
                        GoogleGenAiChatOptions.builder()
                                .model("gemini-2.5-flash")
                                .temperature(0.2)
                                .build()
                )
                .build();

        IncidentAiService service = new IncidentAiService(
                ChatClient.builder(model),
                new RagKnowledgeService()
        );

        Category category = new Category();
        category.setName("Bueiro entupido");

        Incident incident = new Incident();
        incident.setId(100L);
        incident.setCategory(category);
        incident.setTitle("Bueiro obstruído");
        incident.setDescription(
                "Um bueiro está entupido por lixo e a água " +
                        "está acumulando na rua após a chuva."
        );
        incident.setNeighborhood("Mooca");

        AiTriageResult result = service.analyze(incident);

        assertNotNull(result);
        assertNotNull(result.urgency());
        assertNotNull(result.summary());
        assertNotNull(result.guidance());

        assertFalse(result.summary().isBlank());
        assertFalse(result.guidance().isBlank());

        assertTrue(result.summary().length() <= 300);
        assertTrue(result.guidance().length() <= 600);

        System.out.println("Urgência: " + result.urgency());
        System.out.println("Resumo: " + result.summary());
        System.out.println("Orientação: " + result.guidance());
    }
}
