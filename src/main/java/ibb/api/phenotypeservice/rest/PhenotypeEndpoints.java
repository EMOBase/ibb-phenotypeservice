package ibb.api.phenotypeservice.rest;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import ibb.api.phenotypeservice.core.Phenotype;
import ibb.api.phenotypeservice.core.PhenotypeGroupRepository;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.ConvertGroup;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/phenotypes")
public class PhenotypeEndpoints {
    
    @Inject
    PhenotypeGroupRepository pgRepo;

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
        return pgRepo.addPhenotype(phenotype, images);
    }

    @GET
    @Path("/by-gene/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Phenotype> getByGene(@RestPath @NotBlank String gene) throws IOException {
        return pgRepo.findByGene(gene);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@RestPath String id) throws IOException {
        pgRepo.deletePhenotype(id);
    }
}
