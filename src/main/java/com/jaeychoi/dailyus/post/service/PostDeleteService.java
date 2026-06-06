package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class PostDeleteService {

  private final PostMapper postMapper;
  private final UserFollowMapper userFollowMapper;
  private final GroupMapper groupMapper;
  private final PostFeedCacheService postFeedCacheService;
  private final PostLikeRepository postLikeRepository;

  @Transactional
  public void deletePost(Long userId, Long postId) {
    Post post = postMapper.findById(postId);
    validateDeletable(userId, post);

    postMapper.deleteCommentLikesByPostId(postId);
    postMapper.deletePostLikesByPostId(postId);
    postMapper.deleteHashtagsByPostId(postId);
    postMapper.deleteCommentsByPostId(postId);
    postMapper.deleteImagesByPostId(postId);
    if (postMapper.delete(postId, userId) == 0) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }

    scheduleAfterCommit(() -> {
      postLikeRepository.clear(postId);
      removePostFromFeedCaches(post);
    });
  }

  private void validateDeletable(Long userId, Post post) {
    if (post == null) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
    if (!post.getUserId().equals(userId)) {
      throw new BaseException(ErrorCode.POST_DELETE_FORBIDDEN);
    }
  }

  private void removePostFromFeedCaches(Post post) {
    Set<Long> recipientUserIds = new LinkedHashSet<>();
    recipientUserIds.add(post.getUserId());
    recipientUserIds.addAll(userFollowMapper.findFollowerIdsByFollowee(post.getUserId()));
    recipientUserIds.addAll(groupMapper.findMembersByMemberId(post.getUserId()));

    postFeedCacheService.removePostFromFeeds(new ArrayList<>(recipientUserIds), post.getPostId());
  }

  private void scheduleAfterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        action.run();
      }
    });
  }
}
