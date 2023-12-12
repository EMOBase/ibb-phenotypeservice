package ibb.api.phenotypeservice;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PhenotypeRepository {

    @ConfigProperty(name = "phenotypeservice.elasticsearch.index-name")
    String indexName;
    
    @Inject
    ElasticsearchClient esClient;

    @Inject
    @RestClient
    ImageServiceClient imageServiceClient;

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

    public Phenotype create(Phenotype phenotype, List<File> images) {
        List<Long> imageIds = null;
        if (images.size() > 0) {
            imageIds = imageServiceClient.upload(images).stream().map(metadata -> metadata.id).toList();
        } else {
            imageIds = List.of();
        }
        phenotype.imageIds = imageIds;
        try {
            IndexResponse response = esClient.index(i -> i.index(indexName).document(phenotype));
            phenotype.id = response.id();
            return phenotype;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(Phenotype phenotype) {
        try {
            imageServiceClient.delete(phenotype.imageIds);
            esClient.delete(d -> d.index(indexName).id(phenotype.id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteIndex() {
        try {
            esClient.indices().delete(d -> d.index(indexName).ignoreUnavailable(true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void refresh() {
        try {
            esClient.indices().refresh(r -> r.index(indexName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
