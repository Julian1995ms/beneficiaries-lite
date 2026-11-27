package mx.com.empresa.backend.infrastructure.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BeneficiaryResourceTest {

    private static final String BASE_PATH = "/api/v1/beneficiaries";

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        QuarkusTransaction.requiringNew().run(() -> entityManager.createQuery("DELETE FROM BeneficiaryEntity").executeUpdate());
    }

    @Test
    void shouldCreateBeneficiaryAndReturn201() {
        given()
                .contentType(ContentType.JSON)
                .body(validRequest("12345678", generateClabe(1)))
                .when().post(BASE_PATH)
                .then()
                .statusCode(201)
                .header("Location", org.hamcrest.Matchers.matchesPattern(".*/api/v1/beneficiaries/[0-9]+$"))
                .body("customerId", equalTo("12345678"))
                .body("bankCode", equalTo("032"))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    void shouldCreateBeneficiaryWithoutEmail() {
        Map<String, Object> request = validRequest("12345678", generateClabe(2));
        request.remove("email");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("email", org.hamcrest.Matchers.nullValue());
    }

    @Test
    void shouldRejectInvalidBodyAndReturnValidationDetails() {
        Map<String, Object> request = validRequest("123", generateClabe(3));
        request.put("alias", "x");
        request.put("maxAmount", BigDecimal.ZERO);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("code", equalTo("BEN-400"))
                .body("message", equalTo("La solicitud contiene datos inválidos"))
                .body("details.field", containsInAnyOrder("customerId", "alias", "maxAmount"));
    }

    @Test
    void shouldReturn404WhenIdDoesNotExist() {
        given()
                .when().get(BASE_PATH + "/999")
                .then()
                .statusCode(404)
                .body("code", equalTo("BEN-404"));
    }

    @Test
    void shouldRejectInvalidClabeVerifierDigit() {
        Map<String, Object> request = validRequest("12345678", "032180000118359718");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("code", equalTo("BEN-400"))
                .body("message", equalTo("CLABE inválida"));
    }

    @Test
    void shouldRejectDuplicateClabeForSameCustomer() {
        String clabe = generateClabe(4);
        create("12345678", clabe);

        given()
                .contentType(ContentType.JSON)
                .body(validRequest("12345678", clabe))
                .when().post(BASE_PATH)
                .then()
                .statusCode(409)
                .body("code", equalTo("BEN-409"));
    }

    @Test
    void shouldAllowSameClabeForDifferentCustomers() {
        String clabe = generateClabe(5);
        create("12345678", clabe);

        given()
                .contentType(ContentType.JSON)
                .body(validRequest("87654321", clabe))
                .when().post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    void shouldRejectDuplicateClabeWhenExistingBeneficiaryIsInactive() {
        String clabe = generateClabe(6);
        long id = create("12345678", clabe);
        deactivate(id);

        given()
                .contentType(ContentType.JSON)
                .body(validRequest("12345678", clabe))
                .when().post(BASE_PATH)
                .then()
                .statusCode(409);
    }

    @Test
    void shouldAllowTenthAndRejectEleventhBeneficiary() {
        for (int i = 1; i <= 9; i++) {
            create("12345678", generateClabe(100 + i));
        }

        given().contentType(ContentType.JSON).body(validRequest("12345678", generateClabe(110)))
                .when().post(BASE_PATH).then().statusCode(201);

        given().contentType(ContentType.JSON).body(validRequest("12345678", generateClabe(111)))
                .when().post(BASE_PATH).then().statusCode(422).body("code", equalTo("BEN-422"));
    }

    @Test
    void shouldListOnlyActiveBeneficiaries() {
        long first = create("12345678", generateClabe(201));
        create("12345678", generateClabe(202));
        deactivate(first);

        given()
                .queryParam("customerId", "12345678")
                .when().get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].status", equalTo("ACTIVE"));
    }

    @Test
    void shouldReturnEmptyArrayWhenCustomerHasNoActiveBeneficiaries() {
        given()
                .queryParam("customerId", "12345678")
                .when().get(BASE_PATH)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void shouldFindExistingBeneficiaryById() {
        long id = create("12345678", generateClabe(301));

        given()
                .when().get(BASE_PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("id", is((int) id));
    }

    @Test
    void shouldReturnInactiveBeneficiaryById() {
        long id = create("12345678", generateClabe(302));
        deactivate(id);

        given()
                .when().get(BASE_PATH + "/" + id)
                .then()
                .statusCode(200)
                .body("status", equalTo("INACTIVE"));
    }

    @Test
    void shouldDeactivateBeneficiaryWith204() {
        long id = create("12345678", generateClabe(401));

        given()
                .when().patch(BASE_PATH + "/" + id + "/deactivate")
                .then()
                .statusCode(204);
    }

    @Test
    void shouldReturn404WhenDeactivatingMissingBeneficiary() {
        given()
                .when().patch(BASE_PATH + "/999/deactivate")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturn409OnSecondDeactivation() {
        long id = create("12345678", generateClabe(402));
        deactivate(id);

        given()
                .when().patch(BASE_PATH + "/" + id + "/deactivate")
                .then()
                .statusCode(409)
                .body("code", equalTo("BEN-409"));
    }

    @Test
    void shouldAllowDifferentClabeAfterLogicalDeletion() {
        long id = create("12345678", generateClabe(501));
        deactivate(id);

        given()
                .contentType(ContentType.JSON)
                .body(validRequest("12345678", generateClabe(502)))
                .when().post(BASE_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    void shouldValidateCustomerIdQueryParameter() {
        given()
                .queryParam("customerId", "123")
                .when().get(BASE_PATH)
                .then()
                .statusCode(400)
                .body("code", equalTo("BEN-400"))
                .body("details.field", org.hamcrest.Matchers.hasItem("customerId"));
    }

    @Test
    void shouldNormalizeAliasAndHolderNameBeforeValidation() {
        Map<String, Object> request = validRequest("12345678", generateClabe(601));
        request.put("alias", "   Alias válido   ");
        request.put("holderName", "   Titular de Prueba   ");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(BASE_PATH)
                .then()
                .statusCode(201)
                .body("alias", equalTo("Alias válido"))
                .body("holderName", equalTo("Titular de Prueba"));
    }

    @Test
    void shouldRejectWhitespaceOnlyAliasAfterNormalization() {
        Map<String, Object> request = validRequest("12345678", generateClabe(602));
        request.put("alias", "     ");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("details.field", org.hamcrest.Matchers.hasItem("alias"));
    }

    @Test
    void shouldRejectUnknownJsonProperty() {
        Map<String, Object> request = validRequest("12345678", generateClabe(701));
        request.put("id", 10);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("code", equalTo("BEN-400"));
    }

    @Test
    void shouldRejectMalformedJson() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"customerId\":\"12345678\"")
                .when().post(BASE_PATH)
                .then()
                .statusCode(400)
                .body("code", equalTo("BEN-400"));
    }

    @Test
    void shouldValidateMaxAmountScaleAndValue() {
        Map<String, Object> zero = validRequest("12345678", generateClabe(801));
        zero.put("maxAmount", BigDecimal.ZERO);
        given().contentType(ContentType.JSON).body(zero).when().post(BASE_PATH).then().statusCode(400);

        Map<String, Object> threeDecimals = validRequest("12345678", generateClabe(802));
        threeDecimals.put("maxAmount", new BigDecimal("1.234"));
        given().contentType(ContentType.JSON).body(threeDecimals).when().post(BASE_PATH).then().statusCode(400);
    }

    @Test
    void shouldReturnStandardErrorShape() {
        given()
                .when().get(BASE_PATH + "/999")
                .then()
                .statusCode(404)
                .body("code", equalTo("BEN-404"))
                .body("message", equalTo("Beneficiario no encontrado"))
                .body("timestamp", org.hamcrest.Matchers.notNullValue())
                .body("details", hasSize(0));
    }

    @Test
    void shouldExposeHealthCheck() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void shouldNotExceedTenActiveBeneficiariesUnderConcurrency() throws Exception {
        for (int i = 1; i <= 9; i++) {
            create("12345678", generateClabe(900 + i));
        }

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> concurrentCreate(start, "12345678", generateClabe(910)));
            Future<Integer> second = executor.submit(() -> concurrentCreate(start, "12345678", generateClabe(911)));
            start.countDown();

            List<Integer> statuses = new ArrayList<>(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)));
            Collections.sort(statuses);
            assertEquals(List.of(201, 422), statuses);

            given().queryParam("customerId", "12345678").when().get(BASE_PATH).then().statusCode(200).body("$", hasSize(10));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldPreventConcurrentDuplicateClabe() throws Exception {
        String clabe = generateClabe(1001);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> concurrentCreate(start, "12345678", clabe));
            Future<Integer> second = executor.submit(() -> concurrentCreate(start, "12345678", clabe));
            start.countDown();

            List<Integer> statuses = new ArrayList<>(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)));
            Collections.sort(statuses);
            assertEquals(List.of(201, 409), statuses);

            given().queryParam("customerId", "12345678").when().get(BASE_PATH).then().statusCode(200).body("$", hasSize(1));
        } finally {
            executor.shutdownNow();
        }
    }

    private int concurrentCreate(CountDownLatch start, String customerId, String clabe) throws InterruptedException {
        start.await(5, TimeUnit.SECONDS);
        return given()
                .contentType(ContentType.JSON)
                .body(validRequest(customerId, clabe))
                .when().post(BASE_PATH)
                .getStatusCode();
    }

    private long create(String customerId, String clabe) {
        return given()
                .contentType(ContentType.JSON)
                .body(validRequest(customerId, clabe))
                .when().post(BASE_PATH)
                .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private void deactivate(long id) {
        given().when().patch(BASE_PATH + "/" + id + "/deactivate").then().statusCode(204);
    }

    private Map<String, Object> validRequest(String customerId, String clabe) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId);
        request.put("alias", "Proveedor prueba");
        request.put("holderName", "Proveedor de Prueba SA de CV");
        request.put("clabe", clabe);
        request.put("maxAmount", new BigDecimal("50000.00"));
        request.put("email", "pagos@example.com");
        return request;
    }

    private String generateClabe(long sequence) {
        String base = "032" + String.format("%014d", sequence);
        int[] weights = {3, 7, 1};
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += ((base.charAt(i) - '0') * weights[i % weights.length]) % 10;
        }
        int verifier = (10 - (sum % 10)) % 10;
        return base + verifier;
    }
}
