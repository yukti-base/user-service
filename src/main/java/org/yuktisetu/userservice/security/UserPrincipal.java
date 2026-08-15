package org.yuktisetu.userservice.security;

import java.util.List;

public record UserPrincipal(
        Long userId,
        String email,
        List<RoleClaim> roles
) {
    public record RoleClaim(String role, Long collegeId, Long deptId) {}
}
