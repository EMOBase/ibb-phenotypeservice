package ibb.api.phenotypeservice.core;

import java.io.File;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import ibb.api.phenotypeservice.core.Phenotype.ImageMetadata;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@RegisterRestClient(configKey = "image-service")
public interface ImageServiceClient {

    @POST
    @Path("/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public List<ImageMetadata> upload(@RestForm File images);

    @POST
    @Path("/image-metadata/get")
    public List<ImageMetadata> getMetadata(List<String> ids);

    @POST
    @Path("/image-metadata/reject")
    public void reject(List<String> ids);
}
