package ibb.api.phenotypeservice.core;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import ibb.api.phenotypeservice.core.Phenotype.ImageMetadata;
import ibb.api.phenotypeservice.helper.ESHelper;
import ibb.api.phenotypeservice.loader.IBeetlePhenotypeLoader;
import ibb.api.phenotypeservice.loader.SynonymLoader;
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

    @ConfigProperty(name = "data.load-ibeetle", defaultValue = "true")
    boolean loadIBeetleData;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ElasticsearchClient esClient;

    @Inject
    @RestClient
    ImageServiceClient imageServiceClient;

    @Inject
    IBeetlePhenotypeLoader phenotypeGroupLoader;

    @Inject
    SynonymLoader synonymLoader;

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

    public PhenotypeGroupSearchResult search(String queryString, Float penetrance, int from, int size) throws QueryParserException, IOException {
        Query query = new QueryParser().withDefaultPenetrance(penetrance).parse(queryString);
        Log.debug(ESHelper.serializeToJSON(query));
        var response = esClient.search(r -> r
                .index(indexName)
                .from(from)
                .size(size)
                .source(s -> s.filter(f -> f.includes("gene")))
                .query(query), PhenotypeGroup.class);
        var converter = new PhenotypeGroupSearchResultConverter();
        var result = converter.convert(response);
        List<Phenotype> phenotypes = result.data.stream().flatMap(pg -> pg.phenotypes.stream()).toList();
        fillImageMetadata(phenotypes);
        return result;
    }

    public Phenotype addPhenotype(Phenotype phenotype, List<File> images) throws IOException {
        List<ImageMetadata> imageMetadatas;
        if (images != null && !images.isEmpty()) {
            imageMetadatas = images.stream()
                .map(imageServiceClient::upload)
                .flatMap(List::stream)
                .toList();
        } else {
            imageMetadatas = List.of();
        }
        phenotype.id = UUID.randomUUID().toString();
        phenotype.createdAt = Instant.now();
        String gene = phenotype.gene;

        phenotype.gene = null;
        phenotype.imageIds = imageMetadatas.stream().map(im -> im.id).toList();
        PhenotypeGroup newPhenotypeGroup = new PhenotypeGroup();
        newPhenotypeGroup.gene = gene;
        newPhenotypeGroup.phenotypes = List.of(phenotype);

        esClient.update(u -> u
            .index(indexName)
            .id(gene)
            .refresh(Refresh.True)
            .script(s -> s.inline(i -> i
                .source("ctx._source.phenotypes.add(params.phenotype)")
                .params("phenotype", JsonData.of(phenotype))))
            .upsert(newPhenotypeGroup)
        , PhenotypeGroup.class);

        phenotype.imageIds = null;
        phenotype.images = imageMetadatas;
        return phenotype;
    }

    public void deletePhenotype(String id) throws IOException {
        List<String> imageIds = esClient.search(r -> r
            .index(indexName)
            .query(q -> q.nested(n -> n
                .path("phenotypes")
                .query(q2 -> q2.term(t -> t.field("phenotypes.id.keyword").value(id))))
            )
            , PhenotypeGroup.class)
            .hits().hits().stream()
            .map(h -> h.source())
            .filter(pg -> pg != null && pg.phenotypes != null)
            .flatMap(pg -> pg.phenotypes.stream())
            .filter(p -> Objects.equals(p.id, id) && p.imageIds != null)
            .flatMap(p -> p.imageIds.stream())
            .toList();
        if (!imageIds.isEmpty()) {
            imageServiceClient.reject(imageIds);
        }
        
        esClient.updateByQuery(u -> u
            .index(indexName)
            .refresh(true)
            .query(q -> q.nested(n -> n
                .path("phenotypes")
                .query(q2 -> q2.term(t -> t.field("phenotypes.id.keyword").value(id)))))
            .script(s -> s.inline(i -> i
                .source("ctx._source.phenotypes.removeIf(p -> p.id == params.id)")
                .params("id", JsonData.of(id))))
        );
    }

    public List<Phenotype> findByGene(String gene) throws IOException {
        var pg = esClient.get(g -> g.id(gene).index(indexName), PhenotypeGroup.class)
            .source();
        if (pg == null) {
            return List.of();
        }
        fillImageMetadata(pg.phenotypes);
        return pg.phenotypes;
    }

    private void fillImageMetadata(List<Phenotype> phenotypes) {
        Map<String, List<Phenotype>> imageIdToPhenotypes = phenotypes.stream()
                .flatMap(p -> p.imageIds.stream().map(i -> Map.entry(i, p)))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        List<String> imageIds = imageIdToPhenotypes.keySet().stream().toList();
        if (imageIds.isEmpty()) {
            return;
        }

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
        List<String> synonyms = synonymLoader.getSynonyms();
        esClient.indices().create(r -> r.index(indexName)
                .settings(s -> s
                        .analysis(a -> a
                                .filter("synonym_filter",
                                        f -> f.definition(d -> d.synonymGraph(sg -> sg.synonyms(synonyms))))
                                .analyzer("phenotype_search_analyzer", aa -> aa.custom(c -> c
                                        .tokenizer("standard")
                                        .filter("lowercase", "synonym_filter")))))
                .mappings(m -> m
                        .properties("phenotypes", p -> p.nested(n -> n))
                ));
        if (loadIBeetleData) {
            phenotypeGroupLoader.loadIBeetlePhenotypes(indexName);
        }
    }

    public String getIndexName() {
        return indexPrefix + "-phenotypegroups";
    }
}
