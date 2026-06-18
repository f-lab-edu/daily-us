import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import java.util.concurrent.ThreadLocalRandom
import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import net.grinder.scriptengine.groovy.junit.annotation.RunRate
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.message.BasicHeader
import org.junit.Test
import org.junit.runner.RunWith
import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPResponse

@RunWith(GrinderRunner)
class CelebrityFeedReadWriteMixTest {

    public static GTest readerLoginTest
    public static GTest writerLoginTest
    public static GTest feedReadTest
    public static GTest celebrityWriteTest

    public static HTTPRequest loginRequest
    public static HTTPRequest feedRequest
    public static HTTPRequest postRequest

    public static String baseUrl = "http://baseUrl"

    public static final int FOLLOWER_RATIO = 70
    public static final int GENERAL_RATIO = 30
    public static final int READ_RATIO = 80
    public static final int WRITE_RATIO = 20

    public static final int FOLLOWER_START_USER_ID = 2
    public static final int FOLLOWER_POOL_SIZE = 100000
    public static final int GENERAL_START_USER_ID = 100002
    public static final int GENERAL_POOL_SIZE = 40000
    public static final int CELEBRITY_USER_ID = 1

    public static final String DEFAULT_PASSWORD = "Password1!"

    public Map currentReader
    public String currentReaderType
    public String readerAccessToken
    public String celebrityAccessToken

    @BeforeProcess
    static void beforeProcess() {
        readerLoginTest = new GTest(1, "readerLogin")
        writerLoginTest = new GTest(2, "celebrityLogin")
        feedReadTest = new GTest(3, "getFeed")
        celebrityWriteTest = new GTest(4, "createCelebrityPost")

        loginRequest = new HTTPRequest()
        feedRequest = new HTTPRequest()
        postRequest = new HTTPRequest()
    }

    @BeforeThread
    void beforeThread() {
        readerLoginTest.record(this, "loginReader")
        writerLoginTest.record(this, "loginCelebrity")
        feedReadTest.record(this, "getFeed")
        celebrityWriteTest.record(this, "createCelebrityPost")

        grinder.statistics.delayReports = true

        currentReader = selectReader(grinder.threadNumber)
        currentReaderType = currentReader.type
        loginReader()
        loginCelebrity()

        grinder.logger.info(
                "thread prepared. readerType={}, readerEmail={}, readRatio={}%, writeRatio={}%",
                currentReaderType,
                currentReader.email,
                READ_RATIO,
                WRITE_RATIO
        )
    }

    void loginReader() {
        readerAccessToken = login(currentReader)
    }

    void loginCelebrity() {
        celebrityAccessToken = login(buildUser(CELEBRITY_USER_ID, "CELEBRITY"))
    }

    Map selectReader(int threadNum) {
        int bucket = threadNum % 10
        if (bucket < 7) {
            int offset = Math.floorMod(threadNum * 9973, FOLLOWER_POOL_SIZE)
            return buildUser(FOLLOWER_START_USER_ID + offset, "FOLLOWER")
        }

        int offset = Math.floorMod(threadNum * 7919, GENERAL_POOL_SIZE)
        return buildUser(GENERAL_START_USER_ID + offset, "GENERAL")
    }

    Map buildUser(int userId, String type) {
        return [
                userId  : userId,
                type    : type,
                email   : String.format("local-user%06d@dailyus.local", userId),
                password: DEFAULT_PASSWORD
        ]
    }

    String login(Map user) {
        String body = new JsonBuilder([
                email   : user.email,
                password: user.password
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
        assertTrue("login failed. user=${user.email}, status=${response.statusCode}", response.statusCode == 200)

        String responseBody = response.bodyText
        assertNotNull("login response body is null", responseBody)

        def json = new JsonSlurper().parseText(responseBody)
        String accessToken = json?.data?.accessToken

        assertNotNull("accessToken is null. user=${user.email}", accessToken)
        return accessToken
    }

    @Test
    @RunRate(80)
    void getFeed() {
        List<Header> headers = [
                new BasicHeader("Authorization", "Bearer " + readerAccessToken)
        ]

        HTTPResponse response = feedRequest.GET(
                baseUrl + "/api/v1/posts?size=10",
                [],
                headers
        )

        assertNotNull("feed response is null", response)
        assertTrue(
                "feed failed. readerType=${currentReaderType}, user=${currentReader.email}, status=${response.statusCode}",
                response.statusCode == 200
        )
    }

    @Test
    @RunRate(20)
    void createCelebrityPost() {
        long suffix = System.currentTimeMillis() + grinder.threadNumber + ThreadLocalRandom.current().nextInt(1000)
        String body = new JsonBuilder([
                imageUrls: [
                        "https://cdn.example.com/perf/celebrity-" + suffix + "-1.jpg",
                        "https://cdn.example.com/perf/celebrity-" + suffix + "-2.jpg"
                ],
                content  : "celebrity perf post #" + suffix + " #celebrity #fanout"
        ]).toString()

        List<Header> headers = [
                new BasicHeader("Content-Type", "application/json"),
                new BasicHeader("Authorization", "Bearer " + celebrityAccessToken)
        ]

        HTTPResponse response = postRequest.POST(
                baseUrl + "/api/v1/posts",
                body.getBytes("UTF-8"),
                headers
        )

        assertNotNull("create post response is null", response)
        assertTrue("create post failed. status=${response.statusCode}", response.statusCode == 201)
    }
}
