package ibb.api.phenotypeservice.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import ibb.api.phenotypeservice.core.Lethality;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IBeetleLethalityLoader {
    @ConfigProperty(name = "data.dir")
    String dataDir;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ElasticsearchClient esClient;

    public void loadIBeetleLethality(String indexName) throws IOException {
        Path dataPath = Paths.get(dataDir, "iBeetle_lethality.csv");
        try (
            var br = Files.newBufferedReader(dataPath);
            BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(esClient))
        ) {
            Log.infov("Loading data from {0} into index {1}", dataPath, indexName);
            br.lines().forEach(line -> {
                Lethality lethality = new Lethality();
                String[] parts = line.split(";");
                String ibNumber = parts[0].strip();
                try {
                    lethality.dali11 = Float.parseFloat(parts[1]);
                } catch (NumberFormatException e) {}
                try {
                    lethality.dali22 = Float.parseFloat(parts[2]);
                } catch (NumberFormatException e) {}
                try {
                    lethality.dapi11 = Float.parseFloat(parts[3]);
                } catch (NumberFormatException e) {}
                ingester.add(op -> op
                    .index(idx -> idx
                                .index(indexName)
                                .id(ibNumber)
                                .document(lethality)
                    )
                );
            });
        }
        esClient.indices().refresh(r -> r.index(indexName));
        Log.infov("Loaded lethalities for {0} iB numbers", esClient.count(r -> r.index(indexName)).count());
    }
}
