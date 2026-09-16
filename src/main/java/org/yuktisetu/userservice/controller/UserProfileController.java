package org.yuktisetu.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yuktisetu.core.exception.ForbiddenException;
import org.yuktisetu.core.response.YuktiSetuResponse;
import org.yuktisetu.core.security.UserPrincipal;
import org.yuktisetu.userservice.dto.UserProfileResponse;
import org.yuktisetu.userservice.policy.UserProfilePolicy;
import org.yuktisetu.userservice.service.UserProfileService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user-profile")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfilePolicy userProfilePolicy;

    @GetMapping("/{userId}")
    public ResponseEntity<YuktiSetuResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable Long userId) {
        log.info("GET /user-profile/{} requested by userId={}", userId, actor.userId());
        if (!userProfilePolicy.canView(actor, userId)) {
            throw new ForbiddenException("You do not have permission to view this profile.");
        }
        return ResponseEntity.ok(YuktiSetuResponse.success(userProfileService.getUserProfile(userId), "Profile fetched"));
    }
}