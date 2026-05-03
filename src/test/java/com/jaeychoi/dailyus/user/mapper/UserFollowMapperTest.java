package com.jaeychoi.dailyus.user.mapper;

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
    Long followerId = insertUser("follower@example.com", "follower");
    Long followeeId = insertUser("followee@example.com", "followee");

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
    Long followerId = insertUser("actor@example.com", "actor");
    Long followeeId = insertUser("target@example.com", "target");

    // when
    userMapper.incrementFolloweeCount(followerId);
    userMapper.incrementFollowerCount(followeeId);
    userMapper.decrementFolloweeCount(followerId);
    userMapper.decrementFollowerCount(followeeId);

    // then
    assertThat(userMapper.findActiveById(followerId).getFolloweeCount()).isZero();
    assertThat(userMapper.findActiveById(followeeId).getFollowerCount()).isZero();
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
}
