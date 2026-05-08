package com.jaeychoi.dailyus.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.user.domain.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
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
    String activeEmail = uniqueEmail("active");
    String activeNickname = uniqueNickname("active-user");
    String deletedEmail = uniqueEmail("deleted");
    String deletedNickname = uniqueNickname("deleted-user");
    insertUser(activeEmail, activeNickname, null);
    insertUser(deletedEmail, deletedNickname, LocalDateTime.of(2026, 3, 26, 10, 0));

    // when
    boolean activeEmailExists = userMapper.existsActiveByEmail(activeEmail);
    boolean activeNicknameExists = userMapper.existsActiveByNickname(activeNickname);
    boolean deletedEmailExists = userMapper.existsActiveByEmail(deletedEmail);
    boolean deletedNicknameExists = userMapper.existsActiveByNickname(deletedNickname);

    // then
    assertThat(activeEmailExists).isTrue();
    assertThat(activeNicknameExists).isTrue();
    assertThat(deletedEmailExists).isFalse();
    assertThat(deletedNicknameExists).isFalse();
  }

  @Test
  void existsActiveByIdReturnsTrueOnlyForActiveUsers() throws Exception {
    // given
    Long activeUserId = insertUser(uniqueEmail("active-id"), uniqueNickname("active-id-user"), null);
    Long deletedUserId = insertUser(
        uniqueEmail("deleted-id"),
        uniqueNickname("deleted-id-user"),
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
    String email = uniqueEmail("new");
    String nickname = uniqueNickname("new-user");
    User user = User.builder()
        .email(email)
        .password("encoded-password")
        .nickname(nickname)
        .build();

    // when
    userMapper.insert(user);

    // then
    assertThat(user.getUserId()).isNotNull();
    assertThat(userMapper.existsActiveByEmail(email)).isTrue();
    assertThat(userMapper.existsActiveByNickname(nickname)).isTrue();
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
                    profile_image,
                    deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            PreparedStatement.RETURN_GENERATED_KEYS
        )) {
      statement.setString(1, email);
      statement.setString(2, "encoded-password");
      statement.setString(3, nickname);
      statement.setLong(4, 0L);
      statement.setLong(5, 0L);
      statement.setString(6, null);
      statement.setTimestamp(7, deletedAt == null ? null : Timestamp.valueOf(deletedAt));
      statement.executeUpdate();

      try (var generatedKeys = statement.getGeneratedKeys()) {
        assertThat(generatedKeys.next()).isTrue();
        return generatedKeys.getLong(1);
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
