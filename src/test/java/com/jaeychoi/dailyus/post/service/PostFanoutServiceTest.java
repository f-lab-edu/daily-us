package com.jaeychoi.dailyus.post.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.app.FeedCacheHybridProperties;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.post.event.PostCreatedEvent;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostFanoutServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private UserFollowMapper userFollowMapper;

  @Mock
  private GroupMapper groupMapper;

  @Mock
  private PostFeedCacheService postFeedCacheService;

  @Mock
  private FeedCacheHybridProperties feedCacheHybridProperties;

  @InjectMocks
  private PostFanoutService postFanoutService;

  @Test
  void fanoutAddsPostIdToAuthorFollowersAndGroupMembers() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 4, 26, 12, 0);
    PostCreatedEvent event = new PostCreatedEvent(15L, 3L, createdAt);
    when(userFollowMapper.findFollowerIdsByFollowee(3L)).thenReturn(List.of(9L, 7L, 3L));
    when(groupMapper.findMembersByMemberId(3L)).thenReturn(List.of(11L, 7L, 3L));

    postFanoutService.fanout(event);

    verify(postFeedCacheService).cachePostToAuthorFeed(3L, 15L, createdAt);
    verify(postFeedCacheService).cachePostToFeeds(List.of(3L, 9L, 7L, 11L), 15L, createdAt);
  }

  @Test
  void fanoutAddsPostIdOnlyToAuthorWhenAuthorIsHot() {
    LocalDateTime createdAt = LocalDateTime.of(2026, 4, 26, 12, 0);
    PostCreatedEvent event = new PostCreatedEvent(15L, 3L, createdAt);
    when(feedCacheHybridProperties.enabled()).thenReturn(true);
    when(feedCacheHybridProperties.hotAuthorThreshold()).thenReturn(10000L);
    when(userMapper.findFollowerCountByUserId(3L)).thenReturn(10000L);

    postFanoutService.fanout(event);

    verify(postFeedCacheService).cachePostToAuthorFeed(3L, 15L, createdAt);
    verify(postFeedCacheService).cachePostToFeeds(List.of(3L), 15L, createdAt);
    verify(userFollowMapper, never()).findFollowerIdsByFollowee(3L);
    verify(groupMapper, never()).findMembersByMemberId(3L);
  }
}
