package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.app.FeedCacheHybridProperties;
import com.jaeychoi.dailyus.post.dto.PostFeedItemResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostFeedService {

  private static final long DEFAULT_SIZE = 10L;

  private final PostMapper postMapper;
  private final PostFeedCacheService postFeedCacheService;
  private final FeedCacheHybridProperties feedCacheHybridProperties;

  public PostFeedResponse getFeed(Long userId, LocalDateTime createdAt, Long postId, Long size) {
    long pageSize = resolvePageSize(size);
    long querySize = pageSize + 1;
    List<Long> cachedPostIds =
        postFeedCacheService.findCachedPostIds(userId, postId, querySize);

    List<PostFeedRow> rows;
    if (cachedPostIds != null) {
      rows = loadFeedRowsByPostIds(cachedPostIds);
    } else {
      rows = postFeedCacheService.loadFeedRows(userId, createdAt, postId, querySize);
    }

    if (feedCacheHybridProperties.enabled()) {
      List<Long> hotAuthorIds = postMapper.findHotFeedAuthorIds(
          userId,
          feedCacheHybridProperties.hotAuthorThreshold()
      );
      List<PostFeedRow> hotAuthorRows = loadFeedRowsByPostIds(
          postFeedCacheService.findCachedAuthorPostIds(hotAuthorIds, createdAt, querySize)
      );
      rows = mergeFeedRows(
          rows,
          filterRowsAfterCursor(hotAuthorRows, createdAt, postId),
          querySize
      );
    }

    boolean hasNext = hasNext(rows, pageSize);
    List<PostFeedRow> pageRows = getPageRows(rows, pageSize, hasNext);
    Map<Long, List<String>> imageUrlsByPostId = loadImageUrlsByPostId(pageRows);
    PostFeedRow lastRow = getLastRow(pageRows);
    return new PostFeedResponse(
        toItems(pageRows, imageUrlsByPostId),
        lastRow == null ? null : lastRow.createdAt(),
        lastRow == null ? null : lastRow.postId(),
        hasNext,
        (long) pageRows.size()
    );
  }

  private long resolvePageSize(Long size) {
    if (size == null || size <= 0) {
      return DEFAULT_SIZE;
    }
    return size;
  }

  private List<PostFeedRow> filterRowsAfterCursor(
      List<PostFeedRow> rows,
      LocalDateTime createdAt,
      Long postId
  ) {
    if (rows == null || rows.isEmpty()) {
      return List.of();
    }
    if (createdAt == null || postId == null) {
      return rows;
    }

    return rows.stream()
        .filter(row -> isBeforeCursor(row, createdAt, postId))
        .toList();
  }

  private boolean isBeforeCursor(PostFeedRow row, LocalDateTime createdAt, Long postId) {
    if (row.createdAt() == null || row.postId() == null) {
      return false;
    }
    if (row.createdAt().isBefore(createdAt)) {
      return true;
    }
    return row.createdAt().isEqual(createdAt) && row.postId() < postId;
  }

  private List<PostFeedRow> mergeFeedRows(
      List<PostFeedRow> cachedRows,
      List<PostFeedRow> hotAuthorRows,
      long limit
  ) {
    Map<Long, PostFeedRow> rowByPostId = Stream.concat(
            nullSafeStream(cachedRows),
            nullSafeStream(hotAuthorRows)
        )
        .filter(row -> row.postId() != null)
        .collect(Collectors.toMap(
            PostFeedRow::postId,
            row -> row,
            (left, right) -> left
        ));

    return rowByPostId.values().stream()
        .sorted(Comparator
            .comparing(PostFeedRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PostFeedRow::postId, Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(limit)
        .toList();
  }

  private Stream<PostFeedRow> nullSafeStream(List<PostFeedRow> rows) {
    if (rows == null) {
      return Stream.empty();
    }
    return rows.stream().filter(Objects::nonNull);
  }

  private List<PostFeedRow> loadFeedRowsByPostIds(List<Long> postIds) {
    if (postIds == null || postIds.isEmpty()) {
      return null;
    }

    Map<Long, Integer> orderByPostId = new HashMap<>();
    for (int i = 0; i < postIds.size(); i++) {
      orderByPostId.put(postIds.get(i), i);
    }

    return postMapper.findFeedPostsByIds(postIds).stream()
        .sorted(Comparator.comparingInt(
            row -> orderByPostId.getOrDefault(row.postId(), Integer.MAX_VALUE)))
        .toList();
  }

  private boolean hasNext(List<PostFeedRow> rows, long pageSize) {
    return rows.size() > pageSize;
  }

  private List<PostFeedRow> getPageRows(List<PostFeedRow> rows, long pageSize, boolean hasNext) {
    return hasNext ? rows.subList(0, (int) pageSize) : rows;
  }

  private List<PostFeedItemResponse> toItems(
      List<PostFeedRow> rows,
      Map<Long, List<String>> imageUrlsByPostId
  ) {
    return rows.stream()
        .map(row -> new PostFeedItemResponse(
            row.postId(),
            row.userId(),
            row.nickname(),
            row.profileImage(),
            row.content(),
            imageUrlsByPostId.getOrDefault(row.postId(), Collections.emptyList()),
            row.likeCount(),
            row.createdAt()
        ))
        .toList();
  }

  private PostFeedRow getLastRow(List<PostFeedRow> rows) {
    return rows.isEmpty() ? null : rows.get(rows.size() - 1);
  }

  private Map<Long, List<String>> loadImageUrlsByPostId(List<PostFeedRow> rows) {
    if (rows.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Long> postIds = rows.stream()
        .map(PostFeedRow::postId)
        .toList();

    return postMapper.findImagesByPostIds(postIds).stream()
        .collect(Collectors.groupingBy(
            PostImageRow::postId,
            LinkedHashMap::new,
            Collectors.mapping(PostImageRow::imageUrl, Collectors.toList())
        ));
  }
}
