package com.jaeychoi.dailyus.comment.service;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentDeleteService {

  private final CommentMapper commentMapper;

  @Transactional
  public void delete(Long userId, Long commentId) {
    Comment target = commentMapper.findActiveById(commentId);
    if (target == null) {
      throw new BaseException(ErrorCode.COMMENT_NOT_FOUND);
    }
    if (!target.getUserId().equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }

    boolean includeReplies = target.getParentId() == null;
    commentMapper.deleteCommentLikes(commentId, includeReplies);
    commentMapper.delete(commentId, includeReplies);
  }
}
