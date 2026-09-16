package org.yuktisetu.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yuktisetu.core.response.YuktiSetuResponse;
import org.yuktisetu.userservice.dto.StudentProfileRequest;
import org.yuktisetu.userservice.dto.StudentProfileResponse;
import org.yuktisetu.userservice.service.StudentProfileService;
import org.yuktisetu.core.security.UserPrincipal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/student-profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping
    public ResponseEntity<YuktiSetuResponse<StudentProfileResponse>> getMyProfile(@AuthenticationPrincipal UserPrincipal actor) {
        log.info("GET /student-profile requested by userId={}", actor.userId());
        return ResponseEntity.ok(YuktiSetuResponse.success(studentProfileService.getProfile(actor.userId()), "Profile fetched"));
    }

    @PatchMapping
    public ResponseEntity<YuktiSetuResponse<StudentProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal actor,
            @Valid @RequestBody StudentProfileRequest request) {
        log.info("PATCH /student-profile requested by userId={}", actor.userId());
        return ResponseEntity.ok(YuktiSetuResponse.success(studentProfileService.updateProfile(actor.userId(), request), "Profile updated"));
    }
}
