package com.jaeychoi.dailyus.user.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
import com.jaeychoi.dailyus.post.dto.PostFeedItemResponse;
import com.jaeychoi.dailyus.post.dto.PostFeedResponse;
import com.jaeychoi.dailyus.user.dto.UserActivityResponse;
import com.jaeychoi.dailyus.user.dto.UserFollowResponse;
import com.jaeychoi.dailyus.user.dto.UserGroupItemResponse;
import com.jaeychoi.dailyus.user.dto.UserGroupResponse;
import com.jaeychoi.dailyus.user.dto.UserMyProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileUpdateRequest;
import com.jaeychoi.dailyus.user.service.UserActivityService;
import com.jaeychoi.dailyus.user.service.UserFollowService;
import com.jaeychoi.dailyus.user.service.UserMyGroupService;
import com.jaeychoi.dailyus.user.service.UserPostService;
import com.jaeychoi.dailyus.user.service.UserProfileService;
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
class UserControllerTest {

  @Mock
  private UserFollowService userFollowService;

  @Mock
  private UserProfileService userProfileService;

  @Mock
  private UserActivityService userActivityService;

  @Mock
  private UserMyGroupService userMyGroupService;

  @Mock
  private UserPostService userPostService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(
            new UserController(userFollowService, userProfileService, userActivityService,
                userMyGroupService, userPostService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
  }

  @Test
  void getMyGroupsReturnsOkResponse() throws Exception {
    UserGroupResponse response = new UserGroupResponse(List.of(
        new UserGroupItemResponse(10L, "daily-us", "https://example.com/group.png"),
        new UserGroupItemResponse(11L, "backend-study", null)
    ));
    when(userMyGroupService.getMyGroups(1L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/me/groups")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.items[0].groupId").value(10L))
        .andExpect(jsonPath("$.data.items[0].name").value("daily-us"))
        .andExpect(jsonPath("$.data.items[0].groupImage").value("https://example.com/group.png"))
        .andExpect(jsonPath("$.data.items[1].groupId").value(11L))
        .andExpect(jsonPath("$.data.items[1].name").value("backend-study"));
  }

  @Test
  void getMyGroupsReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/users/me/groups"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getMyPostsReturnsOkResponse() throws Exception {
    PostFeedResponse response = new PostFeedResponse(
        List.of(new PostFeedItemResponse(
            10L,
            1L,
            "user",
            "https://example.com/profile.png",
            "my content",
            List.of("https://cdn.example.com/1.png"),
            3L,
            LocalDateTime.of(2026, 5, 1, 10, 0)
        )),
        LocalDateTime.of(2026, 5, 1, 10, 0),
        10L,
        true,
        10L
    );
    when(userPostService.getMyPosts(1L, null, null, 10L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/me/posts")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-05-01T10:00:00"))
        .andExpect(jsonPath("$.data.lastPostId").value(10L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.items[0].postId").value(10L))
        .andExpect(jsonPath("$.data.items[0].imageUrls[0]").value("https://cdn.example.com/1.png"));
  }

  @Test
  void getMyPostsPassesCursorAndSizeQueryParameters() throws Exception {
    PostFeedResponse response = new PostFeedResponse(
        List.of(),
        LocalDateTime.of(2026, 5, 1, 8, 0),
        7L,
        true,
        5L
    );
    when(userPostService.getMyPosts(
        1L,
        LocalDateTime.of(2026, 5, 1, 9, 0),
        15L,
        5L
    )).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/me/posts")
            .queryParam("createdAt", "2026-05-01T09:00:00")
            .queryParam("postId", "15")
            .queryParam("size", "5")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-05-01T08:00:00"))
        .andExpect(jsonPath("$.data.lastPostId").value(7L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(5L));
  }

  @Test
  void getMyPostsReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/users/me/posts"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getMyProfileReturnsOkResponse() throws Exception {
    UserMyProfileResponse response = new UserMyProfileResponse(
        1L,
        "user@example.com",
        "dailyus",
        "sharing daily workouts",
        "https://cdn.example.com/profile.png",
        3L,
        7L,
        5L
    );
    when(userProfileService.getMyProfile(1L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/me")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.userId").value(1L))
        .andExpect(jsonPath("$.data.email").value("user@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("dailyus"))
        .andExpect(jsonPath("$.data.intro").value("sharing daily workouts"))
        .andExpect(jsonPath("$.data.profileImage").value("https://cdn.example.com/profile.png"))
        .andExpect(jsonPath("$.data.followerCount").value(3L))
        .andExpect(jsonPath("$.data.followeeCount").value(7L))
        .andExpect(jsonPath("$.data.postCount").value(5L));
  }

  @Test
  void updateMyProfileReturnsOkResponse() throws Exception {
    UserProfileResponse response = new UserProfileResponse(
        1L,
        "dailyus-new",
        "updated intro",
        "https://cdn.example.com/profile.png",
        3L,
        7L,
        5L,
        false
    );
    when(userProfileService.updateProfile(
        1L,
        new UserProfileUpdateRequest(
            "dailyus-new",
            "updated intro",
            "https://cdn.example.com/profile.png"
        ))).thenReturn(response);

    mockMvc.perform(patch("/api/v1/users/me")
            .contentType("application/json")
            .content("""
                {
                  "nickname": "dailyus-new",
                  "intro": "updated intro",
                  "profileImage": "https://cdn.example.com/profile.png"
                }
                """)
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.nickname").value("dailyus-new"))
        .andExpect(jsonPath("$.data.intro").value("updated intro"))
        .andExpect(jsonPath("$.data.profileImage").value("https://cdn.example.com/profile.png"));
  }

  @Test
  void updateMyProfileReturnsBadRequestWhenNicknameIsBlank() throws Exception {
    mockMvc.perform(patch("/api/v1/users/me")
            .contentType("application/json")
            .content("""
                {
                  "nickname": "   "
                }
                """)
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.message").value("Invalid input."));
  }

  @Test
  void getMyProfileReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getUserProfileReturnsOkResponse() throws Exception {
    UserProfileResponse response = new UserProfileResponse(
        2L,
        "target",
        "target intro",
        "https://cdn.example.com/target.png",
        8L,
        3L,
        12L,
        true
    );
    when(userProfileService.getProfile(1L, 2L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/2")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.userId").value(2L))
        .andExpect(jsonPath("$.data.nickname").value("target"))
        .andExpect(jsonPath("$.data.intro").value("target intro"))
        .andExpect(jsonPath("$.data.profileImage").value("https://cdn.example.com/target.png"))
        .andExpect(jsonPath("$.data.followerCount").value(8L))
        .andExpect(jsonPath("$.data.followeeCount").value(3L))
        .andExpect(jsonPath("$.data.postCount").value(12L))
        .andExpect(jsonPath("$.data.following").value(true))
        .andExpect(jsonPath("$.data.email").doesNotExist());
  }

  @Test
  void getUserProfileReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/users/2"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getMyActivitiesReturnsOkResponse() throws Exception {
    UserActivityResponse response = new UserActivityResponse(2026, 3, List.of(10, 11, 12));
    when(userActivityService.getMyActivities(1L, 2026, 3)).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/me/activities")
            .queryParam("year", "2026")
            .queryParam("month", "3")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.year").value(2026))
        .andExpect(jsonPath("$.data.month").value(3))
        .andExpect(jsonPath("$.data.activityDays[0]").value(10))
        .andExpect(jsonPath("$.data.activityDays[2]").value(12));
  }

  @Test
  void getMyActivitiesReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    mockMvc.perform(get("/api/v1/users/me/activities"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void getUserPostsReturnsOkResponse() throws Exception {
    PostFeedResponse response = new PostFeedResponse(
        List.of(new PostFeedItemResponse(
            20L,
            2L,
            "target",
            "https://example.com/target.png",
            "target content",
            List.of("https://cdn.example.com/20-1.png"),
            4L,
            LocalDateTime.of(2026, 5, 2, 10, 0)
        )),
        LocalDateTime.of(2026, 5, 2, 10, 0),
        20L,
        false,
        10L
    );
    when(userPostService.getPosts(2L, null, null, 10L)).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/2/posts")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-05-02T10:00:00"))
        .andExpect(jsonPath("$.data.lastPostId").value(20L))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.items[0].userId").value(2L))
        .andExpect(jsonPath("$.data.items[0].postId").value(20L));
  }

  @Test
  void getUserPostsPassesCursorAndSizeQueryParameters() throws Exception {
    PostFeedResponse response = new PostFeedResponse(
        List.of(),
        LocalDateTime.of(2026, 5, 2, 8, 0),
        17L,
        true,
        5L
    );
    when(userPostService.getPosts(
        2L,
        LocalDateTime.of(2026, 5, 2, 9, 0),
        18L,
        5L
    )).thenReturn(response);

    mockMvc.perform(get("/api/v1/users/2/posts")
            .queryParam("createdAt", "2026-05-02T09:00:00")
            .queryParam("postId", "18")
            .queryParam("size", "5")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lastCreatedAt").value("2026-05-02T08:00:00"))
        .andExpect(jsonPath("$.data.lastPostId").value(17L))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.size").value(5L));
  }

  @Test
  void followReturnsCreatedResponse() throws Exception {
    UserFollowResponse response = new UserFollowResponse(2L, true, 3L, 1L);
    when(userFollowService.follow(1L, 2L)).thenReturn(response);

    mockMvc.perform(post("/api/v1/users/2/follow")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.followee").value(2L))
        .andExpect(jsonPath("$.data.following").value(true))
        .andExpect(jsonPath("$.data.followerCount").value(3L))
        .andExpect(jsonPath("$.data.followeeCount").value(1L));
  }

  @Test
  void unfollowReturnsOkResponse() throws Exception {
    UserFollowResponse response = new UserFollowResponse(2L, false, 2L, 1L);
    when(userFollowService.unfollow(1L, 2L)).thenReturn(response);

    mockMvc.perform(delete("/api/v1/users/2/follow")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.followee").value(2L))
        .andExpect(jsonPath("$.data.following").value(false))
        .andExpect(jsonPath("$.data.followerCount").value(2L))
        .andExpect(jsonPath("$.data.followeeCount").value(1L));
  }

  @Test
  void followReturnsConflictWhenRelationshipAlreadyExists() throws Exception {
    when(userFollowService.follow(anyLong(), anyLong()))
        .thenThrow(new BaseException(ErrorCode.FOLLOW_ALREADY_EXISTS));

    mockMvc.perform(post("/api/v1/users/2/follow")
            .requestAttr(AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "user@example.com", "user")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.FOLLOW_ALREADY_EXISTS.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.FOLLOW_ALREADY_EXISTS.getMessage()));
  }
}
