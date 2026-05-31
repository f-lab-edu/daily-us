package com.jaeychoi.dailyus.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostDetailResponse;
import com.jaeychoi.dailyus.post.dto.PostDetailRow;
import com.jaeychoi.dailyus.post.dto.PostImageRow;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostDetailServiceTest {

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private PostDetailService postDetailService;

  @Test
  void getDetailReturnsPostDetailResponse() {
    when(postMapper.findDetailById(10L, 1L)).thenReturn(new PostDetailRow(
        10L,
        2L,
        "author",
        "https://example.com/profile.png",
        "detail content",
        7L,
        true,
        LocalDateTime.of(2026, 5, 17, 9, 0)
    ));
    when(postMapper.findImagesByPostIds(List.of(10L))).thenReturn(List.of(
        new PostImageRow(10L, "https://cdn.example.com/1.png"),
        new PostImageRow(10L, "https://cdn.example.com/2.png")
    ));

    PostDetailResponse response = postDetailService.getDetail(1L, 10L);

    assertThat(response.postId()).isEqualTo(10L);
    assertThat(response.userId()).isEqualTo(2L);
    assertThat(response.nickname()).isEqualTo("author");
    assertThat(response.imageUrls()).containsExactly(
        "https://cdn.example.com/1.png",
        "https://cdn.example.com/2.png"
    );
    assertThat(response.likeCount()).isEqualTo(7L);
    assertThat(response.likedByMe()).isTrue();
  }

  @Test
  void getDetailThrowsWhenPostDoesNotExist() {
    when(postMapper.findDetailById(10L, 1L)).thenReturn(null);

    assertThatThrownBy(() -> postDetailService.getDetail(1L, 10L))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POST_NOT_FOUND));
  }
}
