package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.app.FeedCacheHybridProperties;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.post.event.PostCreatedEvent;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostFanoutService {

  private final UserMapper userMapper;
  private final UserFollowMapper userFollowMapper;
  private final GroupMapper groupMapper;
  private final PostFeedCacheService postFeedCacheService;
  private final FeedCacheHybridProperties feedCacheHybridProperties;

  public void fanout(PostCreatedEvent event) {
    postFeedCacheService.cachePostToAuthorFeed(event.userId(), event.postId(), event.createdAt());

    if (isHotAuthor(event.userId())) {
      postFeedCacheService.cachePostToFeeds(
          List.of(event.userId()),
          event.postId(),
          event.createdAt()
      );
      return;
    }

    Set<Long> recipientUserIds = new LinkedHashSet<>();
    recipientUserIds.add(event.userId());
    recipientUserIds.addAll(userFollowMapper.findFollowerIdsByFollowee(event.userId()));
    recipientUserIds.addAll(groupMapper.findMembersByMemberId(event.userId()));

    postFeedCacheService.cachePostToFeeds(
        new ArrayList<>(recipientUserIds),
        event.postId(),
        event.createdAt()
    );
  }

  private boolean isHotAuthor(Long userId) {
    if (!feedCacheHybridProperties.enabled()) {
      return false;
    }

    Long followerCount = userMapper.findFollowerCountByUserId(userId);
    return followerCount != null && followerCount >= feedCacheHybridProperties.hotAuthorThreshold();
  }
}
