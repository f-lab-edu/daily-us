package com.jaeychoi.dailyus.aggregate.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

@MybatisTest
class AggregateReconciliationMapperTest {

  @Autowired
  private AggregateReconciliationMapper aggregateReconciliationMapper;

  @Autowired
  private DataSource dataSource;

  @Test
  void reconcilePostLikeCountFindsAndFixesMismatchedActivePosts() throws Exception {
    Long userId = insertUser("post-like-reconcile@example.com", "post-like-reconcile");
    Long postId = insertPost(userId, "wrong count post");
    insertPostLike(postId, userId);
    insertPostLike(postId, insertUser("post-like-other@example.com", "post-like-other"));
    updatePostLikeCount(postId, 99L);

    assertThat(aggregateReconciliationMapper.findPostIdsWithLikeCountMismatch(10))
        .containsExactly(postId);

    assertThat(aggregateReconciliationMapper.reconcilePostLikeCount(postId)).isEqualTo(1);
    assertThat(findPostLikeCount(postId)).isEqualTo(2L);
  }

  @Test
  void reconcileCommentLikeCountFindsAndFixesMismatchedActiveComments() throws Exception {
    Long userId = insertUser("comment-like-reconcile@example.com", "comment-like-reconcile");
    Long otherUserId = insertUser("comment-like-other@example.com", "comment-like-other");
    Long postId = insertPost(userId, "post for comment");
    Long commentId = insertComment(userId, postId, "wrong count comment");
    insertCommentLike(commentId, userId);
    insertCommentLike(commentId, otherUserId);
    updateCommentLikeCount(commentId, 45L);

    assertThat(aggregateReconciliationMapper.findCommentIdsWithLikeCountMismatch(10))
        .containsExactly(commentId);

    assertThat(aggregateReconciliationMapper.reconcileCommentLikeCount(commentId)).isEqualTo(1);
    assertThat(findCommentLikeCount(commentId)).isEqualTo(2L);
  }

  @Test
  void reconcileFollowCountsFixesActiveUserCountsOnly() throws Exception {
    Long activeTargetId = insertUser("follow-target@example.com", "follow-target");
    Long activeFollowerId = insertUser("active-follower@example.com", "active-follower");
    Long activeFolloweeId = insertUser("active-followee@example.com", "active-followee");
    Long deletedFollowerId = insertUser("deleted-follower@example.com", "deleted-follower");
    Long deletedFolloweeId = insertUser("deleted-followee@example.com", "deleted-followee");
    insertFollow(activeFollowerId, activeTargetId);
    insertFollow(deletedFollowerId, activeTargetId);
    insertFollow(activeTargetId, activeFolloweeId);
    insertFollow(activeTargetId, deletedFolloweeId);
    updateFollowerCount(activeTargetId, 99L);
    updateFolloweeCount(activeTargetId, 88L);
    softDeleteUser(deletedFollowerId);
    softDeleteUser(deletedFolloweeId);

    assertThat(aggregateReconciliationMapper.findUserIdsWithFollowerCountMismatch(10))
        .contains(activeTargetId);
    assertThat(aggregateReconciliationMapper.findUserIdsWithFolloweeCountMismatch(10))
        .contains(activeTargetId);

    assertThat(aggregateReconciliationMapper.reconcileFollowerCount(activeTargetId)).isEqualTo(1);
    assertThat(aggregateReconciliationMapper.reconcileFolloweeCount(activeTargetId)).isEqualTo(1);
    assertThat(findFollowerCount(activeTargetId)).isEqualTo(1L);
    assertThat(findFolloweeCount(activeTargetId)).isEqualTo(1L);
  }

  @Test
  void reconcileMemberCountExcludesDeletedUsersFromActiveGroupCount() throws Exception {
    Long ownerId = insertUser("group-owner@example.com", "group-owner");
    Long activeMemberId = insertUser("active-member@example.com", "active-member");
    Long deletedMemberId = insertUser("deleted-member@example.com", "deleted-member");
    Long groupId = insertGroup(ownerId, "reconcile-group");
    insertGroupMember(groupId, ownerId);
    insertGroupMember(groupId, activeMemberId);
    insertGroupMember(groupId, deletedMemberId);
    updateMemberCount(groupId, 55);
    softDeleteUser(deletedMemberId);

    assertThat(aggregateReconciliationMapper.findGroupIdsWithMemberCountMismatch(10))
        .containsExactly(groupId);

    assertThat(aggregateReconciliationMapper.reconcileMemberCount(groupId)).isEqualTo(1);
    assertThat(findMemberCount(groupId)).isEqualTo(2);
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
                    followee_count
                ) VALUES (?, ?, ?, ?, ?)
                """,
            new String[]{"user_id"}
        )) {
      statement.setString(1, email);
      statement.setString(2, "encoded-password");
      statement.setString(3, nickname);
      statement.setLong(4, 0L);
      statement.setLong(5, 0L);
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
                    owner_id
                ) VALUES (?, ?, ?, ?)
                """,
            new String[]{"group_id"}
        )) {
      statement.setString(1, name);
      statement.setString(2, "intro");
      statement.setString(3, null);
      statement.setLong(4, ownerId);
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

  private void updatePostLikeCount(Long postId, Long likeCount) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE posts SET like_count = ? WHERE post_id = ?"
        )) {
      statement.setLong(1, likeCount);
      statement.setLong(2, postId);
      statement.executeUpdate();
    }
  }

  private void updateCommentLikeCount(Long commentId, Long likeCount) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE comments SET like_count = ? WHERE comment_id = ?"
        )) {
      statement.setLong(1, likeCount);
      statement.setLong(2, commentId);
      statement.executeUpdate();
    }
  }

  private void updateFollowerCount(Long userId, Long followerCount) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE users SET follower_count = ? WHERE user_id = ?"
        )) {
      statement.setLong(1, followerCount);
      statement.setLong(2, userId);
      statement.executeUpdate();
    }
  }

  private void updateFolloweeCount(Long userId, Long followeeCount) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE users SET followee_count = ? WHERE user_id = ?"
        )) {
      statement.setLong(1, followeeCount);
      statement.setLong(2, userId);
      statement.executeUpdate();
    }
  }

  private void updateMemberCount(Long groupId, int memberCount) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE user_groups SET member_count = ? WHERE group_id = ?"
        )) {
      statement.setInt(1, memberCount);
      statement.setLong(2, groupId);
      statement.executeUpdate();
    }
  }

  private void softDeleteUser(Long userId) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE user_id = ?"
        )) {
      statement.setLong(1, userId);
      statement.executeUpdate();
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

  private Long findCommentLikeCount(Long commentId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT like_count FROM comments WHERE comment_id = ?"
    )) {
      statement.setLong(1, commentId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
      }
    }
  }

  private Long findFollowerCount(Long userId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT follower_count FROM users WHERE user_id = ?"
    )) {
      statement.setLong(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
      }
    }
  }

  private Long findFolloweeCount(Long userId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT followee_count FROM users WHERE user_id = ?"
    )) {
      statement.setLong(1, userId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
      }
    }
  }

  private int findMemberCount(Long groupId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT member_count FROM user_groups WHERE group_id = ?"
    )) {
      statement.setLong(1, groupId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }
}
