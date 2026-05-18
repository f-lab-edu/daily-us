package com.jaeychoi.dailyus.comment.service;

import com.jaeychoi.dailyus.comment.domain.Comment;
import com.jaeychoi.dailyus.comment.dto.CommentCreateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentCreateResponse;
import com.jaeychoi.dailyus.comment.mapper.CommentMapper;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentCreateService {

  private final CommentMapper commentMapper;

  @Transactional
  public CommentCreateResponse create(Long postId, Long userId, CommentCreateRequest request) {
    validatePostExists(postId);
    validateParent(postId, request.parentId());

    Comment comment = Comment.builder()
        .content(request.content())
        .userId(userId)
        .postId(postId)
        .parentId(request.parentId())
        .build();

    commentMapper.insert(comment);

    Comment savedComment = commentMapper.findActiveCommentById(comment.getCommentId());
    if (savedComment == null) {
      throw new IllegalStateException("Saved comment must exist after insert.");
    }

    return new CommentCreateResponse(
        savedComment.getCommentId(),
        savedComment.getPostId(),
        savedComment.getUserId(),
        savedComment.getContent(),
        savedComment.getParentId(),
        savedComment.getLikeCount(),
        savedComment.getCreatedAt()
    );
  }

  private void validatePostExists(Long postId) {
    if (!commentMapper.existsActivePostById(postId)) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
  }

  private void validateParent(Long postId, Long parentId) {
    if (parentId == null) {
      return;
    }

    Comment parentComment = commentMapper.findActiveCommentById(parentId);
    if (parentComment == null) {
      throw new BaseException(ErrorCode.COMMENT_NOT_FOUND);
    }
    if (!parentComment.getPostId().equals(postId)) {
      throw new BaseException(ErrorCode.COMMENT_PARENT_MISMATCH);
    }
    if (parentComment.getParentId() != null) {
      throw new BaseException(ErrorCode.COMMENT_REPLY_DEPTH_EXCEEDED);
    }
  }
}
