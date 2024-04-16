package ibb.api.phenotypeservice.rest;

import java.io.File;
import java.util.List;
import java.util.UUID;

import ibb.api.phenotypeservice.core.ImageServiceClient;
import ibb.api.phenotypeservice.core.Phenotype.ImageMetadata;

public class ImageServiceClientMock implements ImageServiceClient {
    
    @Override
    public List<ImageMetadata> upload(File file) {
        ImageMetadata metadata = new ImageMetadata();
        metadata.id = UUID.randomUUID().toString();
        return List.of(metadata);
    }

    @Override
    public List<ImageMetadata> getMetadata(List<String> ids) {
        return ids.stream().map(id -> {
            ImageMetadata metadata = new ImageMetadata();
            metadata.id = id;
            metadata.status = ImageMetadata.Status.APPROVED;
            return metadata;
        }).toList();
    }

    @Override
    public void reject(List<String> ids) {
    }
}
