package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostLikeResponse;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

  private final PostMapper postMapper;

  @Transactional
  public PostLikeResponse like(Long userId, Long postId) {
    validatePostExists(postId);

    if (postMapper.countLikesByPostIdAndUserId(postId, userId) > 0) {
      throw new BaseException(ErrorCode.POST_LIKE_ALREADY_EXISTS);
    }

    postMapper.insertLike(postId, userId);
    postMapper.incrementLikeCount(postId);

    return new PostLikeResponse(postId, true, getLikeCount(postId));
  }

  @Transactional
  public PostLikeResponse unlike(Long userId, Long postId) {
    validatePostExists(postId);

    int deletedCount = postMapper.deleteLike(postId, userId);
    if (deletedCount == 0) {
      throw new BaseException(ErrorCode.POST_LIKE_NOT_FOUND);
    }

    postMapper.decrementLikeCount(postId);

    return new PostLikeResponse(postId, false, getLikeCount(postId));
  }

  private void validatePostExists(Long postId) {
    if (!postMapper.existsActiveById(postId)) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
  }

  private Long getLikeCount(Long postId) {
    return postMapper.findLikeCountByPostId(postId);
  }
}
