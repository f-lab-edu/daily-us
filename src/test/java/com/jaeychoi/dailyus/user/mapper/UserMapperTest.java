package com.jaeychoi.dailyus.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.user.domain.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

@MybatisTest
class UserMapperTest {

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private DataSource dataSource;

  @Test
  void existsActiveByEmailAndNicknameReturnsTrueOnlyForActiveUsers() throws Exception {
    // given
    insertUser("active@example.com", "active-user", null);
    insertUser("deleted@example.com", "deleted-user", LocalDateTime.of(2026, 3, 26, 10, 0));

    // when
    boolean activeEmailExists = userMapper.existsActiveByEmail("active@example.com");
    boolean activeNicknameExists = userMapper.existsActiveByNickname("active-user");
    boolean deletedEmailExists = userMapper.existsActiveByEmail("deleted@example.com");
    boolean deletedNicknameExists = userMapper.existsActiveByNickname("deleted-user");

    // then
    assertThat(activeEmailExists).isTrue();
    assertThat(activeNicknameExists).isTrue();
    assertThat(deletedEmailExists).isFalse();
    assertThat(deletedNicknameExists).isFalse();
  }

  @Test
  void existsActiveByIdReturnsTrueOnlyForActiveUsers() throws Exception {
    // given
    Long activeUserId = insertUser("active-id@example.com", "active-id-user", null);
    Long deletedUserId = insertUser(
        "deleted-id@example.com",
        "deleted-id-user",
        LocalDateTime.of(2026, 3, 26, 10, 0)
    );

    // when
    boolean activeExists = userMapper.existsActiveById(activeUserId);
    boolean deletedExists = userMapper.existsActiveById(deletedUserId);

    // then
    assertThat(activeExists).isTrue();
    assertThat(deletedExists).isFalse();
  }

  @Test
  void insertPersistsUserAndSetsGeneratedKey() {
    // given
    User user = User.builder()
        .email("new@example.com")
        .password("encoded-password")
        .nickname("new-user")
        .build();

    // when
    userMapper.insert(user);

    // then
    assertThat(user.getUserId()).isNotNull();
    assertThat(userMapper.existsActiveByEmail("new@example.com")).isTrue();
    assertThat(userMapper.existsActiveByNickname("new-user")).isTrue();
  }

  private Long insertUser(String email, String nickname, LocalDateTime deletedAt) throws Exception {
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
            PreparedStatement.RETURN_GENERATED_KEYS
        )) {
      statement.setString(1, email);
      statement.setString(2, "encoded-password");
      statement.setString(3, nickname);
      statement.setLong(4, 0L);
      statement.setLong(5, 0L);
      statement.setString(6, null);
      statement.setString(7, null);
      statement.setTimestamp(8, deletedAt == null ? null : Timestamp.valueOf(deletedAt));
      statement.executeUpdate();

      try (var generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        return generatedKeys.getLong(1);
      }
    }
  }
}
