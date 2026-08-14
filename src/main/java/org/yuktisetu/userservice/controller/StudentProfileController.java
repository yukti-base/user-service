package org.yuktisetu.userservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yuktisetu.userservice.dto.StudentProfileRequest;
import org.yuktisetu.userservice.dto.StudentProfileResponse;
import org.yuktisetu.userservice.security.UserPrincipal;
import org.yuktisetu.userservice.service.StudentProfileService;

@RestController
@RequestMapping("/profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping
    public ResponseEntity<StudentProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(studentProfileService.getProfile(actor.userId()));
    }

    @PatchMapping
    public ResponseEntity<StudentProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal actor,
            @Valid @RequestBody StudentProfileRequest request) {
        return ResponseEntity.ok(studentProfileService.updateProfile(actor.userId(), request));
    }
}
