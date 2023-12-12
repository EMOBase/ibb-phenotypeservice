package ibb.api.phenotypeservice;

import java.io.File;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/images")
@RegisterRestClient(configKey = "image-service")
public interface ImageServiceClient {

    public static class ImageMetadata {
        public Long id;
    }
    
    @POST
    public List<ImageMetadata> upload(@RestForm("images") List<File> images);

    @DELETE
    public void delete(@RestQuery("ids") List<Long> ids);
}
