package com.jaeychoi.dailyus.post.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jaeychoi.dailyus.post.domain.Post;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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
}
