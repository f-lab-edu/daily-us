package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.post.event.PostCreatedEvent;
import com.jaeychoi.dailyus.post.repository.PostFeedRepository;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostFanoutService {

  private final UserFollowMapper userFollowMapper;
  private final GroupMapper groupMapper;
  private final PostFeedRepository postFeedRepository;

  public void fanout(PostCreatedEvent event) {
    Set<Long> recipientUserIds = new LinkedHashSet<>();
    recipientUserIds.add(event.userId());
    recipientUserIds.addAll(userFollowMapper.findFollowerIdsByFollowee(event.userId()));
    recipientUserIds.addAll(groupMapper.findMembersByMemberId(event.userId()));

    postFeedRepository.addPostIdToFeeds(
        new ArrayList<>(recipientUserIds),
        event.postId(),
        event.createdAt()
    );
  }
}
