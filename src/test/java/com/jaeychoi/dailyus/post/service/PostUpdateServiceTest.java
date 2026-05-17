package com.jaeychoi.dailyus.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.hashtag.domain.Hashtag;
import com.jaeychoi.dailyus.hashtag.mapper.HashtagMapper;
import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostUpdateRequest;
import com.jaeychoi.dailyus.post.dto.PostUpdateResponse;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostUpdateServiceTest {

  @Mock
  private PostMapper postMapper;

  @Mock
  private HashtagMapper hashtagMapper;

  @InjectMocks
  private PostUpdateService postUpdateService;

  @Test
  void updatePostReplacesContentImagesAndHashtags() {
    PostUpdateRequest request = new PostUpdateRequest(
        List.of("https://cdn.example.com/new-1.png", "https://cdn.example.com/new-2.png"),
        "updated content #Morning #Routine"
    );
    when(postMapper.findById(10L)).thenReturn(Post.builder()
        .postId(10L)
        .userId(1L)
        .likeCount(3L)
        .build());
    when(hashtagMapper.findByNames(List.of("morning", "routine")))
        .thenReturn(List.of(Hashtag.builder().hashtagId(20L).name("routine").build()));
    doAnswer(invocation -> {
      Hashtag hashtag = invocation.getArgument(0);
      hashtag.setHashtagId(11L);
      return null;
    }).when(hashtagMapper).insert(any(Hashtag.class));

    PostUpdateResponse response = postUpdateService.updatePost(1L, 10L, request);

    verify(postMapper).updateContent(10L, request.content());
    verify(postMapper).deleteImagesByPostId(10L);
    verify(postMapper).insertImages(10L, request.imageUrls());
    verify(hashtagMapper).deletePostHashtags(10L);
    verify(hashtagMapper).insertPostHashtags(10L, List.of(11L, 20L));
    assertThat(response.postId()).isEqualTo(10L);
    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.content()).isEqualTo(request.content());
    assertThat(response.imageUrls()).containsExactlyElementsOf(request.imageUrls());
    assertThat(response.hashtags()).containsExactly("morning", "routine");
    assertThat(response.likeCount()).isEqualTo(3L);
  }

  @Test
  void updatePostDeletesExistingHashtagMappingsWhenContentHasNoHashtags() {
    PostUpdateRequest request = new PostUpdateRequest(
        List.of("https://cdn.example.com/new-1.png"),
        "updated content"
    );
    when(postMapper.findById(10L)).thenReturn(Post.builder()
        .postId(10L)
        .userId(1L)
        .likeCount(3L)
        .build());

    PostUpdateResponse response = postUpdateService.updatePost(1L, 10L, request);

    verify(hashtagMapper).deletePostHashtags(10L);
    verify(hashtagMapper, never()).findByNames(any());
    verify(hashtagMapper, never()).insertPostHashtags(any(), any());
    assertThat(response.hashtags()).isEmpty();
  }

  @Test
  void updatePostThrowsWhenImagesAreMissing() {
    PostUpdateRequest request = new PostUpdateRequest(List.of(), "updated content");

    assertThatThrownBy(() -> postUpdateService.updatePost(1L, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_IMAGE_REQUIRED);
  }

  @Test
  void updatePostThrowsWhenPostDoesNotExist() {
    PostUpdateRequest request = new PostUpdateRequest(
        List.of("https://cdn.example.com/new-1.png"),
        "updated content"
    );
    when(postMapper.findById(10L)).thenReturn(null);

    assertThatThrownBy(() -> postUpdateService.updatePost(1L, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_NOT_FOUND);
  }

  @Test
  void updatePostThrowsWhenUserIsNotOwner() {
    PostUpdateRequest request = new PostUpdateRequest(
        List.of("https://cdn.example.com/new-1.png"),
        "updated content"
    );
    when(postMapper.findById(10L)).thenReturn(Post.builder()
        .postId(10L)
        .userId(2L)
        .likeCount(3L)
        .build());

    assertThatThrownBy(() -> postUpdateService.updatePost(1L, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }
}
