package org.yuktisetu.userservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yuktisetu.core.exception.ForbiddenException;
import org.yuktisetu.core.exception.NotFoundException;
import org.yuktisetu.db.Achievement;
import org.yuktisetu.db.CodingProfile;
import org.yuktisetu.db.College;
import org.yuktisetu.db.Department;
import org.yuktisetu.db.ProfessionalProfile;
import org.yuktisetu.db.Project;
import org.yuktisetu.db.StudentProfile;
import org.yuktisetu.db.User;
import org.yuktisetu.db.UserRoleAssignment;
import org.yuktisetu.db.WorkExperience;
import org.yuktisetu.model.RoleType;
import org.yuktisetu.repository.StudentProfileRepository;
import org.yuktisetu.repository.UserRepository;
import org.yuktisetu.repository.UserRoleAssignmentRepository;
import org.yuktisetu.userservice.dto.StudentProfileRequest;
import org.yuktisetu.userservice.dto.StudentProfileResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long userId) {
        log.debug("Fetching student profile for userId={}", userId);

        UserRoleAssignment studentAssignment = findActiveStudentAssignment(userId);

        StudentProfile profile = studentProfileRepository.findByUserIdAndUser_IsDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.warn("Profile fetch failed — no active profile for userId={}", userId);
                    return new NotFoundException("Student profile not found for this user: " + userId);
                });

        log.debug("Profile fetch succeeded for userId={}", userId);
        return toResponse(profile, studentAssignment);
    }

    @Transactional
    public StudentProfileResponse updateProfile(Long userId, StudentProfileRequest req) {
        long start = System.currentTimeMillis();
        log.info("Profile update requested for userId={}", userId);

        UserRoleAssignment studentAssignment = findActiveStudentAssignment(userId);

        boolean isNewProfile = false;
        StudentProfile profile = studentProfileRepository.findByUserIdAndUser_IsDeletedFalse(userId)
                .orElse(null);

        if (profile == null) {
            log.info("No existing profile for userId={} — creating new profile", userId);
            profile = buildBlankProfile(userId);
            isNewProfile = true;
        }

        // Only personal details, coCubesScore, and the portfolio collections
        // are student-editable. institution/degree/branch/cgpa/graduationYear/
        // tenthPercentage/twelfthPercentage/semester GPAs are admin-controlled
        // (sourced from College/Department/UserRoleAssignment.degree, or set
        // via admin-service's bulk-student-import) and are deliberately not
        // part of StudentProfileRequest at all, so there is nothing to guard
        // against here -- they simply cannot arrive in this request.
        if (req.dateOfBirth() != null) profile.setDateOfBirth(req.dateOfBirth());
        if (req.address() != null) profile.setAddress(req.address());
        if (req.coCubesScore() != null) profile.setCoCubesScore(req.coCubesScore());

        if (req.skills() != null) {
            log.debug("Replacing skills for userId={} — count={}", userId, req.skills().size());
            profile.getSkills().clear();
            profile.getSkills().addAll(req.skills());
        }
        if (req.codingProfiles() != null) {
            log.debug("Replacing codingProfiles for userId={} — count={}", userId, req.codingProfiles().size());
            profile.getCodingProfiles().clear();
            for (StudentProfileRequest.CodingProfileDTO d : req.codingProfiles()) {
                profile.getCodingProfiles().add(CodingProfile.builder()
                        .studentProfile(profile)
                        .platform(d.platform())
                        .username(d.username())
                        .profileLink(d.profileLink())
                        .rating(d.rating())
                        .build());
            }
        }
        if (req.professionalProfiles() != null) {
            log.debug("Replacing professionalProfiles for userId={} — count={}", userId, req.professionalProfiles().size());
            profile.getProfessionalProfiles().clear();
            for (StudentProfileRequest.ProfessionalProfileDTO d : req.professionalProfiles()) {
                profile.getProfessionalProfiles().add(ProfessionalProfile.builder()
                        .studentProfile(profile)
                        .platform(d.platform())
                        .profileLink(d.profileLink())
                        .build());
            }
        }
        if (req.projects() != null) {
            log.debug("Replacing projects for userId={} — count={}", userId, req.projects().size());
            profile.getProjects().clear();
            for (StudentProfileRequest.ProjectDTO d : req.projects()) {
                profile.getProjects().add(Project.builder()
                        .studentProfile(profile)
                        .title(d.title())
                        .projectLink(d.projectLink())
                        .description(d.description())
                        .technologies(d.technologies())
                        .build());
            }
        }
        if (req.workExperiences() != null) {
            log.debug("Replacing workExperiences for userId={} — count={}", userId, req.workExperiences().size());
            profile.getWorkExperiences().clear();
            for (StudentProfileRequest.WorkExperienceDTO d : req.workExperiences()) {
                profile.getWorkExperiences().add(WorkExperience.builder()
                        .studentProfile(profile)
                        .company(d.company())
                        .role(d.role())
                        .duration(d.duration())
                        .description(d.description())
                        .build());
            }
        }
        if (req.achievements() != null) {
            log.debug("Replacing achievements for userId={} — count={}", userId, req.achievements().size());
            profile.getAchievements().clear();
            for (StudentProfileRequest.AchievementDTO d : req.achievements()) {
                profile.getAchievements().add(Achievement.builder()
                        .studentProfile(profile)
                        .title(d.title())
                        .description(d.description())
                        .build());
            }
        }

        profile.setUpdatedAt(new Date());
        StudentProfile saved = studentProfileRepository.save(profile);

        long elapsedMs = System.currentTimeMillis() - start;
        log.info("Profile update completed for userId={} — created={}, elapsedMs={}", userId, isNewProfile, elapsedMs);

        return toResponse(saved, studentAssignment);
    }

    /** Not persisted here — the single save() in updateProfile() handles the insert. */
    private StudentProfile buildBlankProfile(Long userId) {
        User user = userRepository.getReferenceById(userId);
        Date now = new Date();
        return StudentProfile.builder()
                .user(user)
                .skills(new ArrayList<>())
                .codingProfiles(new ArrayList<>())
                .professionalProfiles(new ArrayList<>())
                .projects(new ArrayList<>())
                .workExperiences(new ArrayList<>())
                .achievements(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Returns the caller's active STUDENT role assignment -- the source of
     * truth for institution/branch (College/Department) and degree at
     * response time, in addition to being the access-control gate this used
     * to be (assertStudentRole).
     */
    private UserRoleAssignment findActiveStudentAssignment(Long userId) {
        List<UserRoleAssignment> activeRoles = userRoleAssignmentRepository.findByUserIdAndIsActiveTrue(userId);
        if (activeRoles.isEmpty()) {
            log.warn("Access denied — userId={} has no active role assignment", userId);
            throw new ForbiddenException("Access denied: user " + userId + " has no active role assignment");
        }
        if (activeRoles.size() > 1) {
            log.warn("User {} has {} active role assignments: {}",
                    userId, activeRoles.size(), activeRoles.stream().map(UserRoleAssignment::getRole).toList());
        }

        UserRoleAssignment studentAssignment = activeRoles.stream()
                .filter(r -> r.getRole() == RoleType.STUDENT)
                .findFirst()
                .orElse(null);

        if (studentAssignment == null) {
            List<RoleType> roles = activeRoles.stream().map(UserRoleAssignment::getRole).toList();
            log.warn("Access denied — userId={} has role(s)={}, expected STUDENT", userId, roles);
            throw new ForbiddenException(String.format("Access denied, %d cannot perform this action with the role(s): %s", userId, roles));
        }
        return studentAssignment;
    }

    private StudentProfileResponse toResponse(StudentProfile p, UserRoleAssignment studentAssignment) {
        List<StudentProfileRequest.CodingProfileDTO> coding = p.getCodingProfiles().stream()
                .map(c -> new StudentProfileRequest.CodingProfileDTO(c.getPlatform(), c.getUsername(), c.getProfileLink(), c.getRating()))
                .toList();
        List<StudentProfileRequest.ProfessionalProfileDTO> prof = p.getProfessionalProfiles().stream()
                .map(c -> new StudentProfileRequest.ProfessionalProfileDTO(c.getPlatform(), c.getProfileLink()))
                .toList();
        List<StudentProfileRequest.ProjectDTO> projects = p.getProjects().stream()
                .map(c -> new StudentProfileRequest.ProjectDTO(c.getTitle(), c.getProjectLink(), c.getDescription(), c.getTechnologies()))
                .toList();
        List<StudentProfileRequest.WorkExperienceDTO> work = p.getWorkExperiences().stream()
                .map(c -> new StudentProfileRequest.WorkExperienceDTO(c.getCompany(), c.getRole(), c.getDuration(), c.getDescription()))
                .toList();
        List<StudentProfileRequest.AchievementDTO> achievements = p.getAchievements().stream()
                .map(c -> new StudentProfileRequest.AchievementDTO(c.getTitle(), c.getDescription()))
                .toList();

        College college = studentAssignment.getCollege();
        Department department = studentAssignment.getDepartment();

        return new StudentProfileResponse(
                p.getId(),
                p.getUser().getId(),
                p.getDateOfBirth(),
                p.getAddress(),
                college != null ? college.getName() : null,
                studentAssignment.getDegree(),
                department != null ? department.getName() : null,
                p.getCgpa(),
                p.getGraduationYear(),
                p.getTenthPercentage(),
                p.getTwelfthPercentage(),
                p.getSem1Gpa(),
                p.getSem2Gpa(),
                p.getSem3Gpa(),
                p.getSem4Gpa(),
                p.getSem5Gpa(),
                p.getSem6Gpa(),
                p.getSem7Gpa(),
                p.getSem8Gpa(),
                p.getCoCubesScore(),
                p.getCompositeScore(),
                new ArrayList<>(p.getSkills()),
                coding,
                prof,
                projects,
                work,
                achievements
        );
    }
}
