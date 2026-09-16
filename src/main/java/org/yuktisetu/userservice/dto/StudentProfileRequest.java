package org.yuktisetu.userservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.util.Date;
import java.util.List;

// institution/degree/branch/cgpa/graduationYear/tenthPercentage/
// twelfthPercentage/semester GPAs are deliberately NOT here -- those are
// admin-controlled academic facts (sourced from College/Department/
// UserRoleAssignment.degree and StudentProfile's own admin-set columns),
// never student-editable. coCubesScore is the one academic field a student
// does self-report.
public record StudentProfileRequest(
        Date dateOfBirth,
        String address,

        @DecimalMin(value = "0", message = "coCubesScore must be between 0 and 800")
        @DecimalMax(value = "800", message = "coCubesScore must be between 0 and 800")
        Double coCubesScore,

        List<String> skills,
        List<CodingProfileDTO> codingProfiles,
        List<ProfessionalProfileDTO> professionalProfiles,
        List<ProjectDTO> projects,
        List<WorkExperienceDTO> workExperiences,
        List<AchievementDTO> achievements
) {
    public record CodingProfileDTO(String platform, String username, String profileLink, Double rating) {}
    public record ProfessionalProfileDTO(String platform, String profileLink) {}
    public record ProjectDTO(String title, String projectLink, String description, String technologies) {}
    public record WorkExperienceDTO(String company, String role, String duration, String description) {}
    public record AchievementDTO(String title, String description) {}
}
