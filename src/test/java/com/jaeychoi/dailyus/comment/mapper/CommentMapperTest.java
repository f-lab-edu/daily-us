package com.jaeychoi.dailyus.comment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@MybatisTest
class CommentMapperTest {

  @Autowired
  private CommentMapper commentMapper;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void existsActivePostByIdReturnsTrueOnlyForActivePost() throws Exception {
    Long userId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long activePostId = insertPost(userId, "active");
    Long deletedPostId = insertPost(userId, "deleted");
    updatePostDeletedAt(deletedPostId, LocalDateTime.of(2026, 4, 6, 12, 0));

    assertThat(commentMapper.existsActivePostById(activePostId)).isTrue();
    assertThat(commentMapper.existsActivePostById(deletedPostId)).isFalse();
    assertThat(commentMapper.existsActivePostById(9999L)).isFalse();
  }

  @Test
  void findCommentsReturnsTopLevelCommentsOrderedByCreatedAtDescAndLikeState() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long postId = insertPost(authorId, "post");
    Long olderCommentId = insertComment(authorId, postId, null, "older");
    Long newerCommentId = insertComment(authorId, postId, null, "newer");
    Long replyId = insertComment(loginUserId, postId, newerCommentId, "reply");
    updateCommentCreatedAt(olderCommentId, LocalDateTime.of(2026, 4, 6, 8, 0));
    updateCommentCreatedAt(newerCommentId, LocalDateTime.of(2026, 4, 6, 9, 0));
    updateCommentCreatedAt(replyId, LocalDateTime.of(2026, 4, 6, 10, 0));
    insertCommentLike(newerCommentId, loginUserId);

    List<CommentRow> rows = commentMapper.findComments(postId, loginUserId, 10L, null, null);

