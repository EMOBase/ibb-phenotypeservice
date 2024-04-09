package ibb.api.phenotypeservice;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.phenotypeservice.PhenotypeRepository.PhenotypeAggregationQueryResult;
import ibb.api.phenotypeservice.PhenotypeRepository.QueryResult;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.groups.ConvertGroup;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/phenotypes")
public class PhenotypeEndpoints {
    
    @Inject
    PhenotypeRepository phenotypeRepository;

    @GET
    @Path("/search")
    public PhenotypeAggregationQueryResult search(
        @RestQuery @NotBlank String query,
        @RestQuery String searchAfter
    ) throws IOException {
        return phenotypeRepository.search(query, searchAfter);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Phenotype create(
        @RestForm("images")
        List<File> images,

        @RestForm
        @PartType(MediaType.APPLICATION_JSON)
        @Valid
        @ConvertGroup(to = Phenotype.ValidationGroups.Create.class)
        Phenotype phenotype
    ) throws IOException { 
        return phenotypeRepository.create(phenotype, images);
    }

    @GET
    @Path("/by-gene/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    public QueryResult<Phenotype> getByGene(@RestPath @NotBlank String gene) throws IOException {
        return phenotypeRepository.findByGene(gene);
    }

    @GET
    @Path("/pending")
    @Produces(MediaType.APPLICATION_JSON)
    public QueryResult<Phenotype> getPending(
        @RestQuery @DefaultValue("0") int from,
        @RestQuery @DefaultValue("20") int size
    ) throws IOException {
        return phenotypeRepository.findPending(from, size);
    }

    @POST
    @Path("/reject")
    public void reject(@NotEmpty List<String> ids) throws IOException {
        phenotypeRepository.reject(ids);
    }

    @POST
    @Path("/approve")
    public void approve(@NotEmpty List<String> ids) throws IOException {
        phenotypeRepository.approve(ids);
    }
}
