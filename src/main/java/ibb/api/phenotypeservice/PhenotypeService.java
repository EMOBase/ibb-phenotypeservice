package ibb.api.phenotypeservice;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Path("/phenotypes")
public class PhenotypeService {

    @Provider
    public static class CustomExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception exception) {
            Log.error(exception.getMessage(), exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Inject
    @RestClient
    ImageService imageService;

    @Inject
    SearchSession searchSession;

    @Transactional
    void onStart(@Observes StartupEvent ev) throws InterruptedException {
        // only reindex if we imported some content
        if (Phenotype.count() > 0) {
            searchSession.massIndexer().startAndWait();
        }
    }

    @POST
    @Transactional
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
        
        List<Long> imageIds = null;
        if (images.size() > 0) {
            imageIds = imageService.upload(images).stream().map(metadata -> metadata.id).toList();
        }
        phenotype.imageIds = imageIds;
        Phenotype.persist(phenotype);
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
    @Transactional
    public void delete(@RestPath Long id) {
        Phenotype existing = Phenotype.findById(id);
        if (existing == null) {
            throw new NotFoundException();
        }
        imageService.delete(existing.imageIds);
        existing.delete();
    }

    @GET
    @Path("/search")
    @Transactional
    public List<Phenotype> search(@RestQuery String pattern, @RestQuery Optional<Integer> size) {
        return searchSession.search(Phenotype.class)
            .where(f ->
                pattern == null || pattern.trim().isEmpty() ?
                    f.matchAll() :
                    f.simpleQueryString()
                        .fields(
                            "gene", 
                            "description", 
                            "dsRNA.name",
                            "structure.termId",
                            "structure.termName",
                            "process"
                        )
                        .matching(pattern)
            )
            .fetchHits(size.orElse(20));
    }
}
