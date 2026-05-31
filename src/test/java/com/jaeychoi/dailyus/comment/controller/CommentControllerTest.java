package com.jaeychoi.dailyus.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateRequest;
import com.jaeychoi.dailyus.comment.dto.CommentUpdateResponse;
import com.jaeychoi.dailyus.comment.service.CommentUpdateService;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
import java.time.LocalDateTime;
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
  private CommentUpdateService commentUpdateService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentUpdateService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setValidator(validator)
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void updateCommentReturnsOkResponse() throws Exception {
    CommentUpdateRequest request = new CommentUpdateRequest("updated comment");
    CommentUpdateResponse response = new CommentUpdateResponse(
        10L,
        "updated comment",
        true,
        LocalDateTime.of(2026, 5, 18, 12, 0)
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
        .andExpect(jsonPath("$.data.edited").value(true))
        .andExpect(jsonPath("$.data.updatedAt").value("2026-05-18T12:00:00"));
  }

  @Test
  void updateCommentReturnsBadRequestWhenContentIsBlank() throws Exception {
    CommentUpdateRequest request = new CommentUpdateRequest(" ");

    mockMvc.perform(patch("/api/v1/comments/10")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
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
}
