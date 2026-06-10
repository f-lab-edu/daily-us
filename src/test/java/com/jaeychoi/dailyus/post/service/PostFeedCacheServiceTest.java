package com.jaeychoi.dailyus.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.app.FeedCacheHybridProperties;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
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
class PostFeedCacheServiceTest {

  @Mock
  private PostMapper postMapper;

  @Mock
  private PostFeedRepository postFeedRepository;

  @Mock
  private FeedCacheHybridProperties feedCacheHybridProperties;

  @InjectMocks
  private PostFeedCacheService postFeedCacheService;

  @Test
  void loadFeedRowsRefreshesFirstPageCache() {
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
    when(postMapper.findFeedPosts(userId, 100L, null, null)).thenReturn(List.of(firstRow, secondRow));

    postFeedCacheService.loadFeedRows(userId, null, null, 3L);

    verify(postMapper).findFeedPosts(userId, 100L, null, null);
    verify(postFeedRepository).addPostIdToFeeds(List.of(userId), 10L, firstRow.createdAt(), 100L);
    verify(postFeedRepository).addPostIdToFeeds(List.of(userId), 9L, secondRow.createdAt(), 100L);
  }

  @Test
  void loadFeedRowsDoesNotRefreshCursorRequestCache() {
    Long userId = 1L;
    LocalDateTime cursorCreatedAt = LocalDateTime.of(2026, 4, 6, 9, 0);
    when(postMapper.existsFeedPosts(userId)).thenReturn(false);
    when(postMapper.findRecentFeedPosts(11L, cursorCreatedAt, 15L)).thenReturn(List.of());

    postFeedCacheService.loadFeedRows(userId, cursorCreatedAt, 15L, 11L);

    verify(postMapper).findRecentFeedPosts(11L, cursorCreatedAt, 15L);
    verify(postFeedRepository, never()).addPostIdToFeeds(List.of(userId), 15L, cursorCreatedAt, 100L);
  }

  @Test
  void refreshUserFeedCacheLoadsAtLeastMaxFeedCacheSize() {
    Long userId = 1L;
    PostFeedRow row = new PostFeedRow(
        10L,
        2L,
        "author-1",
        "https://example.com/profile-1.png",
        "content-1",
        3L,
        LocalDateTime.of(2026, 4, 6, 10, 0)
    );
    when(postMapper.existsFeedPosts(userId)).thenReturn(true);
    when(postMapper.findFeedPosts(userId, 100L, null, null)).thenReturn(List.of(row));

    postFeedCacheService.refreshUserFeedCache(userId);

    verify(postMapper).findFeedPosts(userId, 100L, null, null);
    verify(postFeedRepository).addPostIdToFeeds(List.of(userId), 10L, row.createdAt(), 100L);
  }

  @Test
  void cachePostToFeedsUsesSharedMaxFeedCacheSize() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 4, 26, 12, 0);

    postFeedCacheService.cachePostToFeeds(List.of(3L, 9L), 15L, createdAt);

    verify(postFeedRepository).addPostIdToFeeds(List.of(3L, 9L), 15L, createdAt, 100L);
  }

  @Test
  void loadFeedRowsExcludesHotAuthorsWhenHybridEnabled() {
    Long userId = 1L;
    when(feedCacheHybridProperties.enabled()).thenReturn(true);
    when(feedCacheHybridProperties.hotAuthorThreshold()).thenReturn(10000L);
    when(postMapper.existsFeedPosts(userId)).thenReturn(true);
    when(postMapper.findNormalFeedPosts(userId, 10000L, 100L, null, null)).thenReturn(List.of());

    postFeedCacheService.loadFeedRows(userId, null, null, 3L);

    verify(postMapper).findNormalFeedPosts(userId, 10000L, 100L, null, null);
    verify(postMapper, never()).findFeedPosts(userId, 100L, null, null);
  }

  @Test
  void cachePostToAuthorFeedUsesSharedAuthorFeedCacheSize() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 4, 26, 12, 0);

    postFeedCacheService.cachePostToAuthorFeed(3L, 15L, createdAt);

    verify(postFeedRepository).addPostIdToAuthorFeed(3L, 15L, createdAt, 500L);
  }

  @Test
  void findCachedAuthorPostIdsWarmsAuthorTimelineWhenCacheMiss() {
    Long authorId = 3L;
    LocalDateTime createdAt = LocalDateTime.of(2026, 4, 26, 12, 0);
    PostFeedRow row = new PostFeedRow(
        15L,
        authorId,
        "author",
        null,
        "content",
        0L,
        createdAt
    );
    when(postFeedRepository.findByAuthorIdsAndCursor(List.of(authorId), null, 3L))
        .thenReturn(List.of(), List.of(15L));
    when(postMapper.findPostsByUserId(authorId, 500L, null, null)).thenReturn(List.of(row));

    List<Long> postIds = postFeedCacheService.findCachedAuthorPostIds(
        List.of(authorId),
        null,
        3L
    );

    verify(postFeedRepository).addPostIdToAuthorFeed(authorId, 15L, createdAt, 500L);
    assertThat(postIds).containsExactly(15L);
  }

  @Test
  void removePostFromFeedsDelegatesToRepository() {
    postFeedCacheService.removePostFromFeeds(List.of(3L, 9L), 15L);

    verify(postFeedRepository).removePostIdFromFeeds(List.of(3L, 9L), 15L);
  }

  @Test
  void removePostFromAuthorFeedDelegatesToRepository() {
    postFeedCacheService.removePostFromAuthorFeed(3L, 15L);

    verify(postFeedRepository).removePostIdFromAuthorFeed(3L, 15L);
  }
}
