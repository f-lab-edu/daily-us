package com.jaeychoi.dailyus.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import com.jaeychoi.dailyus.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = "post.created.test")
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "app.kafka.topics.post-created=post.created.test",
    "app.feed-cache.warmup.enabled=false"
})
class PostFeedFanoutApiIntegrationTest {

  private static final Duration FANOUT_SLA = Duration.ofSeconds(3);

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
          .withExposedPorts(6379);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("spring.data.redis.password", () -> "");
    registry.add("spring.data.redis.client-name", () -> "post-feed-fanout-api-test");
  }

  @BeforeEach
  void setUp() {
    flushRedis();
    clearDatabase();
  }

  @Test
  void createPostEventuallyAppearsInFollowerFeedWithinThreeSeconds() throws Exception {
    TestUser author = insertUser("author");
    TestUser follower = insertUser("follower");
    insertFollow(follower.userId(), author.userId());

    Long oldPostId = insertPost(author.userId(), "old-post");
    updatePostCreatedAt(oldPostId, LocalDateTime.of(2026, 4, 6, 8, 0));

    JsonNode warmFeed = getFeed(follower.accessToken(), 10L, null, null);
    assertThat(extractPostIds(warmFeed)).containsExactly(oldPostId);

    JsonNode createdPost = createPost(
        author.accessToken(),
        """
            {
              "imageUrls": ["https://cdn.example.com/new-post.png"],
              "content": "new fanout post #Morning"
            }
            """
    );
    Long createdPostId = createdPost.path("postId").asLong();

    JsonNode updatedFeed = awaitFeedContainsPost(follower.accessToken(), 10L, createdPostId);

    assertThat(extractPostIds(updatedFeed)).contains(createdPostId, oldPostId);
    assertThat(extractPostIds(updatedFeed).get(0)).isEqualTo(createdPostId);
    assertThat(updatedFeed.path("items").get(0).path("content").asText())
        .isEqualTo("new fanout post #Morning");
    assertThat(extractImageUrls(updatedFeed.path("items").get(0)))
        .containsExactly("https://cdn.example.com/new-post.png");
  }

  @Test
  void createPostEventuallyAppearsInAuthorsOwnFeedWithinThreeSeconds() throws Exception {
    TestUser author = insertUser("author");

    JsonNode createdPost = createPost(
        author.accessToken(),
        """
            {
              "imageUrls": ["https://cdn.example.com/self-post.png"],
              "content": "self fanout post #Solo"
            }
            """
    );
    Long createdPostId = createdPost.path("postId").asLong();

    JsonNode updatedFeed = awaitFeedContainsPost(author.accessToken(), 10L, createdPostId);

    assertThat(extractPostIds(updatedFeed)).contains(createdPostId);
    assertThat(extractPostIds(updatedFeed).get(0)).isEqualTo(createdPostId);
    assertThat(updatedFeed.path("items").get(0).path("content").asText())
        .isEqualTo("self fanout post #Solo");
  }

  @Test
  void createPostEventuallyAppearsInGroupMembersFeedWithoutFollowRelation() throws Exception {
    TestUser author = insertUser("author");
    TestUser groupMember = insertUser("group-member");

    Long groupId = insertGroup(author.userId(), "fanout-group");
    insertGroupMember(groupId, author.userId());
    insertGroupMember(groupId, groupMember.userId());

    JsonNode warmFeed = getFeed(groupMember.accessToken(), 10L, null, null);
    assertThat(extractPostIds(warmFeed)).isEmpty();

    JsonNode createdPost = createPost(
        author.accessToken(),
        """
            {
              "imageUrls": ["https://cdn.example.com/group-post.png"],
              "content": "group only fanout #Crew"
            }
            """
    );
    Long createdPostId = createdPost.path("postId").asLong();

    JsonNode updatedFeed = awaitFeedContainsPost(groupMember.accessToken(), 10L, createdPostId);

    assertThat(extractPostIds(updatedFeed)).containsExactly(createdPostId);
    assertThat(updatedFeed.path("items").get(0).path("content").asText())
        .isEqualTo("group only fanout #Crew");
  }

  @Test
  void createPostDoesNotAppearInPersonalizedFeedOfUnrelatedUser() throws Exception {
    TestUser author = insertUser("author");
    TestUser unrelatedReader = insertUser("unrelated-reader");
    TestUser relatedFollowee = insertUser("related-followee");

    insertFollow(unrelatedReader.userId(), relatedFollowee.userId());
    Long relatedPostId = insertPost(relatedFollowee.userId(), "related-post");
    updatePostCreatedAt(relatedPostId, LocalDateTime.of(2026, 4, 6, 8, 0));

    JsonNode warmFeed = getFeed(unrelatedReader.accessToken(), 10L, null, null);
    assertThat(extractPostIds(warmFeed)).containsExactly(relatedPostId);

    JsonNode createdPost = createPost(
        author.accessToken(),
        """
            {
              "imageUrls": ["https://cdn.example.com/unrelated-post.png"],
              "content": "should not reach unrelated feed #Private"
            }
            """
    );
    Long createdPostId = createdPost.path("postId").asLong();

    Awaitility.await()
        .atMost(FANOUT_SLA)
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(() -> {
          JsonNode latestFeed = getFeed(unrelatedReader.accessToken(), 10L, null, null);
          assertThat(extractPostIds(latestFeed))
              .containsExactly(relatedPostId)
              .doesNotContain(createdPostId);
        });
  }

  @Test
  void feedApiMaintainsEightReadsToTwoWritesMixWithFanoutAndDeduplication() throws Exception {
    TestUser author = insertUser("author");
    TestUser reader = insertUser("reader");
    TestUser outsider = insertUser("outsider");

    insertFollow(reader.userId(), author.userId());
    Long groupId = insertGroup(author.userId(), "daily-us");
    insertGroupMember(groupId, author.userId());
    insertGroupMember(groupId, reader.userId());

    Long oldestPostId = insertPost(author.userId(), "seed-1");
    Long middlePostId = insertPost(author.userId(), "seed-2");
    Long newestSeedPostId = insertPost(author.userId(), "seed-3");
    Long outsiderPostId = insertPost(outsider.userId(), "outsider");
    updatePostCreatedAt(oldestPostId, LocalDateTime.of(2026, 4, 6, 6, 0));
    updatePostCreatedAt(middlePostId, LocalDateTime.of(2026, 4, 6, 7, 0));
    updatePostCreatedAt(newestSeedPostId, LocalDateTime.of(2026, 4, 6, 8, 0));
    updatePostCreatedAt(outsiderPostId, LocalDateTime.of(2026, 4, 6, 11, 0));

    List<JsonNode> reads = new ArrayList<>();
    List<JsonNode> writes = new ArrayList<>();

    JsonNode read1 = getFeed(reader.accessToken(), 10L, null, null);
    reads.add(read1);

    JsonNode read2 = getFeed(reader.accessToken(), 2L, null, null);
    reads.add(read2);

    JsonNode write1 = createPost(
        author.accessToken(),
        """
            {
              "imageUrls": ["https://cdn.example.com/write-1.png"],
              "content": "write one #Routine"
            }
            """
    );
    writes.add(write1);
    Long write1PostId = write1.path("postId").asLong();

    JsonNode read3 = awaitFeedContainsPost(reader.accessToken(), 10L, write1PostId);
    reads.add(read3);

    JsonNode read4 = getFeed(reader.accessToken(), 3L, null, null);
    reads.add(read4);

    JsonNode read5 = getFeed(
        reader.accessToken(),
        10L,
        read4.path("lastCreatedAt").asText(),
        read4.path("lastPostId").asLong()
    );
    reads.add(read5);

    JsonNode write2 = createPost(
        author.accessToken(),
        """
            {
              "imageUrls": [
                "https://cdn.example.com/write-2-a.png",
                "https://cdn.example.com/write-2-b.png"
              ],
              "content": "write two #Routine #Focus"
            }
            """
    );
    writes.add(write2);
    Long write2PostId = write2.path("postId").asLong();

    JsonNode read6 = awaitFeedContainsPost(reader.accessToken(), 10L, write2PostId);
    reads.add(read6);

    JsonNode read7 = getFeed(reader.accessToken(), 2L, null, null);
    reads.add(read7);

    JsonNode read8 = getFeed(
        reader.accessToken(),
        10L,
        read7.path("lastCreatedAt").asText(),
        read7.path("lastPostId").asLong()
    );
    reads.add(read8);

    assertThat(reads).hasSize(8);
    assertThat(writes).hasSize(2);

    assertThat(extractPostIds(read1))
        .containsExactly(newestSeedPostId, middlePostId, oldestPostId)
        .doesNotContain(outsiderPostId);

    assertThat(extractPostIds(read2))
        .containsExactly(newestSeedPostId, middlePostId);
    assertThat(read2.path("hasNext").asBoolean()).isTrue();

    assertThat(extractPostIds(read3).get(0)).isEqualTo(write1PostId);
    assertThat(countOccurrences(extractPostIds(read3), write1PostId)).isEqualTo(1);

    assertThat(extractPostIds(read4))
        .containsExactly(write1PostId, newestSeedPostId, middlePostId);
    assertThat(read4.path("hasNext").asBoolean()).isTrue();

    assertThat(extractPostIds(read5)).containsExactly(oldestPostId);

    assertThat(extractPostIds(read6))
        .containsExactly(write2PostId, write1PostId, newestSeedPostId, middlePostId, oldestPostId)
        .doesNotContain(outsiderPostId);
    assertThat(countOccurrences(extractPostIds(read6), write2PostId)).isEqualTo(1);
    assertThat(countOccurrences(extractPostIds(read6), write1PostId)).isEqualTo(1);

    assertThat(extractPostIds(read7)).containsExactly(write2PostId, write1PostId);
    assertThat(extractPostIds(read8))
        .containsExactly(newestSeedPostId, middlePostId, oldestPostId)
        .doesNotContain(write2PostId, write1PostId, outsiderPostId);

    assertThat(extractImageUrls(read6.path("items").get(0)))
        .containsExactly(
            "https://cdn.example.com/write-2-a.png",
            "https://cdn.example.com/write-2-b.png"
        );
    assertThat(read6.path("items").get(0).path("content").asText())
        .isEqualTo("write two #Routine #Focus");
  }

  private JsonNode awaitFeedContainsPost(String accessToken, Long size, Long expectedPostId) {
    final JsonNode[] latest = new JsonNode[1];
    Awaitility.await()
        .atMost(FANOUT_SLA)
        .pollInterval(Duration.ofMillis(200))
        .until(() -> {
          JsonNode feed = getFeedUnchecked(accessToken, size, null, null);
          latest[0] = feed;
          return extractPostIds(feed).contains(expectedPostId);
        });
    return latest[0];
  }

  private JsonNode getFeedUnchecked(
      String accessToken,
      Long size,
      String createdAt,
      Long postId
  ) {
    try {
      return getFeed(accessToken, size, createdAt, postId);
    } catch (Exception e) {
      throw new IllegalStateException("Feed polling failed", e);
    }
  }

  private JsonNode getFeed(
      String accessToken,
      Long size,
      String createdAt,
      Long postId
  ) throws Exception {
    var request = get("/api/v1/posts")
        .header("Authorization", "Bearer " + accessToken)
        .param("size", String.valueOf(size));
    if (createdAt != null) {
      request.param("createdAt", createdAt);
    }
    if (postId != null) {
      request.param("postId", String.valueOf(postId));
    }

    MvcResult result = mockMvc.perform(request)
        .andExpect(status().isOk())
        .andReturn();

    return parseData(result);
  }

  private JsonNode createPost(String accessToken, String body) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/posts")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isCreated())
        .andReturn();

    return parseData(result);
  }

  private JsonNode parseData(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
  }

  private List<Long> extractPostIds(JsonNode feedData) {
    List<Long> postIds = new ArrayList<>();
    for (JsonNode item : feedData.path("items")) {
      postIds.add(item.path("postId").asLong());
    }
    return postIds;
  }

  private List<String> extractImageUrls(JsonNode item) {
    List<String> imageUrls = new ArrayList<>();
    for (JsonNode imageUrl : item.path("imageUrls")) {
      imageUrls.add(imageUrl.asText());
    }
    return imageUrls;
  }

  private int countOccurrences(List<Long> values, Long target) {
    int count = 0;
    for (Long value : values) {
      if (target.equals(value)) {
        count++;
      }
    }
    return count;
  }

  private void flushRedis() {
    stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushAll();
      return null;
    });
  }

  private void clearDatabase() {
    try (Connection connection = dataSource.getConnection()) {
      executeDelete(connection, "DELETE FROM comment_likes");
      executeDelete(connection, "DELETE FROM comments");
      executeDelete(connection, "DELETE FROM post_likes");
      executeDelete(connection, "DELETE FROM post_images");
      executeDelete(connection, "DELETE FROM hashtag_posts");
      executeDelete(connection, "DELETE FROM hashtag");
      executeDelete(connection, "DELETE FROM posts");
      executeDelete(connection, "DELETE FROM group_members");
      executeDelete(connection, "DELETE FROM user_follow");
      executeDelete(connection, "DELETE FROM user_groups");
      executeDelete(connection, "DELETE FROM users");
    } catch (Exception e) {
      throw new IllegalStateException("Failed to clear database", e);
    }
  }

  private void executeDelete(Connection connection, String sql) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  private TestUser insertUser(String prefix) throws Exception {
    String email = prefix + "-" + UUID.randomUUID() + "@example.com";
    String nickname = prefix + "-" + UUID.randomUUID();

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO users (
                    email,
                    password,
                    nickname,
                    follower_count,
                    followee_count,
                    profile_image,
                    deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            new String[]{"user_id"}
        )) {
      statement.setString(1, email);
      statement.setString(2, "encoded-password");
      statement.setString(3, nickname);
      statement.setLong(4, 0L);
      statement.setLong(5, 0L);
      statement.setString(6, null);
      statement.setTimestamp(7, null);
      statement.executeUpdate();

      try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        Long userId = generatedKeys.getLong(1);
        String accessToken = jwtTokenProvider.createAccessToken(User.builder()
            .userId(userId)
            .email(email)
            .nickname(nickname)
            .password("encoded-password")
            .build());
        return new TestUser(userId, email, nickname, accessToken);
      }
    }
  }

  private void insertFollow(Long followerId, Long followeeId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO user_follow (follower, followee) VALUES (?, ?)"
        )) {
      statement.setLong(1, followerId);
      statement.setLong(2, followeeId);
      statement.executeUpdate();
    }
  }

  private Long insertGroup(Long ownerId, String name) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO user_groups (
                    name,
                    intro,
                    group_image,
                    owner_id,
                    member_count,
                    deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?)
                """,
            new String[]{"group_id"}
        )) {
      statement.setString(1, name);
      statement.setString(2, "intro");
      statement.setString(3, null);
      statement.setLong(4, ownerId);
      statement.setInt(5, 0);
      statement.setTimestamp(6, null);
      statement.executeUpdate();

      try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        return generatedKeys.getLong(1);
      }
    }
  }

  private void insertGroupMember(Long groupId, Long userId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)"
        )) {
      statement.setLong(1, groupId);
      statement.setLong(2, userId);
      statement.executeUpdate();
    }
  }

  private Long insertPost(Long userId, String content) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO posts (content, user_id) VALUES (?, ?)",
            new String[]{"post_id"}
        )) {
      statement.setString(1, content);
      statement.setLong(2, userId);
      statement.executeUpdate();

      try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        return generatedKeys.getLong(1);
      }
    }
  }

  private void updatePostCreatedAt(Long postId, LocalDateTime createdAt) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE posts SET created_at = ? WHERE post_id = ?"
        )) {
      statement.setTimestamp(1, Timestamp.valueOf(createdAt));
      statement.setLong(2, postId);
      statement.executeUpdate();
    }
  }

  private record TestUser(Long userId, String email, String nickname, String accessToken) {
  }
}
