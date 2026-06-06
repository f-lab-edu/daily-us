package com.jaeychoi.dailyus.comment.domain;

import java.time.LocalDateTime;
public class Comment {

  private Long commentId;
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;
  private Long likeCount = 0L;
  private Long userId;
  private Long postId;
  private Long parentId;

  public Comment() {
  }

  public Comment(Long commentId, String content, LocalDateTime createdAt, LocalDateTime updatedAt,
      LocalDateTime deletedAt, Long likeCount, Long userId, Long postId, Long parentId) {
    this.commentId = commentId;
    this.content = content;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deletedAt = deletedAt;
    this.likeCount = likeCount;
    this.userId = userId;
    this.postId = postId;
    this.parentId = parentId;
  }

  public static CommentBuilder builder() {
    return new CommentBuilder();
  }

  public Long getCommentId() {
    return commentId;
  }

  public void setCommentId(Long commentId) {
    this.commentId = commentId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Long getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(Long likeCount) {
    this.likeCount = likeCount;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getPostId() {
    return postId;
  }

  public void setPostId(Long postId) {
    this.postId = postId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public static final class CommentBuilder {
    private Long commentId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long likeCount = 0L;
    private Long userId;
    private Long postId;
    private Long parentId;

    private CommentBuilder() {
    }

    public CommentBuilder commentId(Long commentId) {
      this.commentId = commentId;
      return this;
    }

    public CommentBuilder content(String content) {
      this.content = content;
      return this;
    }

    public CommentBuilder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public CommentBuilder updatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public CommentBuilder deletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public CommentBuilder likeCount(Long likeCount) {
      this.likeCount = likeCount;
      return this;
    }

    public CommentBuilder userId(Long userId) {
      this.userId = userId;
      return this;
    }

    public CommentBuilder postId(Long postId) {
      this.postId = postId;
      return this;
    }

    public CommentBuilder parentId(Long parentId) {
      this.parentId = parentId;
      return this;
    }

    public Comment build() {
      return new Comment(commentId, content, createdAt, updatedAt, deletedAt, likeCount, userId,
          postId, parentId);
    }
  }
}
