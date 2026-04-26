package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.post.dto.PostFeedItemResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostFeedRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostFeedService {

  private static final long DEFAULT_SIZE = 10L;

  private final PostMapper postMapper;
  private final PostFeedRepository postFeedRepository;

  public PostFeedResponse getFeed(Long userId, LocalDateTime createdAt, Long postId, Long size) {
    long pageSize = resolvePageSize(size);
    List<Long> cachedPostIds =
        postFeedRepository.findByUserIdAndCursor(userId, postId, pageSize + 1);

    List<PostFeedRow> rows;
    if (cachedPostIds != null) {
      rows = loadFeedRowsByPostIds(cachedPostIds);
    } else {
      rows = loadFeedRowsByCursor(userId, createdAt, postId, pageSize + 1);
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

  private List<PostFeedRow> loadFeedRowsByCursor(
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

  private List<PostFeedRow> loadFeedRowsByPostIds(List<Long> postIds) {
    if (postIds.isEmpty()) {
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
    return rows.get(rows.size() - 1);
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
