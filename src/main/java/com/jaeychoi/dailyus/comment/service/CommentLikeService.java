package com.jaeychoi.dailyus.comment.service;

import com.jaeychoi.dailyus.comment.dto.CommentLikeResponse;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

  private final CommentMapper commentMapper;

  @Transactional
  public CommentLikeResponse like(Long userId, Long commentId) {
    validateCommentExists(commentId);

    try {
      commentMapper.insertLike(commentId, userId);
    } catch (DuplicateKeyException e) {
      throw new BaseException(ErrorCode.COMMENT_LIKE_ALREADY_EXISTS);
    }
    commentMapper.applyLikeCountDelta(commentId, 1L);

    return new CommentLikeResponse(commentId, true, getLikeCount(commentId));
  }

  @Transactional
  public CommentLikeResponse unlike(Long userId, Long commentId) {
    validateCommentExists(commentId);

    int deletedCount = commentMapper.deleteLike(commentId, userId);
    if (deletedCount == 0) {
      throw new BaseException(ErrorCode.COMMENT_LIKE_NOT_FOUND);
    }
    commentMapper.applyLikeCountDelta(commentId, -1L);

    return new CommentLikeResponse(commentId, false, getLikeCount(commentId));
  }

  private void validateCommentExists(Long commentId) {
    if (commentMapper.findActiveById(commentId) == null) {
      throw new BaseException(ErrorCode.COMMENT_NOT_FOUND);
    }
  }

  private Long getLikeCount(Long commentId) {
    return commentMapper.findLikeCountByCommentId(commentId);
  }
}
