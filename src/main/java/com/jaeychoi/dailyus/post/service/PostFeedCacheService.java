package com.jaeychoi.dailyus.post.service;

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

  private final PostMapper postMapper;
  private final PostFeedRepository postFeedRepository;

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

  public void removePostFromFeeds(List<Long> userIds, Long postId) {
    postFeedRepository.removePostIdFromFeeds(userIds, postId);
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
