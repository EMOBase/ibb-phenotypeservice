package ibb.api.phenotypeservice.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class PhenotypeGroupRepositoryTest {

    @Inject
    PhenotypeGroupRepository phenotypeGroupRepository;

    @Inject
    ElasticsearchClient esClient;

    @Test
    public void shouldLoadIBeetleData() throws IOException {
        List<PhenotypeGroup> pgs = esClient.search(s -> s
            .index(phenotypeGroupRepository.getIndexName())
        , PhenotypeGroup.class).hits().hits().stream().map(h -> h.source()).toList();

        assertThat(pgs.size(), equalTo(2));;
    }
}
