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
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

  @Mock
  private PostMapper postMapper;

  @Mock
  private PostLikeRepository postLikeRepository;

  @InjectMocks
  private PostLikeService postLikeService;

  @Test
  void likeCreatesPostLikeAndIncrementsCount() {
    when(postMapper.existsActiveById(10L)).thenReturn(true);
    when(postMapper.findLikeCountByPostId(10L)).thenReturn(3L);
    when(postLikeRepository.incrementDelta(10L)).thenReturn(1L);

    PostLikeResponse response = postLikeService.like(1L, 10L);

    verify(postMapper).insertLike(10L, 1L);
    verify(postLikeRepository).incrementDelta(10L);
    assertThat(response).isEqualTo(new PostLikeResponse(10L, true, 4L));
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
    org.mockito.Mockito.doThrow(new DuplicateKeyException("duplicate"))
        .when(postMapper).insertLike(10L, 1L);

    assertThatThrownBy(() -> postLikeService.like(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_LIKE_ALREADY_EXISTS);

    verify(postLikeRepository, never()).incrementDelta(10L);
  }

  @Test
  void unlikeDeletesPostLikeAndDecrementsCount() {
    when(postMapper.existsActiveById(10L)).thenReturn(true);
    when(postMapper.deleteLike(10L, 1L)).thenReturn(1);
    when(postMapper.findLikeCountByPostId(10L)).thenReturn(2L);
    when(postLikeRepository.decrementDelta(10L)).thenReturn(-1L);

    PostLikeResponse response = postLikeService.unlike(1L, 10L);

    verify(postLikeRepository).decrementDelta(10L);
    assertThat(response).isEqualTo(new PostLikeResponse(10L, false, 1L));
  }

  @Test
  void unlikeThrowsWhenLikeDoesNotExist() {
    when(postMapper.existsActiveById(10L)).thenReturn(true);
    when(postMapper.deleteLike(10L, 1L)).thenReturn(0);

    assertThatThrownBy(() -> postLikeService.unlike(1L, 10L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_LIKE_NOT_FOUND);

    verify(postLikeRepository, never()).decrementDelta(10L);
  }
}
