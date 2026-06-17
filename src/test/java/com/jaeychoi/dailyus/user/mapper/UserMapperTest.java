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
import org.springframework.jdbc.core.JdbcTemplate;

@MybatisTest
class UserMapperTest {

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void existsActiveByEmailAndNicknameReturnsTrueOnlyForActiveUsers() throws Exception {
    String activeEmail = uniqueEmail("active");
    String activeNickname = uniqueNickname("active-user");
    String deletedEmail = uniqueEmail("deleted");
    String deletedNickname = uniqueNickname("deleted-user");
    insertUser(activeEmail, activeNickname, null);
    insertUser(deletedEmail, deletedNickname, LocalDateTime.of(2026, 3, 26, 10, 0));

    boolean activeEmailExists = userMapper.existsActiveByEmail(activeEmail);
    boolean activeNicknameExists = userMapper.existsActiveByNickname(activeNickname);
    boolean deletedEmailExists = userMapper.existsActiveByEmail(deletedEmail);
    boolean deletedNicknameExists = userMapper.existsActiveByNickname(deletedNickname);

    assertThat(activeEmailExists).isTrue();
    assertThat(activeNicknameExists).isTrue();
    assertThat(deletedEmailExists).isFalse();
    assertThat(deletedNicknameExists).isFalse();
  }

  @Test
  void existsActiveByIdReturnsTrueOnlyForActiveUsers() throws Exception {
    Long activeUserId = insertUser(uniqueEmail("active-id"), uniqueNickname("active-id-user"), null);
    Long deletedUserId = insertUser(
        uniqueEmail("deleted-id"),
        uniqueNickname("deleted-id-user"),
        LocalDateTime.of(2026, 3, 26, 10, 0)
    );

    boolean activeExists = userMapper.existsActiveById(activeUserId);
    boolean deletedExists = userMapper.existsActiveById(deletedUserId);

    assertThat(activeExists).isTrue();
    assertThat(deletedExists).isFalse();
  }

  @Test
  void insertPersistsUserAndSetsGeneratedKey() {
    String email = uniqueEmail("new");
    String nickname = uniqueNickname("new-user");
    User user = User.builder()
        .email(email)
        .password("encoded-password")
        .nickname(nickname)
        .build();

    userMapper.insert(user);

    assertThat(user.getUserId()).isNotNull();
    assertThat(userMapper.existsActiveByEmail(email)).isTrue();
    assertThat(userMapper.existsActiveByNickname(nickname)).isTrue();
  }

  @Test
  void updateProfileUpdatesNicknameIntroAndProfileImage() throws Exception {
    Long userId = insertUser(uniqueEmail("profile"), uniqueNickname("profile-user"), null);

    User user = userMapper.findActiveById(userId);
    String updatedNickname = uniqueNickname("updated-user");
    user.setNickname(updatedNickname);
    user.setIntro("updated intro");
    user.setProfileImage("https://cdn.example.com/profile.png");

    int updatedRows = userMapper.updateProfile(user);
    User updatedUser = userMapper.findActiveById(userId);

    assertThat(updatedRows).isEqualTo(1);
    assertThat(updatedUser.getNickname()).isEqualTo(updatedNickname);
    assertThat(updatedUser.getIntro()).isEqualTo("updated intro");
    assertThat(updatedUser.getProfileImage()).isEqualTo("https://cdn.example.com/profile.png");
  }

  @Test
  void withdrawUpdatesEmailNicknameAndDeletedAtForActiveUser() throws Exception {
    Long userId = insertUser(uniqueEmail("withdraw"), uniqueNickname("withdraw-user"), null);
    User user = userMapper.findActiveById(userId);

    user.setEmail("withdrawn+" + userId + "+" + user.getEmail());
    user.setNickname("withdrawn-" + userId + "-" + user.getNickname());
    user.setDeletedAt(LocalDateTime.of(2026, 6, 17, 23, 0));

    int deletedRows = userMapper.withdraw(user);
    LocalDateTime deletedAt = findDeletedAtByUserId(userId);
    String withdrawnEmail = findEmailByUserId(userId);
    String withdrawnNickname = findNicknameByUserId(userId);

    assertThat(deletedRows).isEqualTo(1);
    assertThat(countActiveUsersById(userId)).isZero();
    assertThat(deletedAt).isNotNull();
    assertThat(withdrawnEmail).startsWith("withdrawn+" + userId + "+");
    assertThat(withdrawnNickname).startsWith("withdrawn-" + userId + "-");
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

  private LocalDateTime findDeletedAtByUserId(Long userId) throws Exception {
    Timestamp deletedAt = jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM users WHERE user_id = ?",
        Timestamp.class,
        userId
    );
    return deletedAt == null ? null : deletedAt.toLocalDateTime();
  }

  private String findEmailByUserId(Long userId) {
    return jdbcTemplate.queryForObject(
        "SELECT email FROM users WHERE user_id = ?",
        String.class,
        userId
    );
  }

  private String findNicknameByUserId(Long userId) {
    return jdbcTemplate.queryForObject(
        "SELECT nickname FROM users WHERE user_id = ?",
        String.class,
        userId
    );
  }

  private int countActiveUsersById(Long userId) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM users WHERE user_id = ? AND deleted_at IS NULL",
        Integer.class,
        userId
    );
    return count == null ? 0 : count;
  }

  private String uniqueEmail(String prefix) {
    return prefix + "-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueNickname(String prefix) {
    return prefix + "-" + UUID.randomUUID();
  }
}
