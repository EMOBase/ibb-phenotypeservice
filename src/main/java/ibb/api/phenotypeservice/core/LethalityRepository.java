package ibb.api.phenotypeservice.core;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import ibb.api.phenotypeservice.loader.IBeetleLethalityLoader;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class LethalityRepository {
    
    @Inject
    ElasticsearchClient esClient;

    @ConfigProperty(name = "phenotypeservice.elasticsearch.index-prefix")
    String indexPrefix;

    @ConfigProperty(name = "phenotypeservice.elasticsearch.delete-index-on-start", defaultValue = "false")
    boolean deleteIndexOnStart;

    @ConfigProperty(name = "data.load-ibeetle", defaultValue = "true")
    boolean loadIBeetleData;

    @Inject
    IBeetleLethalityLoader lethalityLoader;

    private String indexName;

    @PostConstruct
    void init() {
        indexName = getIndexName();
    }

    void startup(@Observes StartupEvent ev) throws IOException {
        if (deleteIndexOnStart) {
            esClient.indices().delete(r -> r.index(indexName).ignoreUnavailable(true));
        }
        createIndexIfNotExists();
    }

    public Lethality findByIbNumber(String ibNumber) throws IOException {
        return esClient.get(r -> r.index(indexName).id(ibNumber), Lethality.class).source();
    }

    private void createIndexIfNotExists() throws IOException {
        if (esClient.indices().exists(r -> r.index(indexName)).value()) {
            Log.infov("Index {0} already exists, skipping loading data", indexName);
            return;
        }
        esClient.indices().create(r -> r.index(indexName));
        if (loadIBeetleData) {
            lethalityLoader.loadIBeetleLethality(indexName);
        }
    }

    public String getIndexName() {
        return indexPrefix + "-lethality";
    }
}
