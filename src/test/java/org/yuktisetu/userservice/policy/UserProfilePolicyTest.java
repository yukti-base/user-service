package org.yuktisetu.userservice.policy;

import org.junit.jupiter.api.Test;
import org.yuktisetu.core.security.UserPrincipal;
import org.yuktisetu.identity.db.UserRoleAssignment;
import org.yuktisetu.identity.model.RoleType;
import org.yuktisetu.identity.repository.UserRoleAssignmentRepository;
import org.yuktisetu.institute.db.College;
import org.yuktisetu.institute.db.Department;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The 2026-11-07 view rule: "everything below", within the viewer's own scope. */
class UserProfilePolicyTest {

    private static final long STUDENT_ID = 500L;
    private static final long PCCOE = 1L, OTHER_COLLEGE = 2L;
    private static final long COMP = 10L, MECH = 11L;

    private final UserRoleAssignmentRepository repo = mock(UserRoleAssignmentRepository.class);
    private final UserProfilePolicy policy = new UserProfilePolicy(repo);

    private void studentIn(long collegeId, long deptId) {
        College college = new College();
        college.setId(collegeId);
        Department department = new Department();
        department.setId(deptId);
        when(repo.findByUserIdAndIsActiveTrue(STUDENT_ID)).thenReturn(List.of(
                UserRoleAssignment.builder().role(RoleType.STUDENT).college(college).department(department).isActive(true).build()));
    }

    private static UserPrincipal actor(String role, Long collegeId, Long deptId) {
        return new UserPrincipal(1L, "a@x", List.of(new UserPrincipal.RoleClaim(role, collegeId, deptId)));
    }

    @Test
    void superAdminCanOpenAnyStudent() {
        studentIn(PCCOE, COMP);
        assertTrue(policy.canView(actor("TNP_SUPER_ADMIN", null, null), STUDENT_ID));
    }

    @Test
    void collegeAdminAndCoordinatorSeeStudentsOfTheirCollegeOnly() {
        studentIn(PCCOE, COMP);
        assertTrue(policy.canView(actor("TNP_COLLEGE_ADMIN", PCCOE, null), STUDENT_ID));
        assertTrue(policy.canView(actor("TNP_COORDINATOR", PCCOE, null), STUDENT_ID));
        assertFalse(policy.canView(actor("TNP_COORDINATOR", OTHER_COLLEGE, null), STUDENT_ID));
    }

    @Test
    void hodIsRefusedAStudentInAnotherDepartment() {
        studentIn(PCCOE, COMP);
        assertTrue(policy.canView(actor("HOD", PCCOE, COMP), STUDENT_ID));
        assertFalse(policy.canView(actor("HOD", PCCOE, MECH), STUDENT_ID));
    }

    @Test
    void groundVolunteerSeesNobody() {
        studentIn(PCCOE, COMP);
        assertFalse(policy.canView(actor("GROUND_VOLUNTEER", PCCOE, null), STUDENT_ID));
    }
}
