package ibb.api.phenotypeservice.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import ibb.api.phenotypeservice.core.ImageServiceClient;
import ibb.api.phenotypeservice.core.Phenotype;
import ibb.api.phenotypeservice.core.Phenotype.OntologyTerm;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(PhenotypeEndpoints.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PhenotypeEndpointsTest {

    @BeforeAll
    public static void setUp() throws IOException {
        ImageServiceClient imageService = new ImageServiceClientMock();
        QuarkusMock.installMockForType(imageService, ImageServiceClient.class, RestClient.LITERAL);
    }

    private static class Timeline {
        public static final int INITIAL = 1;
        public static final int BEFORE_CREATE_PHENOTYPE = 2;
        public static final int AFTER_CREATE_PHENOTYPE = 3;
        public static final int BEFORE_DELETE_PHENOTYPE = 4;
        public static final int AFTER_DELETE_PHENOTYPE = 5;
    }

    static String phenotypeId;

    @Test
    @Order(Timeline.INITIAL)
    public void shouldFindNoPhenotypes() {
        RestAssured
            .given()
                .pathParam("gene", "TC003413")
            .when()
                .get("/by-gene/{gene}")
            .then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    @Order(Timeline.BEFORE_CREATE_PHENOTYPE)
    public void shouldCreatePhenotype() {
        Phenotype phenotype = createValidPhenotype();
        File[] images = createTempImageFiles(2);
        phenotypeId = RestAssured
            .given()
                .multiPart("phenotype", phenotype, "application/json")
                .multiPart("images", images[0], "image/jpeg")
                .multiPart("images", images[1], "image/jpeg")
            .when()
                .post()
            .then()
                .body("violations", nullValue())
                .statusCode(200)
                .body("id", notNullValue())
            .extract()
                .as(Phenotype.class).id;
    }

    @Test
    @Order(Timeline.AFTER_CREATE_PHENOTYPE)
    public void shouldFindPhenotypesByGene() {
    
        RestAssured
            .given()
                .pathParam("gene", "TC003413")
            .when()
                .get("/by-gene/{gene}")
            .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", equalTo(phenotypeId))
                .body("[0].dsRNA.sequence", equalTo("ATGCG"))
                .body("[0].imageIds.size()", equalTo(2));
    }

    @Test
    @Order(Timeline.BEFORE_DELETE_PHENOTYPE)
    public void shouldDeletePhenotype() {
        RestAssured
            .given()
                .pathParam("id", phenotypeId)
            .when()
                .delete("/{id}")
            .then()
                .statusCode(204);
    }

    @Test
    @Order(Timeline.AFTER_DELETE_PHENOTYPE)
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
        phenotype.structures = List.of(structure);
        phenotype.process = "head development";
        phenotype.penetrance = 0.5f;
        phenotype.numberOfAnimals = 10;

        return phenotype;
    }
}
