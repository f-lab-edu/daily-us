package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.app.FeedCacheHybridProperties;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostFeedRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostFeedCacheService {

  private static final long MAX_FEED_CACHE_SIZE = 100L;
  private static final long MAX_AUTHOR_FEED_CACHE_SIZE = 500L;

  private final PostMapper postMapper;
  private final PostFeedRepository postFeedRepository;
  private final FeedCacheHybridProperties feedCacheHybridProperties;

  public List<Long> findCachedPostIds(Long userId, Long postId, long size) {
    return postFeedRepository.findByUserIdAndCursor(userId, postId, size);
  }

  public List<PostFeedRow> loadFeedRows(Long userId, LocalDateTime createdAt, Long postId, long size) {
    if (!isFirstPageRequest(createdAt, postId)) {
      return findFeedRows(userId, createdAt, postId, size);
    }

    List<PostFeedRow> rows = refreshUserFeedCache(userId, size);
    return rows;
  }

  public void cachePostToFeeds(List<Long> userIds, Long postId, LocalDateTime createdAt) {
    postFeedRepository.addPostIdToFeeds(userIds, postId, createdAt, MAX_FEED_CACHE_SIZE);
  }

  public void cachePostToAuthorFeed(Long authorId, Long postId, LocalDateTime createdAt) {
    postFeedRepository.addPostIdToAuthorFeed(
        authorId,
        postId,
        createdAt,
        MAX_AUTHOR_FEED_CACHE_SIZE
    );
  }

  public List<Long> findCachedAuthorPostIds(
      List<Long> authorIds,
      LocalDateTime createdAt,
      long size
  ) {
    List<Long> cachedPostIds = postFeedRepository.findByAuthorIdsAndCursor(
        authorIds,
        createdAt,
        size
    );
    if (!cachedPostIds.isEmpty() || authorIds == null || authorIds.isEmpty()) {
      return cachedPostIds;
    }

    refreshAuthorFeedCaches(authorIds);
    return postFeedRepository.findByAuthorIdsAndCursor(authorIds, createdAt, size);
  }

  public void removePostFromFeeds(List<Long> userIds, Long postId) {
    postFeedRepository.removePostIdFromFeeds(userIds, postId);
  }

  public void removePostFromAuthorFeed(Long authorId, Long postId) {
    postFeedRepository.removePostIdFromAuthorFeed(authorId, postId);
  }

  public List<PostFeedRow> refreshUserFeedCache(Long userId, long minimumSize) {
    long cacheSize = Math.max(minimumSize, MAX_FEED_CACHE_SIZE);
    List<PostFeedRow> rows = findFeedRows(userId, null, null, cacheSize);
    cacheUserFeed(userId, rows);
    return rows;
  }

  public void refreshUserFeedCache(Long userId) {
    refreshUserFeedCache(userId, MAX_FEED_CACHE_SIZE);
  }

  private void refreshAuthorFeedCaches(List<Long> authorIds) {
    for (Long authorId : authorIds) {
      if (authorId == null) {
        continue;
      }

      List<PostFeedRow> rows = postMapper.findPostsByUserId(
          authorId,
          MAX_AUTHOR_FEED_CACHE_SIZE,
          null,
          null
      );
      for (PostFeedRow row : rows) {
        cachePostToAuthorFeed(authorId, row.postId(), row.createdAt());
      }
    }
  }

  private boolean isFirstPageRequest(LocalDateTime createdAt, Long postId) {
    return createdAt == null && postId == null;
  }

  private List<PostFeedRow> findFeedRows(
      Long userId,
      LocalDateTime createdAt,
      Long postId,
      long size
  ) {
    if (postMapper.existsFeedPosts(userId)) {
      if (feedCacheHybridProperties.enabled()) {
        return postMapper.findNormalFeedPosts(
            userId,
            feedCacheHybridProperties.hotAuthorThreshold(),
            size,
            createdAt,
            postId
        );
      }
      return postMapper.findFeedPosts(userId, size, createdAt, postId);
    }

    return postMapper.findRecentFeedPosts(size, createdAt, postId);
  }

  private void cacheUserFeed(Long userId, List<PostFeedRow> rows) {
    if (userId == null || rows == null || rows.isEmpty()) {
      return;
    }

    for (PostFeedRow row : rows) {
      cachePostToFeeds(List.of(userId), row.postId(), row.createdAt());
    }
  }
}
