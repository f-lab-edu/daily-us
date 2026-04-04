package com.jaeychoi.dailyus.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
import com.jaeychoi.dailyus.post.dto.PostCreateRequest;
import com.jaeychoi.dailyus.post.dto.PostCreateResponse;
import com.jaeychoi.dailyus.post.service.PostCreateService;
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

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(new PostController(postCreateService))
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
        "미라클 모닝 #Morning"
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
    PostCreateRequest request = new PostCreateRequest(List.of(), "매일 매일");

    mockMvc.perform(post("/api/v1/posts")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }
}
