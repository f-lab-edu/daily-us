package com.jaeychoi.dailyus.comment.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

  @Mock
  private CommentGetService commentGetService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentGetService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
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
  void getRepliesReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/comments/101/replies"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }
}
