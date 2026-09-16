package org.yuktisetu.userservice.dto;

import java.util.Date;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Date dateOfBirth,
        String address
) {}