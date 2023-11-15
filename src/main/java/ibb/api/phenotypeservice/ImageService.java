package ibb.api.phenotypeservice;

import java.io.File;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import jakarta.ws.rs.POST;

@RegisterRestClient
public interface ImageService {

    public static class ImageMetadata {
        public Long id;
    }
    
    @POST
    public List<ImageMetadata> upload(@RestForm("images") List<File> images);
}
