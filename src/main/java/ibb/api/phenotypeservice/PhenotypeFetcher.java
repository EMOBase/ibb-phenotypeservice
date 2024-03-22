package ibb.api.phenotypeservice;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PhenotypeFetcher {
    @ConfigProperty(name = "phenotypeservice.elasticsearch.index-prefix")
    String indexPrefix;
    
    @Inject
    ElasticsearchClient esClient;

    private String indexName;

    @PostConstruct
    void init() {
        indexName = indexPrefix + "-*";
    }

    public static class SearchResult<T> {
        public List<T> data;
        public long total;
    }

    public List<Phenotype> findByGene(String gene) {
        try {
            return esClient.search(s -> s
                .index(indexName)
                .size(10000)
                .query(q -> q
                    .term(t -> t
                        .field("gene.keyword")
                        .value(gene)))
            , Phenotype.class)
            .hits().hits().stream().map(this::mapToPhenotype).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Phenotype findById(String id) {
        GetResponse<Phenotype> response;
        try {
            response = esClient.get(r -> r.index(indexName).id(id), Phenotype.class);
            if (!response.found()) {
                return null;
            }
            return mapToPhenotype(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Phenotype> findByIdOptional(String id) {
        return Optional.ofNullable(findById(id));
    }

    public SearchResult<Phenotype> search(String query, int from, int size) {
        try {
            SearchResponse<Phenotype> response = esClient.search(s -> s
                .index(indexName)
                .size(size)
                .from(from)
                .query(q -> q.multiMatch(m -> m
                    .fields(
                        "gene.keyword", 
                        "description", 
                        "dsRNA.name.keyword",
                        "structure.termId.keyword",
                        "structure.termName",
                        "process")
                    .query(query))),
                 Phenotype.class);
            SearchResult<Phenotype> result = new SearchResult<>();
            result.data = response.hits().hits().stream().map(this::mapToPhenotype).toList();
            result.total = Optional.ofNullable(response.hits().total()).map(t -> t.value()).orElseThrow(() -> new RuntimeException("No total in response"));
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Phenotype mapToPhenotype(Hit<Phenotype> hit) {
        Phenotype phenotype = Optional.ofNullable(hit.source()).orElseGet(Phenotype::new);
        phenotype.id = hit.id();
        return phenotype;
    }

    private Phenotype mapToPhenotype(GetResponse<Phenotype> response) {
        Phenotype phenotype = Optional.ofNullable(response.source()).orElseGet(Phenotype::new);
        phenotype.id = response.id();
        return phenotype;
    }
}
