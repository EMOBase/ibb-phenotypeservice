package ibb.api.phenotypeservice.rest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ibb.api.phenotypeservice.core.ImageServiceClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(PhenotypeGroupEndpoints.class)
public class PhenotypeGroupEndpointsTest {

    @BeforeAll
    public static void setUp() throws IOException {
        ImageServiceClient imageService = new ImageServiceClientMock();
        QuarkusMock.installMockForType(imageService, ImageServiceClient.class, RestClient.LITERAL);
    }

    @Test
    public void testSearch() {
        RestAssured
            .when()
                .get("/search?query=wing")
            .then()
                .statusCode(200)
                .body("total", is(1))
                .body("data[0].gene", equalTo("TC000618"))
                .body("data[0].phenotypes.size()", equalTo(1));
    }
}
