package com.jaeychoi.dailyus.group.controller;

import com.jaeychoi.dailyus.auth.annotation.AuthRequired;
import com.jaeychoi.dailyus.auth.annotation.AuthenticatedUser;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import com.jaeychoi.dailyus.group.dto.GroupCreateRequest;
import com.jaeychoi.dailyus.group.dto.GroupCreateResponse;
import com.jaeychoi.dailyus.group.service.GroupCreateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {


  private final GroupCreateService groupCreateService;

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
}
