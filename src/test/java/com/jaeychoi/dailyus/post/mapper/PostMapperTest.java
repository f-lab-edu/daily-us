package com.jaeychoi.dailyus.post.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostDetailRow;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
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
    Long userId = insertUser("author@example.com", "author");
    Post post = Post.builder()
        .userId(userId)
        .content("today's routine")
        .build();

    postMapper.insert(post);

    assertThat(post.getPostId()).isNotNull();
    assertThat(countPosts(post.getPostId())).isEqualTo(1);
  }

  @Test
  void insertImagesPersistsAllPostImages() throws Exception {
    Long userId = insertUser("images@example.com", "images");
    Long postId = insertPost(userId, "post with images");
    List<String> imageUrls = List.of(
        "https://cdn.example.com/1.png",
        "https://cdn.example.com/2.png"
    );

    postMapper.insertImages(postId, imageUrls);

    assertThat(countPostImages(postId)).isEqualTo(2);
    assertThat(loadPostImageUrls(postId))
        .containsExactlyInAnyOrderElementsOf(imageUrls);
  }

  @Test
  void countActiveByUserIdCountsOnlyNonDeletedPosts() throws Exception {
    Long userId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long otherUserId = insertUser(uniqueEmail("other"), uniqueNickname("other"));
    Long activePostId = insertPost(userId, "active post");
    Long deletedPostId = insertPost(userId, "deleted post");
    insertPost(otherUserId, "other user post");
    deletePost(deletedPostId);

    long count = postMapper.countActiveByUserId(userId);

    assertThat(count).isEqualTo(1L);
  }

  @Test
  void postLikeQueriesInsertDeleteAndUpdateCount() throws Exception {
    Long userId = insertUser("post-like-user@example.com", "post-like-user");
    Long postId = insertPost(userId, "liked post");

    assertThat(postMapper.existsActiveById(postId)).isTrue();

    postMapper.insertLike(postId, userId);
    postMapper.applyLikeCountDelta(postId, 1L);

    assertThat(countPostLikes(postId, userId)).isEqualTo(1);
    assertThat(findPostLikeCount(postId)).isEqualTo(1L);

    assertThat(postMapper.deleteLike(postId, userId)).isEqualTo(1);
    postMapper.applyLikeCountDelta(postId, -1L);

    assertThat(countPostLikes(postId, userId)).isZero();
    assertThat(findPostLikeCount(postId)).isZero();
  }

  @Test
  void applyLikeCountDeltaDoesNotDropBelowZero() throws Exception {
    Long userId = insertUser("post-like-floor-user@example.com", "post-like-floor-user");
    Long postId = insertPost(userId, "post with zero likes");

    postMapper.applyLikeCountDelta(postId, -1L);

    assertThat(findPostLikeCount(postId)).isZero();
  }

  @Test
  void deleteMarksPostImagesAndCommentsDeletedAndRemovesLikesAndHashtags() throws Exception {
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long otherUserId = insertUser(uniqueEmail("other"), uniqueNickname("other"));
    Long postId = insertPost(authorId, "delete target");
    postMapper.insertImages(postId, List.of("https://cdn.example.com/delete-1.png"));
    Long commentId = insertComment(otherUserId, postId, "comment");
    insertPostLike(postId, otherUserId);
    insertCommentLike(commentId, authorId);
    Long hashtagId = insertHashtag("delete-tag");
    insertHashtagPost(hashtagId, postId);

    assertThat(postMapper.deleteCommentLikesByPostId(postId)).isEqualTo(1);
    assertThat(postMapper.deletePostLikesByPostId(postId)).isEqualTo(1);
    assertThat(postMapper.deleteHashtagsByPostId(postId)).isEqualTo(1);
    assertThat(postMapper.deleteCommentsByPostId(postId)).isEqualTo(1);
    assertThat(postMapper.deleteImagesByPostId(postId)).isEqualTo(1);
    assertThat(postMapper.delete(postId, authorId)).isEqualTo(1);

    assertThat(postMapper.findById(postId)).isNull();
    assertThat(countActiveComments(postId)).isZero();
    assertThat(countActivePostImages(postId)).isZero();
    assertThat(countPostLikes(postId, otherUserId)).isZero();
    assertThat(countCommentLikes(commentId)).isZero();
    assertThat(countHashtagPosts(postId)).isZero();
  }

  @Test
  void deleteReturnsZeroWhenAuthorDoesNotMatch() throws Exception {
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long otherUserId = insertUser(uniqueEmail("other"), uniqueNickname("other"));
    Long postId = insertPost(authorId, "delete target");

    int updated = postMapper.delete(postId, otherUserId);

    assertThat(updated).isZero();
    assertThat(postMapper.findById(postId)).isNotNull();
  }

  void findDetailByIdReturnsActivePostDetailWithLikedByMe() throws Exception {
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long postId = insertPost(authorId, "detail content");
    updatePostCreatedAt(postId, LocalDateTime.of(2026, 5, 17, 9, 0));
    postMapper.insertImages(postId, List.of(
        "https://cdn.example.com/1.png",
        "https://cdn.example.com/2.png"
    ));
    postMapper.insertLike(postId, loginUserId);
    postMapper.applyLikeCountDelta(postId, 1L);

    PostDetailRow row = postMapper.findDetailById(postId, loginUserId);

    assertThat(row).isNotNull();
    assertThat(row.postId()).isEqualTo(postId);
    assertThat(row.userId()).isEqualTo(authorId);
    assertThat(row.nickname()).startsWith("author-");
    assertThat(row.content()).isEqualTo("detail content");
    assertThat(row.likeCount()).isEqualTo(1L);
    assertThat(row.likedByMe()).isTrue();
    assertThat(row.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 17, 9, 0));
  }

  @Test
  void findDetailByIdReturnsNullWhenPostIsDeleted() throws Exception {
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long postId = insertPost(authorId, "deleted detail");
    deletePost(postId);

    PostDetailRow row = postMapper.findDetailById(postId, loginUserId);

    assertThat(row).isNull();
  }

  @Test
  void findFeedPostsReturnsPostsFromFolloweesAndGroupMembers() throws Exception {
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
    updatePostCreatedAt(followeePostId, LocalDateTime.of(2026, 4, 6, 9, 0));
    updatePostCreatedAt(groupMemberPostId, LocalDateTime.of(2026, 4, 6, 8, 0));
    insertPost(outsiderId, "outsider post");

    List<PostFeedRow> rows = postMapper.findFeedPosts(loginUserId, 10L, null, null);

    assertThat(rows).extracting(PostFeedRow::postId)
        .containsSequence(followeePostId, groupMemberPostId)
        .doesNotContainNull();
    assertThat(rows).extracting(PostFeedRow::userId)
        .contains(followeeId, groupMemberId)
        .doesNotContain(outsiderId);
  }

  @Test
  void findFeedPostsReturnsRowsAfterCompositeCursor() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));
    insertFollow(loginUserId, followeeId);
    Long olderPostId = insertPost(followeeId, "older post");
    Long cursorPostId = insertPost(followeeId, "cursor post");
    Long newerPostId = insertPost(followeeId, "newer post");
    updatePostCreatedAt(olderPostId, LocalDateTime.of(2026, 4, 6, 8, 0));
    updatePostCreatedAt(cursorPostId, LocalDateTime.of(2026, 4, 6, 9, 0));
    updatePostCreatedAt(newerPostId, LocalDateTime.of(2026, 4, 6, 10, 0));

    List<PostFeedRow> rows = postMapper.findFeedPosts(
        loginUserId,
        10L,
        LocalDateTime.of(2026, 4, 6, 9, 0),
        cursorPostId
    );

    assertThat(rows).extracting(PostFeedRow::postId)
        .contains(olderPostId)
        .doesNotContain(cursorPostId, newerPostId);
  }

  @Test
  void findFeedPostsUsesPostIdAsTieBreakerWithinSameCreatedAt() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));
    insertFollow(loginUserId, followeeId);
    Long lowerPostId = insertPost(followeeId, "lower post");
    Long higherPostId = insertPost(followeeId, "higher post");
    LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 4, 6, 9, 0);
    updatePostCreatedAt(lowerPostId, sameCreatedAt);
    updatePostCreatedAt(higherPostId, sameCreatedAt);

    List<PostFeedRow> rows = postMapper.findFeedPosts(loginUserId, 10L, sameCreatedAt,
        higherPostId);

    assertThat(rows).extracting(PostFeedRow::postId)
        .contains(lowerPostId)
        .doesNotContain(higherPostId);
  }

  @Test
  void existsFeedPostsReturnsTrueWhenRelatedUsersHavePosts() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));
    insertFollow(loginUserId, followeeId);
    insertPost(followeeId, "followee post");

    boolean exists = postMapper.existsFeedPosts(loginUserId);

    assertThat(exists).isTrue();
  }

  @Test
  void existsFeedPostsReturnsFalseWhenRelatedUsersHaveNoPosts() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));
    insertFollow(loginUserId, followeeId);

    boolean exists = postMapper.existsFeedPosts(loginUserId);

    assertThat(exists).isFalse();
  }

  @Test
  void findFeedPostsExcludesDeletedFolloweesDeletedMembersAndDeletedGroups() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long activeFolloweeId = insertUser(uniqueEmail("active-followee"),
        uniqueNickname("active-followee"));
    Long deletedFolloweeId = insertUser(uniqueEmail("deleted-followee"),
        uniqueNickname("deleted-followee"));
    Long activeGroupMemberId = insertUser(uniqueEmail("active-group"),
        uniqueNickname("active-group"));
    Long deletedGroupMemberId = insertUser(uniqueEmail("deleted-group"),
        uniqueNickname("deleted-group"));

    insertFollow(loginUserId, activeFolloweeId);
    insertFollow(loginUserId, deletedFolloweeId);

    Long activeGroupId = insertGroup(loginUserId, "active-group");
    Long deletedGroupId = insertGroup(loginUserId, "deleted-group");
    insertGroupMember(activeGroupId, loginUserId);
    insertGroupMember(activeGroupId, activeGroupMemberId);
    insertGroupMember(deletedGroupId, loginUserId);
    insertGroupMember(deletedGroupId, deletedGroupMemberId);

    Long activeFolloweePostId = insertPost(activeFolloweeId, "active followee post");
    Long deletedFolloweePostId = insertPost(deletedFolloweeId, "deleted followee post");
    Long activeGroupMemberPostId = insertPost(activeGroupMemberId, "active group member post");
    Long deletedGroupMemberPostId = insertPost(deletedGroupMemberId, "deleted group member post");

    deleteUser(deletedFolloweeId);
    deleteGroup(deletedGroupId);

    List<PostFeedRow> rows = postMapper.findFeedPosts(loginUserId, 10L, null, null);

    assertThat(rows).extracting(PostFeedRow::postId)
        .contains(activeFolloweePostId, activeGroupMemberPostId)
        .doesNotContain(deletedFolloweePostId, deletedGroupMemberPostId);
  }

  @Test
  void existsFeedPostsReturnsFalseWhenOnlyDeletedRelationsHavePosts() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long deletedFolloweeId = insertUser(uniqueEmail("deleted-followee"),
        uniqueNickname("deleted-followee"));
    Long deletedGroupMemberId = insertUser(uniqueEmail("deleted-group"),
        uniqueNickname("deleted-group"));

    insertFollow(loginUserId, deletedFolloweeId);
    Long deletedGroupId = insertGroup(loginUserId, "deleted-group");
    insertGroupMember(deletedGroupId, loginUserId);
    insertGroupMember(deletedGroupId, deletedGroupMemberId);
    insertPost(deletedFolloweeId, "deleted followee post");
    insertPost(deletedGroupMemberId, "deleted group member post");

    deleteUser(deletedFolloweeId);
    deleteGroup(deletedGroupId);

    boolean exists = postMapper.existsFeedPosts(loginUserId);

    assertThat(exists).isFalse();
  }

  @Test
  void findRecentFeedPostsReturnsRowsAfterCompositeCursor() throws Exception {
    Long firstUserId = insertUser(uniqueEmail("first"), uniqueNickname("first"));
    Long secondUserId = insertUser(uniqueEmail("second"), uniqueNickname("second"));
    Long olderPostId = insertPost(secondUserId, "older post");
    Long cursorPostId = insertPost(firstUserId, "cursor post");
    Long newerPostId = insertPost(firstUserId, "newer post");
    updatePostCreatedAt(olderPostId, LocalDateTime.of(2026, 4, 6, 8, 0));
    updatePostCreatedAt(cursorPostId, LocalDateTime.of(2026, 4, 6, 9, 0));
    updatePostCreatedAt(newerPostId, LocalDateTime.of(2026, 4, 6, 10, 0));

    List<PostFeedRow> rows = postMapper.findRecentFeedPosts(
        10L,
        LocalDateTime.of(2026, 4, 6, 9, 0),
        cursorPostId
    );

    assertThat(rows).extracting(PostFeedRow::postId)
        .contains(olderPostId)
        .doesNotContain(cursorPostId, newerPostId);
  }

  @Test
  void findPostsReturnsOnlyActivePostsByUserIdOfUserOrderedByCreatedAtDescThenPostIdDesc()
      throws Exception {
    Long userId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long otherUserId = insertUser(uniqueEmail("other"), uniqueNickname("other"));
    Long olderPostId = insertPost(userId, "older post");
    Long sameTimeLowerPostId = insertPost(userId, "same-time lower");
    Long sameTimeHigherPostId = insertPost(userId, "same-time higher");
    Long deletedPostId = insertPost(userId, "deleted post");
    insertPost(otherUserId, "other user post");
    updatePostCreatedAt(olderPostId, LocalDateTime.of(2026, 5, 1, 8, 0));
    LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 5, 1, 9, 0);
    updatePostCreatedAt(sameTimeLowerPostId, sameCreatedAt);
    updatePostCreatedAt(sameTimeHigherPostId, sameCreatedAt);
    updatePostCreatedAt(deletedPostId, LocalDateTime.of(2026, 5, 1, 10, 0));
    deletePost(deletedPostId, LocalDateTime.of(2026, 5, 1, 11, 0));

    List<PostFeedRow> rows = postMapper.findPostsByUserId(userId, 10L, null, null);

    assertThat(rows).extracting(PostFeedRow::postId)
        .containsSequence(sameTimeHigherPostId, sameTimeLowerPostId, olderPostId)
        .doesNotContain(deletedPostId);
    assertThat(rows).extracting(PostFeedRow::userId)
        .containsOnly(userId);
  }

  @Test
  void findPostsByUserIdReturnsRowsAfterCompositeCursor() throws Exception {
    Long userId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long olderPostId = insertPost(userId, "older post");
    Long cursorPostId = insertPost(userId, "cursor post");
    Long newerPostId = insertPost(userId, "newer post");
    updatePostCreatedAt(olderPostId, LocalDateTime.of(2026, 5, 1, 8, 0));
    updatePostCreatedAt(cursorPostId, LocalDateTime.of(2026, 5, 1, 9, 0));
    updatePostCreatedAt(newerPostId, LocalDateTime.of(2026, 5, 1, 10, 0));

    List<PostFeedRow> rows = postMapper.findPostsByUserId(
        userId,
        10L,
        LocalDateTime.of(2026, 5, 1, 9, 0),
        cursorPostId
    );

    assertThat(rows).extracting(PostFeedRow::postId)
        .contains(olderPostId)
        .doesNotContain(cursorPostId, newerPostId);
  }

  @Test
  void findImagesByPostIdsReturnsImagesGroupedInCreatedOrder() throws Exception {
    Long userId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long firstPostId = insertPost(userId, "first post");
    Long secondPostId = insertPost(userId, "second post");
    postMapper.insertImages(firstPostId, List.of(
        "https://cdn.example.com/first-1.png",
        "https://cdn.example.com/first-2.png"
    ));
    postMapper.insertImages(secondPostId, List.of("https://cdn.example.com/second-1.png"));

    List<PostImageRow> rows = postMapper.findImagesByPostIds(List.of(firstPostId, secondPostId));

    assertThat(rows).extracting(PostImageRow::postId)
        .containsExactly(firstPostId, firstPostId, secondPostId);
    assertThat(rows).extracting(PostImageRow::imageUrl)
        .containsExactly(
            "https://cdn.example.com/first-1.png",
            "https://cdn.example.com/first-2.png",
            "https://cdn.example.com/second-1.png"
        );
  }

  @Test
  void findActivityDaysByUserIdReturnsDistinctDaysWithinMonth() throws Exception {
    Long userId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long otherUserId = insertUser(uniqueEmail("other"), uniqueNickname("other"));
    Long firstPostId = insertPost(userId, "march 10 first");
    Long secondPostId = insertPost(userId, "march 10 second");
    Long thirdPostId = insertPost(userId, "march 12");
    Long deletedPostId = insertPost(userId, "deleted march 11");
    Long aprilPostId = insertPost(userId, "april 1");
    Long otherUserPostId = insertPost(otherUserId, "other user march 15");
    updatePostCreatedAt(firstPostId, LocalDateTime.of(2026, 3, 10, 9, 0));
    updatePostCreatedAt(secondPostId, LocalDateTime.of(2026, 3, 10, 20, 0));
    updatePostCreatedAt(thirdPostId, LocalDateTime.of(2026, 3, 12, 8, 0));
    updatePostCreatedAt(deletedPostId, LocalDateTime.of(2026, 3, 11, 7, 0));
    updatePostCreatedAt(aprilPostId, LocalDateTime.of(2026, 4, 1, 10, 0));
    updatePostCreatedAt(otherUserPostId, LocalDateTime.of(2026, 3, 15, 10, 0));
    deletePost(deletedPostId);

    List<Integer> activityDays = postMapper.findActivityDaysByUserId(
        userId,
        LocalDateTime.of(2026, 3, 1, 0, 0),
        LocalDateTime.of(2026, 4, 1, 0, 0)
    );

    assertThat(activityDays).containsExactly(10, 12);
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
                    intro,
                    profile_image,
                    deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
            new String[]{"user_id"}
        )) {
      statement.setString(1, email);
      statement.setString(2, "encoded-password");
      statement.setString(3, nickname);
      statement.setLong(4, 0L);
      statement.setLong(5, 0L);
      statement.setString(6, null);
      statement.setString(7, null);
      statement.setTimestamp(8, null);
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

  private Long insertComment(Long userId, Long postId, String content) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO comments (
                    content,
                    user_id,
                    post_id
                ) VALUES (?, ?, ?)
                """,
            new String[]{"comment_id"}
        )) {
      statement.setString(1, content);
      statement.setLong(2, userId);
      statement.setLong(3, postId);
      statement.executeUpdate();

      try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        return generatedKeys.getLong(1);
      }
    }
  }

  private void insertPostLike(Long postId, Long userId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO post_likes (
                    post_id,
                    user_id
                ) VALUES (?, ?)
                """
        )) {
      statement.setLong(1, postId);
      statement.setLong(2, userId);
      statement.executeUpdate();
    }
  }

  private void insertCommentLike(Long commentId, Long userId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO comment_likes (
                    comment_id,
                    user_id
                ) VALUES (?, ?)
                """
        )) {
      statement.setLong(1, commentId);
      statement.setLong(2, userId);
      statement.executeUpdate();
    }
  }

  private Long insertHashtag(String name) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO hashtag (name) VALUES (?)",
            new String[]{"hashtag_id"}
        )) {
      statement.setString(1, name);
      statement.executeUpdate();

      try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        return generatedKeys.getLong(1);
      }
    }
  }

  private void insertHashtagPost(Long hashtagId, Long postId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO hashtag_posts (
                    hashtag_id,
                    post_id
                ) VALUES (?, ?)
                """
        )) {
      statement.setLong(1, hashtagId);
      statement.setLong(2, postId);
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

  private void deletePost(Long postId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE posts SET deleted_at = CURRENT_TIMESTAMP WHERE post_id = ?"
        )) {
      statement.setLong(1, postId);
      statement.executeUpdate();
    }
  }

  private void deleteUser(Long userId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE users SET deleted_at = ? WHERE user_id = ?"
        )) {
      statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 4, 24, 12, 0)));
      statement.setLong(2, userId);
      statement.executeUpdate();
    }
  }

  private void deleteGroup(Long groupId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE user_groups SET deleted_at = ? WHERE group_id = ?"
        )) {
      statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 4, 24, 12, 0)));
      statement.setLong(2, groupId);
      statement.executeUpdate();
    }
  }

  private void deletePost(Long postId, LocalDateTime deletedAt) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE posts SET deleted_at = ? WHERE post_id = ?"
        )) {
      statement.setTimestamp(1, Timestamp.valueOf(deletedAt));
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

  private int countActivePostImages(Long postId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM post_images WHERE post_id = ? AND deleted_at IS NULL"
    )) {
      statement.setLong(1, postId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countPostLikes(Long postId, Long userId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM post_likes WHERE post_id = ? AND user_id = ?"
    )) {
      statement.setLong(1, postId);
      statement.setLong(2, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countCommentLikes(Long commentId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ?"
    )) {
      statement.setLong(1, commentId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countHashtagPosts(Long postId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM hashtag_posts WHERE post_id = ?"
    )) {
      statement.setLong(1, postId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countActiveComments(Long postId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM comments WHERE post_id = ? AND deleted_at IS NULL"
    )) {
      statement.setLong(1, postId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private Long findPostLikeCount(Long postId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT like_count FROM posts WHERE post_id = ?"
    )) {
      statement.setLong(1, postId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
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
