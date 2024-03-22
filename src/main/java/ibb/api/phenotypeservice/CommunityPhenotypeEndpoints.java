package ibb.api.phenotypeservice;

import java.io.File;
import java.util.List;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import ibb.api.phenotypeservice.CommunityPhenotypeRepository.SearchResult;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.ConvertGroup;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/community-phenotypes")
public class CommunityPhenotypeEndpoints {

    // @Provider
    // public static class CustomExceptionMapper implements ExceptionMapper<Exception> {
    //     @Override
    //     public Response toResponse(Exception exception) {
    //         Log.error(exception.getMessage(), exception);
    //         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    //     }
    // }

    @Inject
    CommunityPhenotypeRepository phenotypeRepository;

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
        Phenotype phenotype) {
        
        return phenotypeRepository.create(phenotype, images);
    }

    @GET
    @Path("/by-gene/{gene}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Phenotype> getByGene(@RestPath String gene) {
        return phenotypeRepository.findByGene(gene);
    }

    @DELETE
    @Path("/{id}")
    public void delete(@RestPath String id) {
        Phenotype existing = phenotypeRepository.findById(id);
        if (existing == null) {
            throw new NotFoundException();
        }
        phenotypeRepository.delete(existing);
    }

    @GET
    @Path("/search")
    public SearchResult<Phenotype> search(@NotBlank @RestQuery String query, @DefaultValue("0") @RestQuery int from, @DefaultValue("10") @RestQuery int size) {
        return phenotypeRepository.search(query, from, size);
    }
}
