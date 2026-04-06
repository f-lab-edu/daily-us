package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.post.dto.PostFeedItemResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
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

  private final PostMapper postMapper;

  public PostFeedResponse getFeed(Long userId, Long page, Long size) {
    Long pageValue = page == null ? 0L : page;
    Long sizeValue = size == null ? 10L : size;

    List<PostFeedRow> rows = loadFeedPosts(userId, pageValue, sizeValue);
    Map<Long, List<String>> imageUrlsByPostId = loadImageUrlsByPostId(rows);

    List<PostFeedItemResponse> items = rows.stream()
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

    return new PostFeedResponse(items, page, size);

  }

  private List<PostFeedRow> loadFeedPosts(Long userId, Long page, Long size) {
    Long offset = page * size;

    if (postMapper.existsFeedPosts(userId)) {
      return postMapper.findFeedPosts(userId, size, offset);
    }

    return postMapper.findRecentFeedPosts(size, offset);
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
