package com.jaeychoi.dailyus.post.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.domain.Post;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

@MybatisTest
class PostMapperTest {

  @Autowired
  private PostMapper postMapper;

  @Autowired
  private DataSource dataSource;

  @Test
  void insertPersistsPostAndSetsGeneratedKey() throws Exception {
    // given
    Long userId = insertUser("author@example.com", "author");
    Post post = Post.builder()
        .userId(userId)
        .content("today's routine")
        .build();

    // when
    postMapper.insert(post);

    // then
    assertThat(post.getPostId()).isNotNull();
    assertThat(countPosts(post.getPostId())).isEqualTo(1);
  }

  @Test
  void insertImagesPersistsAllPostImages() throws Exception {
    // given
    Long userId = insertUser("images@example.com", "images");
    Long postId = insertPost(userId, "post with images");
    List<String> imageUrls = List.of(
        "https://cdn.example.com/1.png",
        "https://cdn.example.com/2.png"
    );

    // when
    postMapper.insertImages(postId, imageUrls);

    // then
    assertThat(countPostImages(postId)).isEqualTo(2);
    assertThat(loadPostImageUrls(postId))
        .containsExactlyInAnyOrderElementsOf(imageUrls);
  }

  @Test
  void findFeedPostsReturnsPostsFromFolloweesAndGroupMembers() throws Exception {
    // given
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));
    Long groupMemberId = insertUser(uniqueEmail("group-member"), uniqueNickname("group-member"));
    Long outsiderId = insertUser(uniqueEmail("outsider"), uniqueNickname("outsider"));
    insertFollow(loginUserId, followeeId);
    Long groupId = insertGroup(loginUserId, "daily-us");
    insertGroupMember(groupId, loginUserId);
    insertGroupMember(groupId, groupMemberId);
    Long followeePostId = insertPost(followeeId, "followee post");
    Long groupMemberPostId = insertPost(groupMemberId, "group member post");
    insertPost(outsiderId, "outsider post");

    // when
    List<PostFeedRow> rows = postMapper.findFeedPosts(loginUserId, 10L, 0L);

    // then
    assertThat(rows).extracting(PostFeedRow::postId)
        .contains(followeePostId, groupMemberPostId)
        .doesNotContainNull();
    assertThat(rows).extracting(PostFeedRow::userId)
        .contains(followeeId, groupMemberId)
        .doesNotContain(outsiderId);
  }

  @Test
  void existsFeedPostsReturnsTrueWhenRelatedUsersHavePosts() throws Exception {
    // given
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));
    insertFollow(loginUserId, followeeId);
    insertPost(followeeId, "followee post");

    // when
    boolean exists = postMapper.existsFeedPosts(loginUserId);

    // then
    assertThat(exists).isTrue();
  }

  @Test
  void existsFeedPostsReturnsFalseWhenRelatedUsersHaveNoPosts() throws Exception {
    // given
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));
    insertFollow(loginUserId, followeeId);

    // when
    boolean exists = postMapper.existsFeedPosts(loginUserId);

    // then
    assertThat(exists).isFalse();
  }

  @Test
  void findRecentFeedPostsReturnsRecentPostsOrderedByCreatedAtDesc() throws Exception {
    // given
    Long firstUserId = insertUser(uniqueEmail("first"), uniqueNickname("first"));
    Long secondUserId = insertUser(uniqueEmail("second"), uniqueNickname("second"));
    Long olderPostId = insertPost(firstUserId, "older post");
    Long newerPostId = insertPost(secondUserId, "newer post");
    updatePostCreatedAt(olderPostId, LocalDateTime.of(2026, 4, 6, 8, 0));
    updatePostCreatedAt(newerPostId, LocalDateTime.of(2026, 4, 6, 9, 0));

    // when
    List<PostFeedRow> rows = postMapper.findRecentFeedPosts(10L, 0L);

    // then
    assertThat(rows).extracting(PostFeedRow::postId)
        .containsSequence(newerPostId, olderPostId);
  }

  @Test
  void findImagesByPostIdsReturnsImagesGroupedInCreatedOrder() throws Exception {
    // given
    Long userId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long firstPostId = insertPost(userId, "first post");
    Long secondPostId = insertPost(userId, "second post");
    postMapper.insertImages(firstPostId, List.of(
        "https://cdn.example.com/first-1.png",
        "https://cdn.example.com/first-2.png"
    ));
    postMapper.insertImages(secondPostId, List.of("https://cdn.example.com/second-1.png"));

    // when
    List<PostImageRow> rows = postMapper.findImagesByPostIds(List.of(firstPostId, secondPostId));

    // then
    assertThat(rows).extracting(PostImageRow::postId)
        .containsExactly(firstPostId, firstPostId, secondPostId);
    assertThat(rows).extracting(PostImageRow::imageUrl)
        .containsExactly(
            "https://cdn.example.com/first-1.png",
            "https://cdn.example.com/first-2.png",
            "https://cdn.example.com/second-1.png"
        );
  }

  private Long insertUser(String email, String nickname) throws Exception {
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
        return generatedKeys.getLong(1);
      }
    }
  }

  private Long insertPost(Long userId, String content) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO posts (
                    content,
                    user_id
                ) VALUES (?, ?)
                """,
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

  private void insertFollow(Long followerId, Long followeeId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO user_follow (
                    follower,
                    followee
                ) VALUES (?, ?)
                """
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
                    deleted_at
                ) VALUES (?, ?, ?, ?, ?)
                """,
            new String[]{"group_id"}
        )) {
      statement.setString(1, name);
      statement.setString(2, "intro");
      statement.setString(3, null);
      statement.setLong(4, ownerId);
      statement.setTimestamp(5, null);
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
            """
                INSERT INTO group_members (
                    group_id,
                    user_id
                ) VALUES (?, ?)
                """
        )) {
      statement.setLong(1, groupId);
      statement.setLong(2, userId);
      statement.executeUpdate();
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

  private int countPosts(Long postId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM posts WHERE post_id = ?"
    )) {
      statement.setLong(1, postId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countPostImages(Long postId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM post_images WHERE post_id = ?"
    )) {
      statement.setLong(1, postId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private List<String> loadPostImageUrls(Long postId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT image_url FROM post_images WHERE post_id = ?"
    )) {
      statement.setLong(1, postId);
      try (ResultSet resultSet = statement.executeQuery()) {
        List<String> imageUrls = new ArrayList<>();
        while (resultSet.next()) {
          imageUrls.add(resultSet.getString(1));
        }
        return imageUrls;
      }
    }
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueNickname(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }
}
