package com.jaeychoi.dailyus.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostFeedServiceTest {

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private PostFeedService postFeedService;

  @Test
  void getFeedReturnsPersonalizedFeedWithImages() {
    // given
    Long userId = 1L;
    PostFeedRow firstRow = new PostFeedRow(
        10L,
        2L,
        "author-1",
        "https://example.com/profile-1.png",
        "content-1",
        3L,
        LocalDateTime.of(2026, 4, 6, 10, 0)
    );
    PostFeedRow secondRow = new PostFeedRow(
        9L,
        3L,
        "author-2",
        "https://example.com/profile-2.png",
        "content-2",
        1L,
        LocalDateTime.of(2026, 4, 6, 9, 0)
    );
    when(postMapper.existsFeedPosts(userId)).thenReturn(true);
    when(postMapper.findFeedPosts(userId, 10L, 0L)).thenReturn(List.of(firstRow, secondRow));
    when(postMapper.findImagesByPostIds(List.of(10L, 9L))).thenReturn(List.of(
        new PostImageRow(10L, "https://cdn.example.com/10-1.png"),
        new PostImageRow(10L, "https://cdn.example.com/10-2.png"),
        new PostImageRow(9L, "https://cdn.example.com/9-1.png")
    ));

    // when
    PostFeedResponse response = postFeedService.getFeed(userId, 0L, 10L);

    // then
    verify(postMapper, never()).findRecentFeedPosts(10L, 0L);
    assertThat(response.page()).isEqualTo(0L);
    assertThat(response.size()).isEqualTo(10L);
    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).postId()).isEqualTo(10L);
    assertThat(response.items().get(0).imageUrls())
        .containsExactly("https://cdn.example.com/10-1.png", "https://cdn.example.com/10-2.png");
    assertThat(response.items().get(1).postId()).isEqualTo(9L);
    assertThat(response.items().get(1).imageUrls())
        .containsExactly("https://cdn.example.com/9-1.png");
  }

  @Test
  void getFeedReturnsRecentPostsWhenFeedPostsDoNotExist() {
    // given
    Long userId = 1L;
    PostFeedRow recentRow = new PostFeedRow(
        8L,
        4L,
        "recent-author",
        null,
        "recent-content",
        0L,
        LocalDateTime.of(2026, 4, 6, 8, 0)
    );
    when(postMapper.existsFeedPosts(userId)).thenReturn(false);
    when(postMapper.findRecentFeedPosts(10L, 0L)).thenReturn(List.of(recentRow));
    when(postMapper.findImagesByPostIds(List.of(8L))).thenReturn(List.of());

    // when
    PostFeedResponse response = postFeedService.getFeed(userId, 0L, 10L);

    // then
    verify(postMapper).findRecentFeedPosts(10L, 0L);
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).postId()).isEqualTo(8L);
  }

  @Test
  void getFeedReturnsEmptyItemsWhenFeedPostsExistButRequestedPageHasNoRows() {
    // given
    Long userId = 1L;
    when(postMapper.existsFeedPosts(userId)).thenReturn(true);
    when(postMapper.findFeedPosts(userId, 10L, 10L)).thenReturn(List.of());

    // when
    PostFeedResponse response = postFeedService.getFeed(userId, 1L, 10L);

    // then
    verify(postMapper, never()).findRecentFeedPosts(10L, 10L);
    verify(postMapper, never()).findImagesByPostIds(anyList());
    assertThat(response.items()).isEmpty();
    assertThat(response.page()).isEqualTo(1L);
    assertThat(response.size()).isEqualTo(10L);
  }
}
