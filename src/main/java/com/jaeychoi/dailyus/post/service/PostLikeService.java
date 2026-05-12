package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostLikeResponse;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

  private final PostMapper postMapper;
  private final PostLikeRepository postLikeRepository;

  @Transactional
  public PostLikeResponse like(Long userId, Long postId) {
    validatePostExists(postId);

    try {
      postMapper.insertLike(postId, userId);
    } catch (DuplicateKeyException e) {
      throw new BaseException(ErrorCode.POST_LIKE_ALREADY_EXISTS);
    }

    long delta = postLikeRepository.incrementDelta(postId);

    return new PostLikeResponse(postId, true, getLikeCount(postId, delta));
  }

  @Transactional
  public PostLikeResponse unlike(Long userId, Long postId) {
    validatePostExists(postId);

    int deletedCount = postMapper.deleteLike(postId, userId);
    if (deletedCount == 0) {
      throw new BaseException(ErrorCode.POST_LIKE_NOT_FOUND);
    }

    long delta = postLikeRepository.decrementDelta(postId);

    return new PostLikeResponse(postId, false, getLikeCount(postId, delta));
  }

  private void validatePostExists(Long postId) {
    if (!postMapper.existsActiveById(postId)) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
  }

  private Long getLikeCount(Long postId, long delta) {
    return postMapper.findLikeCountByPostId(postId) + delta;
  }
}
