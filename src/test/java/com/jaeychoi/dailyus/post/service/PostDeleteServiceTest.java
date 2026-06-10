package com.jaeychoi.dailyus.post.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.app.FeedCacheHybridProperties;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostDeleteServiceTest {

  @Mock
  private PostMapper postMapper;

  @Mock
  private UserFollowMapper userFollowMapper;

  @Mock
  private UserMapper userMapper;

  @Mock
  private GroupMapper groupMapper;

  @Mock
  private PostFeedCacheService postFeedCacheService;

  @Mock
  private PostLikeRepository postLikeRepository;

  @Mock
  private FeedCacheHybridProperties feedCacheHybridProperties;

  @InjectMocks
  private PostDeleteService postDeleteService;

  @Test
  void deletePostSoftDeletesPostAndRelatedDataAndEvictsCaches() {
    when(postMapper.findById(10L)).thenReturn(Post.builder().postId(10L).userId(1L).build());
    when(userFollowMapper.findFollowerIdsByFollowee(1L)).thenReturn(List.of(2L, 3L));
    when(groupMapper.findMembersByMemberId(1L)).thenReturn(List.of(1L, 4L));
    when(postMapper.delete(10L, 1L)).thenReturn(1);

    postDeleteService.deletePost(1L, 10L);

    verify(postMapper).deleteCommentLikesByPostId(10L);
    verify(postMapper).deletePostLikesByPostId(10L);
    verify(postMapper).deleteHashtagsByPostId(10L);
    verify(postMapper).deleteCommentsByPostId(10L);
    verify(postMapper).deleteImagesByPostId(10L);
    verify(postMapper).delete(10L, 1L);
    verify(postLikeRepository).clear(10L);
    verify(postFeedCacheService).removePostFromAuthorFeed(1L, 10L);
    verify(postFeedCacheService).removePostFromFeeds(List.of(1L, 2L, 3L, 4L), 10L);
  }

  @Test
  void deletePostDoesNotScanFollowersWhenAuthorIsHot() {
    when(postMapper.findById(10L)).thenReturn(Post.builder().postId(10L).userId(1L).build());
    when(postMapper.delete(10L, 1L)).thenReturn(1);
    when(feedCacheHybridProperties.enabled()).thenReturn(true);
    when(feedCacheHybridProperties.hotAuthorThreshold()).thenReturn(10000L);
    when(userMapper.findFollowerCountByUserId(1L)).thenReturn(10000L);

    postDeleteService.deletePost(1L, 10L);

    verify(postFeedCacheService).removePostFromAuthorFeed(1L, 10L);
    verify(postFeedCacheService).removePostFromFeeds(List.of(1L), 10L);
    verify(userFollowMapper, never()).findFollowerIdsByFollowee(1L);
    verify(groupMapper, never()).findMembersByMemberId(1L);
  }

  @Test
  void deletePostThrowsWhenPostDoesNotExist() {
    when(postMapper.findById(10L)).thenReturn(null);

    assertThatThrownBy(() -> postDeleteService.deletePost(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_NOT_FOUND);

    verify(postMapper, never()).delete(10L, 1L);
    verify(postLikeRepository, never()).clear(10L);
  }

  @Test
  void deletePostThrowsWhenUserIsNotAuthor() {
    when(postMapper.findById(10L)).thenReturn(Post.builder().postId(10L).userId(99L).build());

    assertThatThrownBy(() -> postDeleteService.deletePost(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_DELETE_FORBIDDEN);

    verify(postMapper, never()).delete(10L, 1L);
    verify(postLikeRepository, never()).clear(10L);
  }
}
