package ibb.api.phenotypeservice;

import java.io.File;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import ibb.api.phenotypeservice.models.Phenotype;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/phenotypes")
public class PhenotypeEndpoints {

    @Inject
    @RestClient
    ImageService imageService;

    @POST
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Phenotype create(@RestForm("images") List<File> images,
        @RestForm @PartType(MediaType.APPLICATION_JSON) @Valid Phenotype phenotype) {

        List<Long> imageIds = imageService.upload(images).stream().map(metadata -> metadata.id).toList();
        phenotype.imageIds = imageIds;
        phenotype.persist();
        return phenotype;
    }

    @GET
    @Path("/by-gene/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Phenotype> getByGene(@RestPath String gene) {
        List<Phenotype> phenotypes = Phenotype.list("gene", gene);
        return phenotypes;
    }

    @DELETE
    @Path("/{id}")
    public void delete(@RestPath Long id) {
        Phenotype existing = Phenotype.findById(id);
        if (existing == null) {
            throw new NotFoundException();
        }
        existing.delete();
    }
}
