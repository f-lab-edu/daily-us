package com.jaeychoi.dailyus.comment.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.comment.service.CommentDeleteService;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
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
  private CommentDeleteService commentDeleteService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentDeleteService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
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
}
