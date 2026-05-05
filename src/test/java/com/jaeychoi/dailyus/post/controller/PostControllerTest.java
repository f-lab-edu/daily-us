package com.jaeychoi.dailyus.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.dto.CommentResponse;
import com.jaeychoi.dailyus.comment.dto.CommentResponseItem;
import com.jaeychoi.dailyus.comment.service.CommentGetService;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
import com.jaeychoi.dailyus.post.dto.PostCreateRequest;
import com.jaeychoi.dailyus.post.dto.PostCreateResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedItemResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.post.service.PostCreateService;
import com.jaeychoi.dailyus.post.service.PostFeedService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

  @Mock
  private PostCreateService postCreateService;

  @Mock
  private PostFeedService postFeedService;

  @Mock
  private CommentGetService commentGetService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(
            new PostController(postCreateService, postFeedService, commentGetService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setValidator(validator)
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void createPostReturnsCreatedResponse() throws Exception {
    PostCreateRequest request = new PostCreateRequest(
        List.of("https://cdn.example.com/1.png"),
        "sample content #Morning"
    );
    PostCreateResponse response = new PostCreateResponse(
        10L,
        1L,
        request.content(),
        request.imageUrls(),
        List.of("morning"),
        0L
    );
    when(postCreateService.createPost(anyLong(), any(PostCreateRequest.class))).thenReturn(
        response);

    mockMvc.perform(post("/api/v1/posts")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.postId").value(10L))
        .andExpect(jsonPath("$.data.userId").value(1L))
        .andExpect(jsonPath("$.data.hashtags[0]").value("morning"));
  }

  @Test
  void createPostReturnsBadRequestWhenImagesAreEmpty() throws Exception {
    PostCreateRequest request = new PostCreateRequest(List.of(), "sample content");

    mockMvc.perform(post("/api/v1/posts")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void getFeedReturnsOkResponse() throws Exception {
    PostFeedResponse response = new PostFeedResponse(
        List.of(new PostFeedItemResponse(
            10L,
            2L,
            "author",
            "https://example.com/profile.png",
            "feed content",
            List.of("https://cdn.example.com/1.png", "https://cdn.example.com/2.png"),
            3L,
            LocalDateTime.of(2026, 4, 6, 10, 0)
        )),
        LocalDateTime.of(2026, 4, 6, 10, 0),
        10L,
        true,
        10L
    );
    when(postFeedService.getFeed(1L, null, null, 10L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/posts")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-04-06T10:00:00"))
        .andExpect(jsonPath("$.data.lastPostId").value(10L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(10L))
        .andExpect(jsonPath("$.data.items[0].postId").value(10L))
        .andExpect(jsonPath("$.data.items[0].imageUrls[0]").value("https://cdn.example.com/1.png"));
  }

  @Test
  void getFeedPassesCursorAndSizeQueryParameters() throws Exception {
    PostFeedResponse response = new PostFeedResponse(
        List.of(),
        LocalDateTime.of(2026, 4, 6, 8, 0),
        7L,
        true,
        5L
    );
    when(postFeedService.getFeed(
        1L,
        LocalDateTime.of(2026, 4, 6, 9, 0),
        15L,
        5L
    )).thenReturn(response);

    mockMvc.perform(get("/api/v1/posts")
            .queryParam("createdAt", "2026-04-06T09:00:00")
            .queryParam("postId", "15")
            .queryParam("size", "5")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-04-06T08:00:00"))
        .andExpect(jsonPath("$.data.lastPostId").value(7L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(5L));
  }

  @Test
  void getFeedReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/posts"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getCommentsReturnsOkResponse() throws Exception {
    CommentResponse response = new CommentResponse(
        List.of(new CommentResponseItem(
            101L,
            2L,
            "author",
            "https://example.com/profile.png",
            "comment",
            5L,
            true,
            LocalDateTime.of(2026, 4, 6, 10, 0),
            null,
            List.of(new CommentResponseItem(
                201L,
                3L,
                "replier",
                null,
                "reply",
                1L,
                false,
                LocalDateTime.of(2026, 4, 6, 10, 30),
                101L,
                List.of()
            ))
        )),
        LocalDateTime.of(2026, 4, 6, 10, 0),
        101L,
        true,
        10L
    );
    when(commentGetService.getComments(10L, 1L, null, null, 10L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/posts/10/comments")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-04-06T10:00:00"))
        .andExpect(jsonPath("$.data.lastCommentId").value(101L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.items[0].commentId").value(101L))
        .andExpect(jsonPath("$.data.items[0].likedByMe").value(true))
        .andExpect(jsonPath("$.data.items[0].replies[0].commentId").value(201L));
  }

  @Test
  void getCommentsPassesCursorAndSizeQueryParameters() throws Exception {
    CommentResponse response = new CommentResponse(
        List.of(),
        LocalDateTime.of(2026, 4, 6, 8, 0),
        99L,
        true,
        5L
    );
    when(commentGetService.getComments(
        10L,
        1L,
        LocalDateTime.of(2026, 4, 6, 9, 0),
        150L,
        5L
    )).thenReturn(response);

    mockMvc.perform(get("/api/v1/posts/10/comments")
            .queryParam("createdAt", "2026-04-06T09:00:00")
            .queryParam("commentId", "150")
            .queryParam("size", "5")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-04-06T08:00:00"))
        .andExpect(jsonPath("$.data.lastCommentId").value(99L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(5L));
  }

  @Test
  void getCommentsReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/posts/10/comments"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }
}
