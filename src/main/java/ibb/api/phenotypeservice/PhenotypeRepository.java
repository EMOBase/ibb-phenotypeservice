package ibb.api.phenotypeservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregationSource;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonpDeserializer;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PhenotypeRepository {

    public static class QueryResult<T> {
        public List<T> data;
        public long total;

        public static <T> QueryResult<T> of(SearchResponse<T> response) {
            QueryResult<T> result = new QueryResult<>();
            result.data = response.hits().hits().stream()
                .map(s -> s.source()).toList();
            result.total = Optional.ofNullable(response.hits().total())
                .map(t -> t.value())
                .orElseThrow(() -> new RuntimeException("No total in response"));
            return result;
        }
    }

    public static class PhenotypeAggregation extends QueryResult<Phenotype> {
        public String gene;
    }

    @ConfigProperty(name = "phenotypeservice.elasticsearch.index-prefix")
    String indexPrefix;

    @ConfigProperty(name = "data.dir")
    String dataDir;

    @Inject
    @RestClient
    ImageServiceClient imageServiceClient;

    @Inject
    ElasticsearchClient esClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JsonpDeserializer<Phenotype> phenotypeDeserializer;

    void init(@Observes StartupEvent event) throws IOException {
        initPipeline();
        initIndexTemplate();
        loadIBeetlePhenotypes();
    }

    public void search(String query) throws IOException {
        var response = esClient.search(s -> s
            .size(0)
            .query(q -> q
                .queryString(qs -> qs.query(query))
            )
            .aggregations("genes", a -> a
                .composite(c -> c
                    .sources(List.of(
                        Map.of("gene", CompositeAggregationSource.of(cs -> cs.terms(tt -> tt.field("gene.keyword"))))
                    ))
                )
                .aggregations("top_phenotypes", aa -> aa.topHits(th -> th.size(10)))
                .aggregations("max_score", aa -> aa
                    .max(m -> m
                        .script(ss -> ss.inline(i -> i.source("_score")))
                    )
                )
                .aggregations("sort", aa -> aa
                    .bucketSort(bs -> bs.sort(SortOptions.of(so -> so
                        .field(f -> f.field("max_score").order(SortOrder.Desc))
                    )))
                )
            )
        , Phenotype.class);
        var genes = response.aggregations().get("genes").composite();
        var searchResult = new QueryResult<PhenotypeAggregation>();
        searchResult.total = Optional.ofNullable(response.hits().total())
            .map(t -> t.value())
            .orElseThrow(() -> new RuntimeException("No total in response"));
        searchResult.data = genes.buckets().array().stream()
            .map(b -> {
                var agg = new PhenotypeAggregation();
                agg.gene = b.key().get("gene").stringValue();
                agg.total = b.docCount();
                agg.data = b.aggregations().get("top_phenotypes").topHits().hits().hits().stream()
                    .map(s -> s.source())
                    .filter(s -> s != null)
                    .map(s -> s.deserialize(phenotypeDeserializer))
                    .toList();
                return agg;
            }).toList();
    }
    
    public Phenotype create(Phenotype phenotype, List<File> images) throws IOException {
        List<String> imageIds;
        if (images.size() > 0) {
            // Upload each image in a separate request because multiple images don't work.
            // Quarkus issue.
            imageIds = images.stream()
                .map(imageServiceClient::upload)
                .flatMap(List::stream)
                .map(metadata -> metadata.id).toList();
        } else {
            imageIds = List.of();
        }
        phenotype.imageIds = imageIds;
        esClient.index(i -> i
            .index(getCommunityIndex())
            .document(phenotype)
            .refresh(Refresh.True)
        );
        return phenotype;
    }

    public QueryResult<Phenotype> findByGene(String gene) throws IOException {
        var response = esClient.search(s -> s
            .index(getIndexAlias())
            .size(10000)
            .query(q -> q
                .term(t -> t
                    .field("gene.keyword")
                    .value(gene)))
        , Phenotype.class);
        return QueryResult.of(response);
    }

    public QueryResult<Phenotype> findPending(int from, int size) throws IOException {
        var response = esClient.search(s -> s
            .index(getIndexAlias())
            .from(from)
            .size(size)
            .query(q -> q
                .term(t -> t
                    .field("status.keyword")
                    .value("PENDING")
                )
            )
        , Phenotype.class);
        return QueryResult.of(response);
    }

    public void reject(List<String> ids) throws IOException {
        List<String> imageIds = findCommunityPhenotypeByIds(ids).data.stream()
            .map(p -> p.imageIds)
            .flatMap(List::stream)
            .toList();
        if (imageIds.size() > 0) {
            imageServiceClient.reject(imageIds);
        }
        esClient.deleteByQuery(d -> d
            .index(getCommunityIndex())
            .ignoreUnavailable(true)
            .query(q -> q.ids(i -> i.values(ids)))
            .refresh(true)
        );
    }

    public void approve(List<String> ids) throws IOException {
        List<String> imageIds = findCommunityPhenotypeByIds(ids).data.stream()
            .map(p -> p.imageIds)
            .flatMap(List::stream)
            .toList();
        if (imageIds.size() > 0) {
            imageServiceClient.approve(imageIds);
        }
        esClient.updateByQuery(d -> d
            .index(getCommunityIndex())
            .ignoreUnavailable(true)
            .query(q -> q.ids(i -> i.values(ids)))
            .script(s -> s.inline(i -> i.source("ctx._source.status = 'APPROVED'")))
            .refresh(true)
        );
    }

    private QueryResult<Phenotype> findCommunityPhenotypeByIds(List<String> ids) throws IOException {
        var response = esClient.search(s -> s
            .index(getCommunityIndex())
            .ignoreUnavailable(true)
            .size(10000)
            .query(q -> q.ids(i -> i.values(ids)))
        , Phenotype.class);
        return QueryResult.of(response);
    }

    private String getCommunityIndex() {
        return indexPrefix + "-community";
    }

    private String getIndexAlias() {
        return indexPrefix;
    }

    private void initIndexTemplate() throws IOException {
        esClient.indices().putIndexTemplate(t -> t
            .name(indexPrefix + "-template")
            .indexPatterns(indexPrefix + "-*")
            .template(tt -> tt
                .settings(s -> s.defaultPipeline(indexPrefix + "-pipeline"))
                .aliases(indexPrefix, a -> a)
            )
        );
    }

    private void initPipeline() throws IOException {
        esClient.ingest().putPipeline(p -> p
            .id(indexPrefix + "-pipeline")
            .processors(pr -> pr.set(s -> s
                .field("id")
                .copyFrom("_id")
                .override(false)
            ))
            .processors(pr -> pr.set(s -> s
                .field("_id")
                .copyFrom("id")
                .override(true)
            ))
            .processors(pr -> pr.set(s -> s
                .field("createdAt")
                .copyFrom("_ingest.timestamp")
                .override(false)
            ))
        );
    }

    private void loadIBeetlePhenotypes() throws IOException {
        String indexName = indexPrefix + "-ibeetle";
        if (esClient.indices().exists(r -> r.index(indexName)).value()) {
            Log.infov("Index {0} already exists, skipping loading data", indexName);
            return;
        }

        Path dataPath = Paths.get(dataDir, "iBeetle_phenotypes.ndjson");
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
                    phenotype.iBeetle = true;
                    phenotype.status = Phenotype.Status.APPROVED;
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
        esClient.indices().refresh(r -> r.index(indexName));
        Log.infov("Loaded {0} phenotypes", esClient.count(r -> r.index(indexName)).count());
    }
}
