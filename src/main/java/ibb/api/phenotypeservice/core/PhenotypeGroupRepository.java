package ibb.api.phenotypeservice.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import ibb.api.phenotypeservice.core.Phenotype.ImageMetadata;
import ibb.api.phenotypeservice.helper.ESHelper;
import ibb.api.phenotypeservice.search.PhenotypeGroupSearchResult;
import ibb.api.phenotypeservice.search.PhenotypeGroupSearchResultConverter;
import ibb.api.phenotypeservice.search.queryparser.QueryParser;
import ibb.api.phenotypeservice.search.queryparser.QueryParserException;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PhenotypeGroupRepository {
    
    @ConfigProperty(name = "phenotypeservice.elasticsearch.index-prefix")
    String indexPrefix;

    @ConfigProperty(name = "phenotypeservice.elasticsearch.delete-index-on-start", defaultValue = "false")
    boolean deleteIndexOnStart;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ElasticsearchClient esClient;

    @Inject
    @RestClient
    ImageServiceClient imageServiceClient;

    @Inject
    IBeetlePhenotypeLoader phenotypeGroupLoader;

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

    public PhenotypeGroupSearchResult search(String queryString) throws QueryParserException, IOException {
        Query query = new QueryParser().parse(queryString);
        Log.debug(ESHelper.serializeToJSON(query));
        var response = esClient.search(r -> r
            .index(indexName)
            .source(s -> s.filter(f -> f.includes("gene")))
            .query(query), PhenotypeGroup.class);
        var converter = new PhenotypeGroupSearchResultConverter();
        return converter.convert(response);
    }

    public Phenotype addPhenotype(Phenotype phenotype, List<File> images) throws IOException {
        return null;
    }

    public void deletePhenotype(String id) throws IOException {
    }

    public List<Phenotype> findByGene(String gene) throws IOException {
        return null;
    }

    private void fillImageMetadata(List<Phenotype> phenotypes) {
        Map<String, List<Phenotype>> imageIdToPhenotypes = phenotypes.stream()
            .flatMap(p -> p.imageIds.stream().map(i -> Map.entry(i, p)))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    
        List<String> imageIds = imageIdToPhenotypes.keySet().stream().toList();
        List<ImageMetadata> imageMetadatas = imageServiceClient.getMetadata(imageIds);

        imageMetadatas.stream()
            .filter(im -> imageIdToPhenotypes.containsKey(im.id))
            .forEach(im -> {
                imageIdToPhenotypes.get(im.id).forEach(p -> {
                    if (p.images == null) {
                        p.images = new ArrayList<>();
                    }
                    p.images.add(im);
                });
            });
    }

    private void createIndexIfNotExists() throws IOException {
        String indexName = getIndexName();
        if (esClient.indices().exists(r -> r.index(indexName)).value()) {
            Log.infov("Index {0} already exists, skipping loading data", indexName);
            return;
        }
        esClient.indices().create(r -> r.index(indexName).mappings(m -> m
            .properties("phenotypes", p -> p.nested(n -> n))
        ));
        phenotypeGroupLoader.loadIBeetlePhenotypes(indexName);
    }

    private String getIndexName() {
        return indexPrefix + "-phenotypegroups";
    }
}
