package com.jaeychoi.dailyus.hashtag.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.hashtag.domain.Hashtag;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

@MybatisTest
class HashtagMapperTest {

  @Autowired
  private HashtagMapper hashtagMapper;

  @Autowired
  private DataSource dataSource;

  @Test
  void findByNamesReturnsMatchingHashtags() throws Exception {
    // given
    insertHashtag("morning-find");
    insertHashtag("routine-find");
    insertHashtag("fitness-find");

    // when
    List<Hashtag> hashtags = hashtagMapper.findByNames(List.of("morning-find", "routine-find"));

    // then
    assertThat(hashtags)
        .extracting(Hashtag::getName)
        .containsExactlyInAnyOrder("morning-find", "routine-find");
  }

  @Test
  void insertPersistsHashtagAndSetsGeneratedKey() throws Exception {
    // given
    Hashtag hashtag = Hashtag.builder()
        .name("daily-insert")
        .build();

    // when
    hashtagMapper.insert(hashtag);

    // then
    assertThat(hashtag.getHashtagId()).isNotNull();
    assertThat(countHashtagsByName("daily-insert")).isEqualTo(1);
  }

  @Test
  void insertPostHashtagsPersistsRelations() throws Exception {
    // given
    Long userId = insertUser("writer@example.com", "writer");
    Long postId = insertPost(userId, "post content");
    Long morningId = insertHashtag("morning-link");
    Long routineId = insertHashtag("routine-link");

    // when
    hashtagMapper.insertPostHashtags(postId, List.of(morningId, routineId));

    // then
    assertThat(countHashtagPost(postId, morningId)).isEqualTo(1);
    assertThat(countHashtagPost(postId, routineId)).isEqualTo(1);
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

  private int countHashtagsByName(String name) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM hashtag WHERE name = ?"
    )) {
      statement.setString(1, name);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countHashtagPost(Long postId, Long hashtagId) throws Exception {
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (PreparedStatement statement = connection.prepareStatement(
        "SELECT COUNT(*) FROM hashtag_posts WHERE post_id = ? AND hashtag_id = ?"
    )) {
      statement.setLong(1, postId);
      statement.setLong(2, hashtagId);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }
}
