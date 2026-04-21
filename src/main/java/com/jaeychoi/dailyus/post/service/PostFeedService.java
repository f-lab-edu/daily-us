package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.post.dto.PostFeedItemResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostFeedService {

  private static final long DEFAULT_SIZE = 10L;

  private final PostMapper postMapper;

  public PostFeedResponse getFeed(Long userId, LocalDateTime createdAt, Long postId, Long size) {
    long pageSize = resolvePageSize(size);
    List<PostFeedRow> rows = loadFeedRows(userId, createdAt, postId, pageSize + 1);
    boolean hasNext = hasNext(rows, pageSize);
    List<PostFeedRow> pageRows = getPageRows(rows, pageSize, hasNext);
    Map<Long, List<String>> imageUrlsByPostId = loadImageUrlsByPostId(pageRows);
    PostFeedRow lastRow = hasNext ? getLastRow(pageRows) : null;

    return new PostFeedResponse(
        toItems(pageRows, imageUrlsByPostId),
        lastRow == null ? null : lastRow.createdAt(),
        lastRow == null ? null : lastRow.postId(),
        hasNext,
        pageSize
    );
  }

  private long resolvePageSize(Long size) {
    if (size == null || size <= 0) {
      return DEFAULT_SIZE;
    }
    return size;
  }

  private List<PostFeedRow> loadFeedRows(
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
