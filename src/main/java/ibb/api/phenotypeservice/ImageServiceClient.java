package ibb.api.phenotypeservice;

import java.io.File;
import java.util.List;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "image-service")
@RegisterClientHeaders
@Path("/")
public interface ImageServiceClient {

    public static class ImageMetadata {
        public String id;
    }
    
    @POST
    @Path("/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public List<ImageMetadata> upload(@RestForm File images);

    @DELETE
    @Path("/image-metadata")
    public void delete(@RestQuery("ids") String commaSeparatedIds);
}
