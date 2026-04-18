import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import net.grinder.scriptengine.groovy.junit.annotation.RunRate

import org.junit.Test
import org.junit.runner.RunWith

import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPResponse
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.message.BasicHeader

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

@RunWith(GrinderRunner)
class FeedLoadTest {

    public static GTest loginTest
    public static GTest feedTest

    public static HTTPRequest loginRequest
    public static HTTPRequest feedRequest

    public static String baseUrl = "http://baseUrl"

    // 시나리오 변경용
    public static String SCENARIO = "MIXED"
    // NORMAL / HEAVY / MIXED

    // 일반 사용자군
    public static List<Map> normalUsers = [
            [email: "local-user00001@dailyus.local", password: "Password1!"],
            [email: "local-user00002@dailyus.local", password: "Password1!"],
            [email: "local-user00003@dailyus.local", password: "Password1!"],
            [email: "local-user00004@dailyus.local", password: "Password1!"]
    ]

    // 고비용 사용자군
    public static List<Map> heavyUsers = [
            [email: "local-user00011@dailyus.local", password: "Password1!"],
            [email: "local-user00012@dailyus.local", password: "Password1!"]
    ]

    public Map currentUser
    public String accessToken

    @BeforeProcess
    static void beforeProcess() {
        loginTest = new GTest(1, "login")
        feedTest = new GTest(2, "getFeed")

        loginRequest = new HTTPRequest()
        feedRequest = new HTTPRequest()
    }

    @BeforeThread
    void beforeThread() {
        loginTest.record(this, "login")
        feedTest.record(this, "getFeed")

        grinder.statistics.delayReports = true

        currentUser = selectUserByScenario(grinder.threadNumber)
        login()
    }

    Map selectUserByScenario(int threadNum) {
        if (SCENARIO == "NORMAL") {
            return normalUsers[threadNum % normalUsers.size()]
        }

        if (SCENARIO == "HEAVY") {
            return heavyUsers[threadNum % heavyUsers.size()]
        }

        // MIXED
        // 80% 일반, 20% 고비용
        int bucket = threadNum % 10
        if (bucket < 8) {
            return normalUsers[threadNum % normalUsers.size()]
        }
        return heavyUsers[threadNum % heavyUsers.size()]
    }

    void login() {
        String body = new JsonBuilder([
                email: currentUser.email,
                password: currentUser.password
        ]).toString()

        List<Header> headers = [
                new BasicHeader("Content-Type", "application/json")
        ]

        HTTPResponse response = loginRequest.POST(
                baseUrl + "/api/v1/auth/signin",
                body.getBytes("UTF-8"),
                headers
        )

        assertNotNull("login response is null", response)
        assertTrue("login failed. status=${response.statusCode}", response.statusCode == 200)

        String responseBody = response.bodyText
        assertNotNull("login response body is null", responseBody)

        def json = new JsonSlurper().parseText(responseBody)
        accessToken = json?.data?.accessToken

        assertNotNull("accessToken is null", accessToken)

        grinder.logger.info("login success. scenario={}, user={}", SCENARIO, currentUser.email)
    }

    @Test
    @RunRate(100)
    void getFeed() {
        List<Header> headers = [
                new BasicHeader("Authorization", "Bearer " + accessToken)
        ]

        HTTPResponse response = feedRequest.GET(
                baseUrl + "/api/v1/posts?page=0&size=10",
                [],
                headers
        )

        // grinder.logger.info("get feed success. user={}, response = {}", currentUser.email, response.getBodyText())

        assertNotNull("feed response is null", response)
        assertTrue("feed failed. status=${response.statusCode}", response.statusCode == 200)
    }
}