package com.jaeychoi.dailyus.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostLikeResponse;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private PostLikeService postLikeService;

  @Test
  void likeCreatesPostLikeAndIncrementsCount() {
    when(postMapper.existsActiveById(10L)).thenReturn(true);
    when(postMapper.countLikesByPostIdAndUserId(10L, 1L)).thenReturn(0);
    when(postMapper.findLikeCountByPostId(10L)).thenReturn(3L);

    PostLikeResponse response = postLikeService.like(1L, 10L);

    verify(postMapper).insertLike(10L, 1L);
    verify(postMapper).incrementLikeCount(10L);
    assertThat(response).isEqualTo(new PostLikeResponse(10L, true, 3L));
  }

  @Test
  void likeThrowsWhenPostDoesNotExist() {
    when(postMapper.existsActiveById(10L)).thenReturn(false);

    assertThatThrownBy(() -> postLikeService.like(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_NOT_FOUND);

    verify(postMapper, never()).insertLike(10L, 1L);
  }

  @Test
  void likeThrowsWhenLikeAlreadyExists() {
    when(postMapper.existsActiveById(10L)).thenReturn(true);
    when(postMapper.countLikesByPostIdAndUserId(10L, 1L)).thenReturn(1);

    assertThatThrownBy(() -> postLikeService.like(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_LIKE_ALREADY_EXISTS);

    verify(postMapper, never()).insertLike(10L, 1L);
  }

  @Test
  void unlikeDeletesPostLikeAndDecrementsCount() {
    when(postMapper.existsActiveById(10L)).thenReturn(true);
    when(postMapper.deleteLike(10L, 1L)).thenReturn(1);
    when(postMapper.findLikeCountByPostId(10L)).thenReturn(2L);

    PostLikeResponse response = postLikeService.unlike(1L, 10L);

    verify(postMapper).decrementLikeCount(10L);
    assertThat(response).isEqualTo(new PostLikeResponse(10L, false, 2L));
  }

  @Test
  void unlikeThrowsWhenLikeDoesNotExist() {
    when(postMapper.existsActiveById(10L)).thenReturn(true);
    when(postMapper.deleteLike(10L, 1L)).thenReturn(0);

    assertThatThrownBy(() -> postLikeService.unlike(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_LIKE_NOT_FOUND);

    verify(postMapper, never()).decrementLikeCount(10L);
  }
}
