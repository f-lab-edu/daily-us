package com.jaeychoi.dailyus.group.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import com.jaeychoi.dailyus.group.dto.GroupCreateRequest;
import com.jaeychoi.dailyus.group.dto.GroupCreateResponse;
import com.jaeychoi.dailyus.group.dto.GroupJoinResponse;
import com.jaeychoi.dailyus.group.dto.GroupListResponse;
import com.jaeychoi.dailyus.group.service.GroupCreateService;
import com.jaeychoi.dailyus.group.service.GroupJoinService;
import com.jaeychoi.dailyus.group.service.GroupListService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

  private final GroupCreateService groupCreateService;
  private final GroupJoinService groupJoinService;
  private final GroupListService groupListService;

  @GetMapping
  @AuthRequired
  public ApiResponse<GroupListResponse> getGroups(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      LocalDateTime createdAt,
      @RequestParam(required = false) Long groupId,
      @RequestParam(required = false, defaultValue = "10") Long size
  ) {
    return ApiResponse.success(groupListService.getGroups(createdAt, groupId, size));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @AuthRequired
  public ApiResponse<GroupCreateResponse> createGroup(
      @AuthenticatedUser CurrentUser user,
      @Valid @RequestBody GroupCreateRequest request
  ) {
    GroupCreateResponse response = groupCreateService.create(user.userId(), request);
    return ApiResponse.success(response);
  }

  @PostMapping("/{groupId}/join")
  @AuthRequired
  public ApiResponse<GroupJoinResponse> joinGroup(@PathVariable Long groupId,
      @AuthenticatedUser CurrentUser user) {
    GroupJoinResponse response = groupJoinService.join(groupId, user.userId());
    return ApiResponse.success(response);
  }

}
