package ibb.api.phenotypeservice.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.synonyms.SynonymRule;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SynonymLoader {
    public static String SYNONYM_SET_ID = "phenotypeservice-phenotype-synonyms";

    @ConfigProperty(name = "data.dir")
    String dataDir;

    @Inject
    ElasticsearchClient esClient;

    public void load() throws IOException {
        List<String> synonyms = getSynonyms();
        List<SynonymRule> rules = synonyms.stream()
                .map(line -> SynonymRule.of(r -> r.synonyms(line)))
                .toList();
        esClient.synonyms().putSynonym(s -> s
                .id(SYNONYM_SET_ID)
                .synonymsSet(rules));
    }

    public List<String> getSynonyms() throws IOException {
        Path dataPath = Paths.get(dataDir, "synonyms.txt");
        if (!Files.exists(dataPath)) {
            Log.warnv("Synonyms file {0} not provided", dataPath);
            return List.of();
        }
        try (var br = Files.newBufferedReader(dataPath);) {
            return br.lines().toList();
        }
    }
}
