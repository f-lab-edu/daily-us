package com.jaeychoi.dailyus.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.hashtag.domain.Hashtag;
import com.jaeychoi.dailyus.hashtag.mapper.HashtagMapper;
import com.jaeychoi.dailyus.post.domain.Post;
import com.jaeychoi.dailyus.post.dto.PostCreateRequest;
import com.jaeychoi.dailyus.post.dto.PostCreateResponse;
import com.jaeychoi.dailyus.post.event.PostCreatedEvent;
import com.jaeychoi.dailyus.post.event.PostCreatedEventPublisher;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCreateServiceTest {

  private static final LocalDateTime SAVED_POST_CREATED_AT = LocalDateTime.of(2026, 4, 26, 12, 0);

  @Mock
  private PostMapper postMapper;

  @Mock
  private HashtagMapper hashtagMapper;

  @Mock
  private PostCreatedEventPublisher postCreatedEventPublisher;

  @InjectMocks
  private PostCreateService postCreateService;

  @Test
  void createPostPersistsPostImagesAndHashtags() {
    PostCreateRequest request = new PostCreateRequest(
        List.of("https://cdn.example.com/1.png", "https://cdn.example.com/2.png"),
        "자바 매일 #Morning #Routine"
    );
    doAnswer(invocation -> {
      Post post = invocation.getArgument(0);
      post.setPostId(10L);
      return null;
    }).when(postMapper).insert(any(Post.class));
    stubSavedPostLookup(10L, 1L);
    when(hashtagMapper.findByNames(List.of("morning", "routine")))
        .thenReturn(List.of(Hashtag.builder().hashtagId(30L).name("routine").build()));
    doAnswer(invocation -> {
      Hashtag hashtag = invocation.getArgument(0);
      hashtag.setHashtagId(20L);
      return null;
    }).when(hashtagMapper).insert(any(Hashtag.class));

    PostCreateResponse response = postCreateService.createPost(1L, request);

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postMapper).insert(postCaptor.capture());
    verify(postMapper).insertImages(10L, request.imageUrls());
    verify(hashtagMapper).insertPostHashtags(10L, List.of(20L, 30L));
    verify(postCreatedEventPublisher).publish(any(PostCreatedEvent.class));

    assertThat(postCaptor.getValue().getUserId()).isEqualTo(1L);
    assertThat(postCaptor.getValue().getContent()).isEqualTo(request.content());
    assertThat(response.postId()).isEqualTo(10L);
    assertThat(response.imageUrls()).containsExactlyElementsOf(request.imageUrls());
    assertThat(response.hashtags()).containsExactly("morning", "routine");
  }

  @Test
  void createPostThrowsWhenImagesAreMissing() {
    PostCreateRequest request = new PostCreateRequest(
        List.of(),
        "text"
    );

    assertThatThrownBy(() -> postCreateService.createPost(99L, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_IMAGE_REQUIRED);

    verify(postMapper, never()).insert(any(Post.class));
  }

  @Test
  void createPostNormalizesNewHashtagsBeforeInsert() {
    PostCreateRequest request = new PostCreateRequest(
        List.of("https://cdn.example.com/1.png"),
        "#Morning #ROUTINE"
    );
    doAnswer(invocation -> {
      Post post = invocation.getArgument(0);
      post.setPostId(10L);
      return null;
    }).when(postMapper).insert(any(Post.class));
    stubSavedPostLookup(10L, 1L);
    when(hashtagMapper.findByNames(List.of("morning", "routine"))).thenReturn(List.of());
    doAnswer(invocation -> {
      Hashtag hashtag = invocation.getArgument(0);
      if ("morning".equals(hashtag.getName())) {
        hashtag.setHashtagId(21L);
      } else if ("routine".equals(hashtag.getName())) {
        hashtag.setHashtagId(22L);
      }
      return null;
    }).when(hashtagMapper).insert(any(Hashtag.class));

    postCreateService.createPost(1L, request);

    ArgumentCaptor<Hashtag> hashtagCaptor = ArgumentCaptor.forClass(Hashtag.class);
    verify(hashtagMapper, times(2)).insert(hashtagCaptor.capture());
    assertThat(hashtagCaptor.getAllValues())
        .extracting(Hashtag::getName)
        .containsExactly("morning", "routine");
    verify(hashtagMapper).insertPostHashtags(10L, List.of(21L, 22L));
    verify(postCreatedEventPublisher).publish(any(PostCreatedEvent.class));
  }

  @Test
  void createPostDeduplicatesHashtagsIgnoringCase() {
    PostCreateRequest request = new PostCreateRequest(
        List.of("https://cdn.example.com/1.png"),
        "#Daily #daily #DAILY #Routine"
    );
    doAnswer(invocation -> {
      Post post = invocation.getArgument(0);
      post.setPostId(10L);
      return null;
    }).when(postMapper).insert(any(Post.class));
    stubSavedPostLookup(10L, 1L);
    when(hashtagMapper.findByNames(List.of("daily", "routine")))
        .thenReturn(List.of(
            Hashtag.builder().hashtagId(11L).name("daily").build(),
            Hashtag.builder().hashtagId(12L).name("routine").build()
        ));

    PostCreateResponse response = postCreateService.createPost(1L, request);

    verify(postMapper).insert(any(Post.class));
    verify(postMapper).insertImages(10L, request.imageUrls());
    verify(hashtagMapper).insertPostHashtags(10L, List.of(11L, 12L));
    verify(postCreatedEventPublisher).publish(any(PostCreatedEvent.class));
    assertThat(response.hashtags()).containsExactly("daily", "routine");
  }

  @Test
  void createPostSkipsHashtagPersistenceWhenContentHasNoHashtags() {
    PostCreateRequest request = new PostCreateRequest(
        List.of("https://cdn.example.com/1.png"),
        "plain text only"
    );
    doAnswer(invocation -> {
      Post post = invocation.getArgument(0);
      post.setPostId(10L);
      return null;
    }).when(postMapper).insert(any(Post.class));
    stubSavedPostLookup(10L, 1L);

    PostCreateResponse response = postCreateService.createPost(1L, request);

    verify(postMapper).insert(any(Post.class));
    verify(postMapper).insertImages(10L, request.imageUrls());
    verifyNoInteractions(hashtagMapper);
    verify(postCreatedEventPublisher).publish(any(PostCreatedEvent.class));
    assertThat(response.hashtags()).isEmpty();
  }

  @Test
  void createPostThrowsWhenHashtagLimitIsExceeded() {
    PostCreateRequest request = new PostCreateRequest(
        List.of("https://cdn.example.com/1.png"),
        "#one #two #three #four #five #six #seven #eight #nine #ten #eleven"
    );

    assertThatThrownBy(() -> postCreateService.createPost(1L, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.POST_HASHTAG_LIMIT_EXCEEDED);

    verify(postMapper, never()).insert(any(Post.class));
  }

  private void stubSavedPostLookup(Long postId, Long userId) {
    when(postMapper.findById(postId))
        .thenReturn(Post.builder()
            .postId(postId)
            .userId(userId)
            .createdAt(SAVED_POST_CREATED_AT)
            .build());
  }
}
