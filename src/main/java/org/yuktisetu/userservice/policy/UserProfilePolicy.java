package org.yuktisetu.userservice.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yuktisetu.core.security.UserPrincipal;
import org.yuktisetu.identity.db.UserRoleAssignment;
import org.yuktisetu.identity.model.RoleHierarchy;
import org.yuktisetu.identity.model.RoleType;
import org.yuktisetu.identity.repository.UserRoleAssignmentRepository;

import java.util.List;

/**
 * Profile-visibility rule: a user can always see their own profile. Beyond
 * that, a profile is visible to any role that sits above the target's role
 * (RoleHierarchy.canView -- "everything below", decided 2026-11-07), within the
 * viewer's own scope: a college-scoped viewer must share the target's college,
 * and a dept-scoped viewer (HOD, FDC) its department too. This is the SAME
 * table admin-service's people search uses, so a person someone can find is a
 * person they can open. It used to be canManage (the create table), under which
 * a Super Admin could not open a single student's profile.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfilePolicy {

    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    public boolean canView(UserPrincipal actor, Long targetUserId) {
        if (actor.userId().equals(targetUserId)) {
            return true;
        }

        List<UserRoleAssignment> targetRoles = userRoleAssignmentRepository.findByUserIdAndIsActiveTrue(targetUserId);
        if (targetRoles.isEmpty()) {
            log.warn("Profile view denied — actor userId={}, target userId={} has no active role assignment",
                    actor.userId(), targetUserId);
            return false;
        }

        for (UserRoleAssignment targetAssignment : targetRoles) {
            RoleType targetRole = targetAssignment.getRole();
            for (UserPrincipal.RoleClaim actorClaim : actor.roles()) {
                RoleType actorRole;
                try {
                    actorRole = RoleType.valueOf(actorClaim.role());
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (RoleHierarchy.canView(actorRole, targetRole) && scopeMatches(actorClaim, actorRole, targetAssignment)) {
                    return true;
                }
            }
        }

        log.warn("Profile view denied — actor userId={} has no role that can view target userId={} (target role(s)={})",
                actor.userId(), targetUserId, targetRoles.stream().map(UserRoleAssignment::getRole).toList());
        return false;
    }

    private boolean scopeMatches(UserPrincipal.RoleClaim actorClaim, RoleType actorRole, UserRoleAssignment targetAssignment) {
        if (RoleHierarchy.isTrustWide(actorRole)) {
            return true; // IT / Super Admin: every college
        }
        // canView never lets a college-scoped viewer see a trust-wide role, so
        // the target here always carries a college.
        if (actorClaim.collegeId() == null || !actorClaim.collegeId().equals(targetAssignment.getCollegeId())) {
            return false;
        }
        if (RoleHierarchy.isDeptScoped(actorRole)) {
            return actorClaim.deptId() != null && actorClaim.deptId().equals(targetAssignment.getDeptId());
        }
        return true;
    }
}
