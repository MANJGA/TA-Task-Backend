package ApiTest;

import io.restassured.RestAssured;
import models.ApiError;
import models.User;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;
import utils.DbHelper;
import utils.WireMockAdminClient;

import java.util.List;

import static io.restassured.RestAssured.given;

public class ApiTest {

    private DbHelper db;
    private String baseUrl;
    private WireMockAdminClient wm;

    @BeforeSuite
    public void beforeSuite() {
        baseUrl = System.getProperty("wiremock.baseUrl", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;

        wm = new WireMockAdminClient();
        wm.resetAll();
        wm.seedUsersStubs();

        db = new DbHelper();
    }

    @AfterMethod(alwaysRun = true)
    public void logResultToDb2(ITestResult result) {
        Object[] params = result.getParameters();
        String key = (params != null && params.length > 0 && params[0] instanceof String)
                ? (String) params[0]
                : result.getMethod().getMethodName();

        String status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASSED";
            case ITestResult.FAILURE, ITestResult.SUCCESS_PERCENTAGE_FAILURE -> "FAILED";
            case ITestResult.SKIP -> "SKIPPED";
            default -> "UNKNOWN";
        };

        db.upsertResult(key, status);
    }


    @DataProvider(name = "positiveRequests")
    public Object[][] positiveRequests() {
        return new Object[][] {
                {"testGetAllUsers_Positive", "/users", 2, "Should return two users"},
                {"testFilterByAge_Positive", "/users?age=30", 1, "Should return Alice only"},
                {"testFilterByGender_Positive", "/users?gender=male", 1, "Should return Bob only"},
        };
    }

    @DataProvider(name = "negativeRequests")
    public Object[][] negativeRequests() {
        return new Object[][] {
                {"testInvalidAge_Negative", "/users?age=-1", 400, "Invalid age"},
                {"testInvalidGender_Negative", "/users?gender=unknown", 422, "Unsupported gender"},
                {"testInternalServerError_Negative", "/users?forceError=true", 500, "Internal Server Error"},
        };
    }


    @Test(dataProvider = "positiveRequests")
    public void positiveCases(String testName, String path, int expectedCount, String description) {
        List<User> users =
                given()
                        .when().get(path)
                        .then().statusCode(200)
                        .extract().body().jsonPath().getList("", User.class);

        Assert.assertEquals(users.size(), expectedCount, description);

        if ("/users".equals(path)) {
            Assert.assertTrue(users.stream().anyMatch(u -> u.getName().equals("Alice")));
            Assert.assertTrue(users.stream().anyMatch(u -> u.getName().equals("Bob")));
        }
        if (path.contains("age=30")) {
            Assert.assertEquals(users.get(0).getName(), "Alice");
            Assert.assertEquals(users.get(0).getAge().intValue(), 30);
        }
        if (path.contains("gender=male")) {
            Assert.assertEquals(users.get(0).getName(), "Bob");
            Assert.assertEquals(users.get(0).getGender(), "male");
        }
    }

    @Test(dataProvider = "negativeRequests")
    public void negativeCases(String testName, String path, int expectedStatus, String expectedMsg) {
        ApiError error =
                given()
                        .when().get(path)
                        .then().statusCode(expectedStatus)
                        .extract().as(ApiError.class);

        Assert.assertTrue(error.getMessage() != null && error.getMessage().contains(expectedMsg),
                "Expected message to contain: " + expectedMsg + " but was: " + error.getMessage());
    }
}
