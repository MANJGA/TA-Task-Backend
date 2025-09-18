package utils;

import io.restassured.RestAssured;

import static io.restassured.RestAssured.given;

public class WireMockAdminClient {
    private final String adminBaseUrl;

    public WireMockAdminClient() {
        String base = System.getProperty("wiremock.baseUrl", "http://localhost:8080");
        this.adminBaseUrl = base + "/__admin";
    }

    public void resetAll() {
        given().post(adminBaseUrl + "/mappings/reset").then().statusCode(200);
    }

    public void addStub(String mappingJson) {
        given()
                .header("Content-Type", "application/json")
                .body(mappingJson)
                .post(adminBaseUrl + "/mappings")
                .then().statusCode(201);
    }

    public void seedUsersStubs() {
        // Positive: GET /users returns two users
        addStub("""
        {
          "request": { "method": "GET", "urlPath": "/users" },
          "response": {
            "status": 200,
            "headers": { "Content-Type": "application/json" },
            "jsonBody": [
              { "id": 1, "name": "Alice", "age": 30, "gender": "female" },
              { "id": 2, "name": "Bob",   "age": 25, "gender": "male"   }
            ]
          }
        }
        """);

        // Filter by age: /users?age=30 -> only Alice
        addStub("""
        {
          "request": {
            "method": "GET",
            "urlPath": "/users",
            "queryParameters": {
              "age": { "equalTo": "30" }
            }
          },
          "response": {
            "status": 200,
            "headers": { "Content-Type": "application/json" },
            "jsonBody": [ { "id": 1, "name": "Alice", "age": 30, "gender": "female" } ]
          }
        }
        """);

        // Filter by gender=male -> only Bob
        addStub("""
        {
          "request": {
            "method": "GET",
            "urlPath": "/users",
            "queryParameters": {
              "gender": { "equalTo": "male" }
            }
          },
          "response": {
            "status": 200,
            "headers": { "Content-Type": "application/json" },
            "jsonBody": [ { "id": 2, "name": "Bob", "age": 25, "gender": "male" } ]
          }
        }
        """);

        // Negative: age = -1 -> 400
        addStub("""
        {
          "request": {
            "method": "GET",
            "urlPath": "/users",
            "queryParameters": {
              "age": { "equalTo": "-1" }
            }
          },
          "response": {
            "status": 400,
            "headers": { "Content-Type": "application/json" },
            "jsonBody": { "message": "Invalid age", "code": "BAD_REQUEST" }
          }
        }
        """);

        // Negative: gender=unknown -> 422 or empty list (422-ს ვაბრუნებ ამ შემთხვევაში)
        addStub("""
        {
          "request": {
            "method": "GET",
            "urlPath": "/users",
            "queryParameters": {
              "gender": { "equalTo": "unknown" }
            }
          },
          "response": {
            "status": 422,
            "headers": { "Content-Type": "application/json" },
            "jsonBody": { "message": "Unsupported gender", "code": "UNPROCESSABLE_ENTITY" }
          }
        }
        """);

        // Negative: generic server error /users -> 500
        addStub("""
        {
          "priority": 1,
          "request": {
            "method": "GET",
            "urlPath": "/users",
            "queryParameters": {
              "forceError": { "equalTo": "true" }
            }
          },
          "response": {
            "status": 500,
            "headers": { "Content-Type": "application/json" },
            "jsonBody": { "message": "Internal Server Error", "code": "ISE" }
          }
        }
        """);
    }
}
