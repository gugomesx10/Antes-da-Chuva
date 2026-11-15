package br.com.fiap.gs.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RagKnowledgeServiceTest {

    private final RagKnowledgeService service =
            new RagKnowledgeService();

    @Test
    void shouldRetrieveDrainageGuidance() {
        var results = service.retrieve(
                "Bueiro entupido por lixo e entulho"
        );

        assertFalse(results.isEmpty());
        assertEquals("bueiros", results.getFirst().id());
    }

    @Test
    void shouldLimitRetrievedDocuments() {
        var results = service.retrieve(
                "Alagamento e chuva com agua acumulada"
        );

        assertTrue(results.size() <= 2);
    }

    @Test
    void shouldReturnEmptyForUnrelatedInput() {
        var results = service.retrieve(
                "xadrez astronomia telescopio"
        );

        assertTrue(results.isEmpty());
    }
}
