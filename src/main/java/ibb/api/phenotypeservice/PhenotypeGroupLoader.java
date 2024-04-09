package ibb.api.phenotypeservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PhenotypeGroupLoader {

    @ConfigProperty(name = "phenotypeservice.elasticsearch.index-prefix")
    String indexPrefix;

    @ConfigProperty(name = "data.dir")
    String dataDir;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ElasticsearchClient esClient;

    void loadIBeetlePhenotypes(@Observes StartupEvent ev) throws IOException {
        String indexName = "test";
        // String indexName = indexPrefix + "-ibeetle";
        if (esClient.indices().exists(r -> r.index(indexName)).value()) {
            Log.infov("Index {0} already exists, skipping loading data", indexName);
            return;
        }

        Path dataPath = Paths.get(dataDir, "iBeetle_grouped_phenotypes.ndjson");
        try (
            var br = Files.newBufferedReader(dataPath);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {
            Log.infov("Loading data from {0} into index {1}", dataPath, indexName);
            br.lines().forEach(line -> {
                ingester.add(op -> op
                    .index(idx -> {
                        try {
                            return idx
                                .index(indexName)
                                .document(objectMapper.readTree(line));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    )
                );
            });
        }
        esClient.indices().refresh(r -> r.index(indexName));
        Log.infov("Loaded {0} phenotypes", esClient.count(r -> r.index(indexName)).count());
    }
}
