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
 * that, a profile is visible to any role that could have created the
 * target's role -- i.e. visibility mirrors RoleHierarchy.canManage, scoped
 * the same way role creation itself is scoped (college/dept match, unless
 * the actor holds the managing role trust-wide). This is deliberately the
 * SAME rule table auth-service uses for role creation (see RoleHierarchy in
 * user-dal), not a locally-duplicated copy.
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
                if (RoleHierarchy.canManage(actorRole, targetRole) && scopeMatches(actorClaim, targetAssignment, targetRole)) {
                    return true;
                }
            }
        }

        log.warn("Profile view denied — actor userId={} has no role that manages target userId={} (target role(s)={})",
                actor.userId(), targetUserId, targetRoles.stream().map(UserRoleAssignment::getRole).toList());
        return false;
    }

    private boolean scopeMatches(UserPrincipal.RoleClaim actorClaim, UserRoleAssignment targetAssignment, RoleType targetRole) {
        if (!RoleHierarchy.isCollegeScoped(targetRole)) {
            return true; // target role is trust-wide, no scope check needed
        }
        if (actorClaim.collegeId() == null) {
            return true; // actor holds the managing role trust-wide
        }
        if (!actorClaim.collegeId().equals(targetAssignment.getCollegeId())) {
            return false;
        }
        if (RoleHierarchy.isDeptScoped(targetRole) && actorClaim.deptId() != null) {
            return actorClaim.deptId().equals(targetAssignment.getDeptId());
        }
        return true;
    }
}
