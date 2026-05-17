package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPostServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private UserPostService userPostService;

  @Test
  void getMyPostsReturnsPagedPostsWithImages() {
    Long userId = 1L;
    PostFeedRow firstRow = new PostFeedRow(
        10L,
        userId,
        "user",
        "https://example.com/profile.png",
        "content-1",
        3L,
        LocalDateTime.of(2026, 5, 1, 10, 0)
    );
    PostFeedRow secondRow = new PostFeedRow(
        9L,
        userId,
        "user",
        "https://example.com/profile.png",
        "content-2",
        1L,
        LocalDateTime.of(2026, 5, 1, 9, 0)
    );
    PostFeedRow thirdRow = new PostFeedRow(
        8L,
        userId,
        "user",
        "https://example.com/profile.png",
        "content-3",
        0L,
        LocalDateTime.of(2026, 5, 1, 8, 0)
    );

    when(userMapper.existsActiveById(userId)).thenReturn(true);
    when(postMapper.findPostsByUserId(userId, 3L, null, null)).thenReturn(List.of(firstRow, secondRow, thirdRow));
    when(postMapper.findImagesByPostIds(List.of(10L, 9L))).thenReturn(List.of(
        new PostImageRow(10L, "https://cdn.example.com/10-1.png"),
        new PostImageRow(9L, "https://cdn.example.com/9-1.png")
    ));

    PostFeedResponse response = userPostService.getMyPosts(userId, null, null, 2L);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).postId()).isEqualTo(10L);
    assertThat(response.items().get(0).imageUrls()).containsExactly("https://cdn.example.com/10-1.png");
    assertThat(response.items().get(1).postId()).isEqualTo(9L);
    assertThat(response.lastCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 9, 0));
    assertThat(response.lastPostId()).isEqualTo(9L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.size()).isEqualTo(2L);
  }

  @Test
  void getMyPostsReturnsEmptyItemsWhenCursorHasNoRows() {
    Long userId = 1L;
    LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 5, 1, 9, 0);

    when(userMapper.existsActiveById(userId)).thenReturn(true);
    when(postMapper.findPostsByUserId(userId, 11L, cursorCreatedAt, 9L)).thenReturn(List.of());

    PostFeedResponse response = userPostService.getMyPosts(userId, cursorCreatedAt, 9L, 10L);

    verify(postMapper, never()).findImagesByPostIds(anyList());
    assertThat(response.items()).isEmpty();
    assertThat(response.lastCreatedAt()).isNull();
    assertThat(response.lastPostId()).isNull();
    assertThat(response.hasNext()).isFalse();
    assertThat(response.size()).isEqualTo(10L);
  }

  @Test
  void getMyPostsThrowsWhenUserIsNotActive() {
    when(userMapper.existsActiveById(1L)).thenReturn(false);

    assertThatThrownBy(() -> userPostService.getMyPosts(1L, null, null, 10L))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }
}
