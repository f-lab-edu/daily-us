package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostFeedItemResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
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
public class UserPostService {

  private static final long DEFAULT_SIZE = 10L;

  private final UserMapper userMapper;
  private final PostMapper postMapper;

  public PostFeedResponse getMyPosts(Long userId, LocalDateTime createdAt, Long postId, Long size) {
    validateUser(userId);

    long pageSize = resolvePageSize(size);
    List<PostFeedRow> rows = postMapper.findPostsByUserId(userId, pageSize + 1, createdAt, postId);
    boolean hasNext = rows.size() > pageSize;
    List<PostFeedRow> pageRows = hasNext ? rows.subList(0, (int) pageSize) : rows;
    Map<Long, List<String>> imageUrlsByPostId = loadImageUrlsByPostId(pageRows);
    PostFeedRow lastRow = hasNext ? pageRows.get(pageRows.size() - 1) : null;

    return new PostFeedResponse(
        pageRows.stream()
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
            .toList(),
        lastRow == null ? null : lastRow.createdAt(),
        lastRow == null ? null : lastRow.postId(),
        hasNext,
        pageSize
    );
  }

  private void validateUser(Long userId) {
    if (!userMapper.existsActiveById(userId)) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }
  }

  private long resolvePageSize(Long size) {
    if (size == null || size <= 0) {
      return DEFAULT_SIZE;
    }
    return size;
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
