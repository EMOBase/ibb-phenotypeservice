package ibb.api.phenotypeservice.rest;

import java.io.IOException;

import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.phenotypeservice.core.PhenotypeGroupRepository;
import ibb.api.phenotypeservice.search.PhenotypeGroupSearchResult;
import ibb.api.phenotypeservice.search.queryparser.QueryParserException;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/phenotype-groups")
public class PhenotypeGroupEndpoints {
    
    @Inject
    PhenotypeGroupRepository phenotypeGroupRepository;

    @GET
    @Path("/search")
    public PhenotypeGroupSearchResult search(@RestQuery @NotBlank String query,
        @RestQuery @DefaultValue("0.5") float penetrance,
        @RestQuery @DefaultValue("0") int from,
        @RestQuery @DefaultValue("20") int size
    ) throws IOException {
        try {
            return phenotypeGroupRepository.search(query, penetrance, from, size);
        } catch (QueryParserException e) {
            throw new BadRequestException("query is invalid", e);
        }
    }
}
