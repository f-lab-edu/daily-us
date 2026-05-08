package com.jaeychoi.dailyus.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

@MybatisTest
class UserFollowMapperTest {

  @Autowired
  private UserFollowMapper userFollowMapper;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private DataSource dataSource;

  @Test
  void insertAndDeleteManageFollowRelationship() throws Exception {
    // given
    Long followerId = insertUser(uniqueEmail("follower"), uniqueNickname("follower"));
    Long followeeId = insertUser(uniqueEmail("followee"), uniqueNickname("followee"));

    // when
    userFollowMapper.insert(followerId, followeeId);
    int deletedCount = userFollowMapper.delete(followerId, followeeId);

    // then
    assertThat(deletedCount).isEqualTo(1);
    assertThat(rawExistsByFollowerAndFollowee(followerId, followeeId)).isFalse();
  }

  @Test
  void userMapperCountUpdatesReflectInLoadedUser() throws Exception {
    // given
    Long followerId = insertUser(uniqueEmail("actor"), uniqueNickname("actor"));
    Long followeeId = insertUser(uniqueEmail("target"), uniqueNickname("target"));

    // when
    userMapper.incrementFolloweeCount(followerId);
    userMapper.incrementFollowerCount(followeeId);
    userMapper.decrementFolloweeCount(followerId);
    userMapper.decrementFollowerCount(followeeId);

    // then
    assertThat(userMapper.findActiveById(followerId).getFolloweeCount()).isZero();
    assertThat(userMapper.findActiveById(followeeId).getFollowerCount()).isZero();
  }

  @Test
  void findFollowerIdsByFolloweeReturnsFollowers() throws Exception {
    Long followeeId = insertUser(uniqueEmail("writer"), uniqueNickname("writer"));
    Long firstFollowerId = insertUser(uniqueEmail("first"), uniqueNickname("first"));
    Long secondFollowerId = insertUser(uniqueEmail("second"), uniqueNickname("second"));

    userFollowMapper.insert(firstFollowerId, followeeId);
    userFollowMapper.insert(secondFollowerId, followeeId);

    assertThat(userFollowMapper.findFollowerIdsByFollowee(followeeId))
        .containsExactlyInAnyOrder(firstFollowerId, secondFollowerId);
  }

  @Test
  void findFollowerIdsByFolloweeExcludesDeletedFollowers() throws Exception {
    Long followeeId = insertUser(uniqueEmail("writer2"), uniqueNickname("writer2"));
    Long activeFollowerId = insertUser(uniqueEmail("active"), uniqueNickname("active"));
    Long deletedFollowerId = insertUser(uniqueEmail("deleted"), uniqueNickname("deleted"));

    userFollowMapper.insert(activeFollowerId, followeeId);
    userFollowMapper.insert(deletedFollowerId, followeeId);
    softDeleteUser(deletedFollowerId);

    assertThat(userFollowMapper.findFollowerIdsByFollowee(followeeId))
        .containsExactly(activeFollowerId);
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

      try (var generatedKeys = statement.getGeneratedKeys()) {
        generatedKeys.next();
        return generatedKeys.getLong(1);
      }
    }
  }

  private boolean rawExistsByFollowerAndFollowee(Long followerId, Long followeeId)
      throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        """
            SELECT EXISTS(
                SELECT 1
                FROM user_follow
                WHERE follower = ?
                  AND followee = ?
            )
            """
    )) {
      statement.setLong(1, followerId);
      statement.setLong(2, followeeId);

      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getBoolean(1);
      }
    }
  }

  private void softDeleteUser(Long userId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "UPDATE users SET deleted_at = ? WHERE user_id = ?"
    )) {
      statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 4, 24, 12, 0)));
      statement.setLong(2, userId);
      statement.executeUpdate();
    }
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueNickname(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }
}
