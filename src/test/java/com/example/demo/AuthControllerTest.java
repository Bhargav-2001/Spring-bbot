package com.example.demo;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthControllerTest {

    @LocalServerPort
    private int port;

    private String registerBody;
    private String loginBody;
    private String validateTokenBody;

    @BeforeEach
    public void setUp() throws IOException {
        RestAssured.port = port;

        // Read the JSON file and initialize register, login, and validate token bodies
        File jsonFile = new File("src/test/resources/auth.json");
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
        }

        String jsonString = contentBuilder.toString();
        registerBody = extractJsonSection(jsonString, "register");
        loginBody = extractJsonSection(jsonString, "login");
        validateTokenBody = extractJsonSection(jsonString, "validateToken");
    }

    private String extractJsonSection(String jsonString, String section) {
        String startMarker = "\"" + section + "\":";
        int startIndex = jsonString.indexOf(startMarker) + startMarker.length();
        int endIndex = jsonString.indexOf("}", startIndex) + 1;
        return jsonString.substring(startIndex, endIndex).trim();
    }

    @Test
    public void testRegister() {

        // Send the POST request for register and validate the response
        Response registerResponse = given()
                .contentType(ContentType.JSON)
                .body(registerBody)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract().response();

        handleResponse(registerResponse, "Register");
    }

    @Test
    public void testLogin_Success() {
        // Send the POST request for login and validate the response
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract().response();

        // Extract token from login response
        String token = loginResponse.jsonPath().getString("token");

        // Process login response
        handleResponse(loginResponse, "Login");

        // Update validateTokenBody with the extracted token for further tests
        validateTokenBody = validateTokenBody.replace("\"token\": \"\"", "\"token\": \"" + token + "\"");
    }

    @Test
    public void testValidateToken_ValidToken() {
        // First, login to get a valid token
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(loginBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract().response();

        String token = loginResponse.jsonPath().getString("token");

        // Send the GET request for validate token using the token
        Response validateTokenResponse = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/auth/validate-token")
                .then()
                .statusCode(is(200))
                .extract().response();

        handleResponse(validateTokenResponse, "Validate Token with Valid Token");
    }

    @Test
    public void testValidateToken_InvalidToken() {
        String invalidToken = "invalid-token";

        // Send the GET request for validate token using the invalid token
        Response validateTokenResponse = given()
                .header("Authorization", "Bearer " + invalidToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/auth/validate-token")
                .then()
                .statusCode(is(401))
                .extract().response();

        handleResponse(validateTokenResponse, "Validate Token with Invalid Token");
    }

    @Test
    public void testLogin_InvalidCredentials() {
        // Modify loginBody with invalid credentials
        String invalidLoginBody = loginBody.replace("\"password\": \"password\"", "\"password\": \"wrong_password\"");

        // Send the POST request for login and validate the response
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(invalidLoginBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(is(401))
                .extract().response();

        handleResponse(loginResponse, "Login with Invalid Credentials");
    }

    @Test
    public void testLogin_MissingCredentials() {
        // Modify loginBody to remove the password field
        String missingCredentialsBody = loginBody.replace("\"password\": \"password\"", "");

        // Send the POST request for login and validate the response
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(missingCredentialsBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(is(400))
                .extract().response();

        handleResponse(loginResponse, "Login with Missing Credentials");
    }

    @Test
    public void testLogin_InvalidJsonFormat() {
        // Send the POST request for login with invalid JSON and validate the response
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body("{username: \"user@example.com\", password: password}") // Invalid JSON
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(is(400))
                .extract().response();

        handleResponse(loginResponse, "Login with Invalid JSON Format");
    }

    // Utility method to handle response (printing them for example)
    private void handleResponse(Response response, String action) {
        int statusCode = response.getStatusCode();
        String responseBody = response.getBody().asString();

        System.out.println(action + " Status Code: " + statusCode);
        System.out.println(action + " Response Body: " + responseBody);
    }
}
