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
import com.jaeychoi.dailyus.post.repository.PostFeedRepository;
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

  @Mock
  private PostFeedRepository postFeedRepository;

  @InjectMocks
  private PostFeedService postFeedService;

  @Test
  void getFeedReturnsCachedFeedWhenCacheHit() {
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
    PostFeedRow thirdRow = new PostFeedRow(
        9L,
        3L,
        "author-2",
        "https://example.com/profile-2.png",
        "content-2",
        1L,
        LocalDateTime.of(2026, 4, 6, 9, 0)
    );

    when(postFeedRepository.findByUserIdAndCursor(userId, null, 3L))
        .thenReturn(List.of(10L, 9L, 8L));
    when(postMapper.findFeedPostsByIds(List.of(10L, 9L, 8L))).thenReturn(
        List.of(firstRow, secondRow, thirdRow));
    when(postMapper.findImagesByPostIds(List.of(10L, 9L))).thenReturn(List.of(
        new PostImageRow(10L, "https://cdn.example.com/10-1.png"),
        new PostImageRow(10L, "https://cdn.example.com/10-2.png"),
        new PostImageRow(9L, "https://cdn.example.com/9-1.png"),
        new PostImageRow(8L, "https://cdn.example.com/8-1.png")
    ));

    PostFeedResponse response = postFeedService.getFeed(userId, null, null, 2L);

    verify(postMapper, never()).existsFeedPosts(userId);
    verify(postMapper, never()).findFeedPosts(userId, 3L, null, null);
    verify(postMapper, never()).findRecentFeedPosts(3L, null, null);
    assertThat(response.lastCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 6, 9, 0));
    assertThat(response.lastPostId()).isEqualTo(9L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.size()).isEqualTo(2L);
    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).postId()).isEqualTo(10L);
    assertThat(response.items().get(0).imageUrls())
        .containsExactly("https://cdn.example.com/10-1.png", "https://cdn.example.com/10-2.png");
    assertThat(response.items().get(1).postId()).isEqualTo(9L);
    assertThat(response.items().get(1).imageUrls())
        .containsExactly("https://cdn.example.com/9-1.png");
  }

  @Test
  void getFeedReturnsPersonalizedFeedWithCompositeCursor() {
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
    PostFeedRow thirdRow = new PostFeedRow(
        8L,
        4L,
        "author-3",
        "https://example.com/profile-3.png",
        "content-3",
        0L,
        LocalDateTime.of(2026, 4, 6, 8, 0)
    );

    when(postFeedRepository.findByUserIdAndCursor(userId, null, 3L)).thenReturn(null);
    when(postMapper.existsFeedPosts(userId)).thenReturn(true);
    when(postMapper.findFeedPosts(userId, 3L, null, null))
        .thenReturn(List.of(firstRow, secondRow, thirdRow));
    when(postMapper.findImagesByPostIds(List.of(10L, 9L))).thenReturn(List.of(
        new PostImageRow(10L, "https://cdn.example.com/10-1.png"),
        new PostImageRow(10L, "https://cdn.example.com/10-2.png"),
        new PostImageRow(9L, "https://cdn.example.com/9-1.png")
    ));

    PostFeedResponse response = postFeedService.getFeed(userId, null, null, 2L);

    verify(postMapper, never()).findRecentFeedPosts(3L, null, null);
    assertThat(response.lastCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 6, 9, 0));
    assertThat(response.lastPostId()).isEqualTo(9L);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.size()).isEqualTo(2L);
    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).postId()).isEqualTo(10L);
    assertThat(response.items().get(0).imageUrls())
        .containsExactly("https://cdn.example.com/10-1.png", "https://cdn.example.com/10-2.png");
    assertThat(response.items().get(1).postId()).isEqualTo(9L);
    assertThat(response.items().get(1).imageUrls())
        .containsExactly("https://cdn.example.com/9-1.png");
  }

  @Test
  void getFeedPassesCompositeCursorToRecentFeedQuery() {
    Long userId = 1L;
    LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 4, 6, 9, 0);
    PostFeedRow recentRow = new PostFeedRow(
        8L,
        4L,
        "recent-author",
        null,
        "recent-content",
        0L,
        LocalDateTime.of(2026, 4, 6, 8, 0)
    );

    when(postFeedRepository.findByUserIdAndCursor(userId, 15L, 11L)).thenReturn(null);
    when(postMapper.existsFeedPosts(userId)).thenReturn(false);
    when(postMapper.findRecentFeedPosts(11L, cursorCreatedAt, 15L)).thenReturn(List.of(recentRow));
    when(postMapper.findImagesByPostIds(List.of(8L))).thenReturn(List.of());

    PostFeedResponse response = postFeedService.getFeed(userId, cursorCreatedAt, 15L, 10L);

    verify(postMapper).findRecentFeedPosts(11L, cursorCreatedAt, 15L);
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).postId()).isEqualTo(8L);
    assertThat(response.lastCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 6, 8, 0));
    assertThat(response.lastPostId()).isEqualTo(8L);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.size()).isEqualTo(1L);
  }

  @Test
  void getFeedReturnsEmptyItemsWhenCursorHasNoRows() {
    Long userId = 1L;
    LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 4, 6, 9, 0);

    when(postFeedRepository.findByUserIdAndCursor(userId, 9L, 11L)).thenReturn(null);
    when(postMapper.existsFeedPosts(userId)).thenReturn(true);
    when(postMapper.findFeedPosts(userId, 11L, cursorCreatedAt, 9L)).thenReturn(List.of());

    PostFeedResponse response = postFeedService.getFeed(userId, cursorCreatedAt, 9L, 10L);

    verify(postMapper, never()).findRecentFeedPosts(11L, cursorCreatedAt, 9L);
    verify(postMapper, never()).findImagesByPostIds(anyList());
    assertThat(response.items()).isEmpty();
    assertThat(response.size()).isEqualTo(0L);
    assertThat(response.lastCreatedAt()).isNull();
    assertThat(response.lastPostId()).isNull();
    assertThat(response.hasNext()).isFalse();
  }
}