    assertThat(rows).extracting(CommentRow::commentId)
        .containsExactly(newerCommentId, olderCommentId);
    assertThat(rows.get(0).likedByMe()).isTrue();
    assertThat(rows.get(1).likedByMe()).isFalse();
    assertThat(rows).allMatch(row -> row.parentId() == null);
  }

  @Test
  void findRepliesByParentIdsReturnsRepliesGroupedByParentWithinLimit() throws Exception {
    Long loginUserId = insertUser(uniqueEmail("login"), uniqueNickname("login"));
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long replierId = insertUser(uniqueEmail("replier"), uniqueNickname("replier"));
    Long postId = insertPost(authorId, "post");
    Long parentOneId = insertComment(authorId, postId, null, "parent-1");
    Long parentTwoId = insertComment(authorId, postId, null, "parent-2");
    Long firstReplyId = insertComment(replierId, postId, parentOneId, "reply-1");
    Long secondReplyId = insertComment(loginUserId, postId, parentOneId, "reply-2");
    Long thirdReplyId = insertComment(replierId, postId, parentOneId, "reply-3");
    Long fourthReplyId = insertComment(authorId, postId, parentOneId, "reply-4");
    Long otherReplyId = insertComment(replierId, postId, parentTwoId, "reply-3");
    updateCommentCreatedAt(firstReplyId, LocalDateTime.of(2026, 4, 6, 8, 0));
    updateCommentCreatedAt(secondReplyId, LocalDateTime.of(2026, 4, 6, 9, 0));
    updateCommentCreatedAt(thirdReplyId, LocalDateTime.of(2026, 4, 6, 10, 0));
    updateCommentCreatedAt(fourthReplyId, LocalDateTime.of(2026, 4, 6, 11, 0));
    updateCommentCreatedAt(otherReplyId, LocalDateTime.of(2026, 4, 6, 7, 0));
    insertCommentLike(secondReplyId, loginUserId);

    List<CommentRow> rows =
        commentMapper.findRepliesByParentIds(List.of(parentOneId, parentTwoId), loginUserId, 3L);

    assertThat(rows).extracting(CommentRow::commentId)
        .containsExactly(fourthReplyId, thirdReplyId, secondReplyId, otherReplyId);
    assertThat(rows).extracting(CommentRow::parentId)
        .containsExactly(parentOneId, parentOneId, parentOneId, parentTwoId);
    assertThat(rows).extracting(CommentRow::likedByMe)
        .containsExactly(false, false, true, false);
  }

  @Test
  void findActiveByIdReturnsOnlyActiveComment() throws Exception {
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long postId = insertPost(authorId, "post");
    Long activeCommentId = insertComment(authorId, postId, null, "active");
    Long deletedCommentId = insertComment(authorId, postId, null, "deleted");
    updateCommentDeletedAt(deletedCommentId, LocalDateTime.of(2026, 4, 6, 12, 0));

    Comment activeComment = commentMapper.findActiveById(activeCommentId);
    Comment deletedComment = commentMapper.findActiveById(deletedCommentId);

    assertThat(activeComment).isNotNull();
    assertThat(activeComment.getCommentId()).isEqualTo(activeCommentId);
    assertThat(activeComment.getUserId()).isEqualTo(authorId);
    assertThat(activeComment.getParentId()).isNull();
    assertThat(deletedComment).isNull();
  }

  @Test
  void deleteCommentLikesRemovesParentAndReplyLikes() throws Exception {
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long likerId = insertUser(uniqueEmail("liker"), uniqueNickname("liker"));
    Long postId = insertPost(authorId, "post");
    Long parentCommentId = insertComment(authorId, postId, null, "parent");
    Long replyCommentId = insertComment(authorId, postId, parentCommentId, "reply");
    insertCommentLike(parentCommentId, likerId);
    insertCommentLike(replyCommentId, likerId);

    int deletedCount = commentMapper.deleteCommentLikes(parentCommentId, true);

    assertThat(deletedCount).isEqualTo(2);
    assertThat(countCommentLikes(parentCommentId)).isZero();
    assertThat(countCommentLikes(replyCommentId)).isZero();
  }

  @Test
  void deleteMarksParentAndRepliesDeletedAndResetsLikeCount() throws Exception {
    Long authorId = insertUser(uniqueEmail("author"), uniqueNickname("author"));
    Long postId = insertPost(authorId, "post");
    Long parentCommentId = insertComment(authorId, postId, null, "parent");
    Long replyCommentId = insertComment(authorId, postId, parentCommentId, "reply");
    updateCommentLikeCount(parentCommentId, 5L);
    updateCommentLikeCount(replyCommentId, 2L);

    int updatedCount = commentMapper.delete(parentCommentId, true);

    assertThat(updatedCount).isEqualTo(2);
    assertThat(readDeletedAt(parentCommentId)).isNotNull();
    assertThat(readDeletedAt(replyCommentId)).isNotNull();
    assertThat(readLikeCount(parentCommentId)).isZero();
    assertThat(readLikeCount(replyCommentId)).isZero();
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

  private Long insertComment(Long userId, Long postId, Long parentId, String content) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            """
                INSERT INTO comments (
                    content,
                    user_id,
                    post_id,
                    parent_id
                ) VALUES (?, ?, ?, ?)
                """,
            new String[]{"comment_id"}
        )) {
      statement.setString(1, content);
      statement.setLong(2, userId);
      statement.setLong(3, postId);
      if (parentId == null) {
        statement.setNull(4, java.sql.Types.BIGINT);
      } else {
        statement.setLong(4, parentId);
      }
      statement.executeUpdate();

      try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        return generatedKeys.getLong(1);
      }
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

  private void updateCommentCreatedAt(Long commentId, LocalDateTime createdAt) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE comments SET created_at = ? WHERE comment_id = ?"
        )) {
      statement.setTimestamp(1, Timestamp.valueOf(createdAt));
      statement.setLong(2, commentId);
      statement.executeUpdate();
    }
  }

  private void updatePostDeletedAt(Long postId, LocalDateTime deletedAt) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE posts SET deleted_at = ? WHERE post_id = ?"
        )) {
      statement.setTimestamp(1, Timestamp.valueOf(deletedAt));
      statement.setLong(2, postId);
      statement.executeUpdate();
    }
  }

  private void updateCommentDeletedAt(Long commentId, LocalDateTime deletedAt) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE comments SET deleted_at = ? WHERE comment_id = ?"
        )) {
      statement.setTimestamp(1, Timestamp.valueOf(deletedAt));
      statement.setLong(2, commentId);
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

  private LocalDateTime readDeletedAt(Long commentId) throws Exception {
    Timestamp deletedAt = jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM comments WHERE comment_id = ?",
        Timestamp.class,
        commentId
    );
    return deletedAt == null ? null : deletedAt.toLocalDateTime();
  }

  private long readLikeCount(Long commentId) {
    Long likeCount = jdbcTemplate.queryForObject(
        "SELECT like_count FROM comments WHERE comment_id = ?",
        Long.class,
        commentId
    );
    assertThat(likeCount).isNotNull();
    return likeCount;
  }

  private int countCommentLikes(Long commentId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ?",
        Integer.class,
        commentId
    );
    assertThat(count).isNotNull();
    return count;
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueNickname(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }
}
