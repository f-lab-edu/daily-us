package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostDetailResponse;
import com.jaeychoi.dailyus.post.dto.PostDetailRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostDetailService {

  private final PostMapper postMapper;

  public PostDetailResponse getDetail(Long userId, Long postId) {
    PostDetailRow detailRow = getActivePostDetail(userId, postId);
    return toResponse(detailRow, loadImageUrls(postId));
  }

  private PostDetailRow getActivePostDetail(Long userId, Long postId) {
    PostDetailRow detailRow = postMapper.findDetailById(postId, userId);
    if (detailRow == null) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
    return detailRow;
  }

  private List<String> loadImageUrls(Long postId) {
    return postMapper.findImagesByPostIds(List.of(postId)).stream()
        .map(PostImageRow::imageUrl)
        .collect(Collectors.toList());
  }

  private PostDetailResponse toResponse(PostDetailRow row, List<String> imageUrls) {
    return new PostDetailResponse(
        row.postId(),
        row.userId(),
        row.nickname(),
        row.profileImage(),
        row.content(),
        imageUrls == null ? Collections.emptyList() : imageUrls,
        row.likeCount(),
        Boolean.TRUE.equals(row.likedByMe()),
        row.createdAt()
    );
  }
}
