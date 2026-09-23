package org.yuktisetu.userservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yuktisetu.core.exception.NotFoundException;
import org.yuktisetu.identity.db.User;
import org.yuktisetu.identity.repository.UserRepository;
import org.yuktisetu.userservice.dto.UserProfileResponse;

@Service
@AllArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        log.debug("Fetching user profile for userId={}", userId);

        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> {
                    log.warn("User profile fetch failed — no active user for userId={}", userId);
                    return new NotFoundException("User not found for this user: " + userId);
                });

        log.debug("User profile fetch succeeded for userId={}", userId);
        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getDateOfBirth(),
                user.getAddress()
        );
    }
}