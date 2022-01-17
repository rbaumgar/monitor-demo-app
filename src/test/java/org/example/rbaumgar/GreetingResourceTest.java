package org.example.rbaumgar;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import javax.ws.rs.core.MediaType;

@QuarkusTest
public class GreetingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from monitor-demo-app "));
    }

    @Test
    public void test2xxEndpoint() {
        given()
          .when().get("/hello/2xx")
          .then()
             .statusCode(200)
             .body(is("Got 2xx Response"));
    }

    @Test
    public void test5xxEndpoint() {
        given()
          .when().get("/hello/5xx")
          .then()
             .statusCode(200)
             .body(is("Got 5xx Response"));
    }

    @Test
    public void testalterEndpoint() {
        given()
          .contentType(ContentType.JSON)
          .body("{test}")
          .when().post("/hello/alert-hook")
          .then()
             .statusCode(200)
             .body(is("OK"));
    }

}