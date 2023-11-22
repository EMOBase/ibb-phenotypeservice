package ibb.api.phenotypeservice;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import ibb.api.phenotypeservice.Phenotype.OntologyTerm;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(PhenotypeService.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PhenotypeServiceTest {

    @BeforeAll
    public static void setUp() throws IOException {
        ImageService imageService = new ImageService() {
            @Override
            public List<ImageMetadata> upload(List<File> images) {
                return LongStream.rangeClosed(1, images.size())
                    .mapToObj(i -> {
                        ImageMetadata metadata = new ImageMetadata();
                        metadata.id = (long) i;
                        return metadata;
                    })
                    .toList();
            }

            @Override
            public void delete(List<Long> ids) {}
        };
        QuarkusMock.installMockForType(imageService, ImageService.class, RestClient.LITERAL);
    }

    @Test
    @Order(1)
    public void shouldCreatePhenotype() {
        Phenotype phenotype = createValidPhenotype();
        File[] images = createTempImageFiles(2);
        RestAssured
            .given()
                .multiPart("phenotype", phenotype, "application/json")
                .multiPart("images", images[0], "image/jpeg")
                .multiPart("images", images[1], "image/jpeg")
            .when()
                .post()
            .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("gene", equalTo("TC003413"));
    }

    @Test
    @Order(2)
    public void shouldFindPhenotypesByGene() {
    
        RestAssured
            .given()
                .pathParam("gene", "TC003413")
            .when()
                .get("/by-gene/{gene}")
            .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", equalTo(1))
                .body("[0].gene", equalTo("TC003413"))
                .body("[0].dsRNA.sequence", equalTo("ATGCG"))
                .body("[0].imageIds.size()", equalTo(2));
    }

    @Test
    @Order(2)
    public void shouldFindPhenotypesByPattern() {
        RestAssured
            .given()
                .queryParam("pattern", "head")
            .when()
                .get("/search")
            .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", equalTo(1));
    }

    @Test
    @Order(3)
    public void shouldDeletePhenotype() {
        RestAssured
            .given()
                .pathParam("id", 1)
            .when()
                .delete("/{id}")
            .then()
                .statusCode(204);
    }

    @Test
    @Order(4)
    public void shouldFindNoPhenotypesAfterDeleted() {      
        RestAssured
            .given()
                .pathParam("gene", "TC003413")
            .when()
                .get("/by-gene/{gene}")
            .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    private File[] createTempImageFiles(int size) {
        List<File> images = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            try {
                File image = Files.createTempFile("image", ".jpg").toFile();
                image.deleteOnExit();
                images.add(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return images.toArray(File[]::new);
    }

    private Phenotype createValidPhenotype() {
        Phenotype.DsRNA dsRNA = new Phenotype.DsRNA();
        dsRNA.sequence = "ATGCG";

        Phenotype.Reference reference = new Phenotype.Reference();
        reference.type = Phenotype.Reference.Type.LAB;
        reference.value = "Gregor Bucher";

        OntologyTerm structure = new OntologyTerm();
        structure.termId = "TrOn_0000168";
        structure.termName = "head";

        Phenotype phenotype = new Phenotype();
        phenotype.gene = "TC003413";
        phenotype.dsRNA = dsRNA;
        phenotype.concentration = 1.0f;
        phenotype.injectedStage = "L1";
        phenotype.injectedStrain = "Ga-2";
        phenotype.reference = reference;
        phenotype.description = "head shortened";
        phenotype.structure = structure;
        phenotype.process = "head development";
        phenotype.penetrance = 0.5f;
        phenotype.numberOfAnimals = 10;

        return phenotype;
    }
}
