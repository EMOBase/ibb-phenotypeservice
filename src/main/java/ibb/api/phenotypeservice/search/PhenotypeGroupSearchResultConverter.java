package ibb.api.phenotypeservice.search;

import java.util.Optional;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import ibb.api.phenotypeservice.core.Phenotype;
import ibb.api.phenotypeservice.core.PhenotypeGroup;
import ibb.api.phenotypeservice.helper.StreamHelper;

public class PhenotypeGroupSearchResultConverter {
    
    public PhenotypeGroupSearchResult convert(SearchResponse<PhenotypeGroup> searchResponse) {
        var result = new PhenotypeGroupSearchResult();
        result.total = Optional.ofNullable(searchResponse.hits().total()).map(t -> t.value()).orElseThrow();
        result.data = searchResponse.hits().hits().stream().map(hit -> {
            PhenotypeGroup pg = new PhenotypeGroup();
            pg.gene = Optional.ofNullable(hit.source()).map(h -> h.gene).orElse(null);
            pg.phenotypes = hit.innerHits().values().stream()
                .flatMap(innerHit -> innerHit.hits().hits().stream())
                .map(h -> h.source())
                .map(source -> source.to(Phenotype.class))
                .filter(StreamHelper.distinctByKey(p -> p.id))
                .toList();

            return pg;
        }).toList();
        return result;
    }
}
