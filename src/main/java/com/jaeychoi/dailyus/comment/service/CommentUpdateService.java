package com.jaeychoi.dailyus.comment.service;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateResponse;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentUpdateService {

  private final CommentMapper commentMapper;

  @Transactional
  public CommentUpdateResponse update(Long userId, Long commentId, CommentUpdateRequest request) {
    Comment comment = commentMapper.findActiveCommentById(commentId);
    if (comment == null) {
      throw new BaseException(ErrorCode.COMMENT_NOT_FOUND);
    }
    if (!comment.getUserId().equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }

    commentMapper.updateContent(commentId, request.content());
    Comment updatedComment = commentMapper.findActiveCommentById(commentId);

    return new CommentUpdateResponse(
        updatedComment.getCommentId(),
        updatedComment.getContent(),
        true,
        updatedComment.getUpdatedAt()
    );
  }
}
