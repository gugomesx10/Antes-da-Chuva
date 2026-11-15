package br.com.fiap.gs.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RagKnowledgeService {

    private final List<KnowledgeExcerpt> documents;

    public RagKnowledgeService() {
        this.documents = List.of(
                load(
                        "bueiros",
                        "Prevenção de obstruções",
                        "https://www.gov.br/mdr/pt-br/noticias/defesa-civil-prevencao-ajuda-a-evitar-desastres-naturais",
                        "bueiros.txt"
                ),
                load(
                        "alagamentos",
                        "Segurança durante alagamentos",
                        "https://cge.prefeitura.sp.gov.br/v3/dicas.jsp",
                        "alagamentos.txt"
                ),
                load(
                        "prevencao",
                        "Prevenção e preparação",
                        "https://miniapps.saude.gov.br/guia-clima/como-podemos-contribuir/populacao-em-geral/enchentes-e-inundacoes",
                        "prevencao.txt"
                )
        );
    }

    public List<KnowledgeExcerpt> retrieve(String query) {
        Set<String> queryTerms = tokenize(query);

        return documents.stream()
                .filter(doc -> score(queryTerms, doc) > 0)
                .sorted(
                        Comparator.comparingInt(
                                (KnowledgeExcerpt doc) ->
                                        score(queryTerms, doc)
                        ).reversed()
                )
                .limit(2)
                .toList();
    }

    private int score(Set<String> queryTerms, KnowledgeExcerpt doc) {
        Set<String> documentTerms = tokenize(
                doc.title() + " " + doc.content()
        );

        return (int) queryTerms.stream()
                .filter(documentTerms::contains)
                .count();
    }

    private Set<String> tokenize(String value) {
        String normalized = Normalizer.normalize(
                        value.toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ");

        return Arrays.stream(normalized.split("\\s+"))
                .filter(term -> term.length() >= 4)
                .collect(Collectors.toSet());
    }

    private KnowledgeExcerpt load(
            String id,
            String title,
            String url,
            String fileName
    ) {
        ClassPathResource resource =
                new ClassPathResource("knowledge/" + fileName);

        try (var input = resource.getInputStream()) {
            return new KnowledgeExcerpt(
                    id,
                    title,
                    url,
                    new String(
                            input.readAllBytes(),
                            StandardCharsets.UTF_8
                    )
            );
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Não foi possível carregar " + fileName,
                    ex
            );
        }
    }
}
