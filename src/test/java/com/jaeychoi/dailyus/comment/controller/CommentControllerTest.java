package com.jaeychoi.dailyus.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.dto.CommentLikeResponse;
import com.jaeychoi.dailyus.comment.dto.CommentResponse;
import com.jaeychoi.dailyus.comment.dto.CommentResponseItem;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateResponse;
import com.jaeychoi.dailyus.comment.service.CommentDeleteService;
import com.jaeychoi.dailyus.comment.service.CommentGetService;
import com.jaeychoi.dailyus.comment.service.CommentLikeService;
import com.jaeychoi.dailyus.comment.service.CommentUpdateService;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
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
class CommentControllerTest {

  @Mock
  private CommentDeleteService commentDeleteService;
  @Mock
  private CommentGetService commentGetService;
  @Mock
  private CommentUpdateService commentUpdateService;
  @Mock
  private CommentLikeService commentLikeService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(
            new CommentController(
                commentDeleteService,
                commentUpdateService,
                commentGetService,
                commentLikeService
            ))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setValidator(validator)
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void deleteReturnsOkResponse() throws Exception {
    mockMvc.perform(delete("/api/v1/comments/10")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data").doesNotExist());

    verify(commentDeleteService).delete(1L, 10L);
  }

  @Test
  void deleteReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(delete("/api/v1/comments/10"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getRepliesReturnsOkResponse() throws Exception {
    CommentResponse response = new CommentResponse(
        List.of(new CommentResponseItem(
            201L,
            2L,
            "replier",
            null,
            "reply",
            1L,
            false,
            LocalDateTime.of(2026, 4, 6, 10, 30),
            false,
            101L,
            List.of()
        )),
        LocalDateTime.of(2026, 4, 6, 10, 30),
        201L,
        true,
        3L
    );
    when(commentGetService.getReplies(
        101L,
        1L,
        LocalDateTime.of(2026, 4, 6, 11, 0),
        300L,
        3L
    )).thenReturn(response);

    mockMvc.perform(get("/api/v1/comments/101/replies")
            .queryParam("createdAt", "2026-04-06T11:00:00")
            .queryParam("replyId", "300")
            .queryParam("size", "3")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-04-06T10:30:00"))
        .andExpect(jsonPath("$.data.lastCommentId").value(201L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.items[0].commentId").value(201L));
  }

  @Test
  void updateCommentReturnsOkResponse() throws Exception {
    CommentUpdateRequest request = new CommentUpdateRequest("updated comment");
    CommentUpdateResponse response = new CommentUpdateResponse(
        10L,
        "updated comment",
        true
    );
    when(commentUpdateService.update(eq(1L), eq(10L), any(CommentUpdateRequest.class)))
        .thenReturn(response);

    mockMvc.perform(patch("/api/v1/comments/10")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.commentId").value(10L))
        .andExpect(jsonPath("$.data.content").value("updated comment"))
        .andExpect(jsonPath("$.data.edited").value(true));
  }

  @Test
  void updateCommentReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    CommentUpdateRequest request = new CommentUpdateRequest("updated comment");

    mockMvc.perform(patch("/api/v1/comments/10")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
  }

  @Test
  void likeCommentReturnsCreatedResponse() throws Exception {
    when(commentLikeService.like(1L, 10L)).thenReturn(new CommentLikeResponse(10L, true, 3L));

    mockMvc.perform(post("/api/v1/comments/10/like")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.commentId").value(10L))
        .andExpect(jsonPath("$.data.liked").value(true))
        .andExpect(jsonPath("$.data.likeCount").value(3L));
  }

  @Test
  void unlikeCommentReturnsOkResponse() throws Exception {
    when(commentLikeService.unlike(1L, 10L)).thenReturn(new CommentLikeResponse(10L, false, 2L));

    mockMvc.perform(delete("/api/v1/comments/10/like")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.commentId").value(10L))
        .andExpect(jsonPath("$.data.liked").value(false))
        .andExpect(jsonPath("$.data.likeCount").value(2L));
  }

  @Test
  void getRepliesReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/comments/101/replies"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void likeCommentReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(post("/api/v1/comments/10/like"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }
}
