package ibb.api.phenotypeservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class IBeetlePhenotypeRepository {

    @ConfigProperty(name = "phenotypeservice.elasticsearch.index-prefix")
    String indexPrefix;

    @ConfigProperty(name = "data.dir")
    String dataDir;

    @Inject
    ElasticsearchClient esClient;

    @Inject
    ObjectMapper objectMapper;

    private String indexName;

    @PostConstruct
    void init() {
        indexName = indexPrefix + "-ibeetle";
    }

    void load(@Observes StartupEvent ev) throws IOException {
        if (esClient.indices().exists(r -> r.index(indexName)).value()) {
            Log.infov("Index {0} already exists, skipping data load", indexName);
            return;
        }

        Path dataPath = Paths.get(dataDir, "iBeetle.ndjson");
        try (
            var br = Files.newBufferedReader(dataPath);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {
            Log.infov("Loading data from {0} into index {1}", dataPath, indexName);
            AtomicLong counter = new AtomicLong();
            br.lines().forEach(line -> {
                counter.incrementAndGet();
                Phenotype phenotype;
                try {
                    phenotype = objectMapper.readValue(line, Phenotype.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to parse JSON at line " + counter.get() + ":", e);
                }
                ingester.add(op -> op
                    .index(idx -> idx
                        .id(phenotype.id)
                        .index(indexName)
                        .document(phenotype)
                    )
                );
            });
        }
    }

    public List<Phenotype> findByIB(String ib) {
        try {
            return esClient.search(s -> s
                .index(indexName)
                .size(10000)
                .query(q -> q
                    .term(t -> t
                        .field("dsRNA.name.keyword")
                        .value(ib)))
            , Phenotype.class)
            .hits().hits().stream().map(h -> h.source()).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
