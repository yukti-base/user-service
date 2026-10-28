package org.yuktisetu.userservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.time.LocalDate;
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
        List<AchievementDTO> achievements,
        List<CertificationDTO> certifications
) {
    /**
     * platform stays a String on the wire even though the column is now an enum.
     * CodingPlatform.fromLabel() parses it tolerantly, so a portal still sending
     * "Leetcode" keeps working and an unknown name lands on OTHER rather than
     * 400-ing a student out of saving their profile.
     */
    public record CodingProfileDTO(String platform, String username, String profileLink, Double rating) {}
    public record ProfessionalProfileDTO(String platform, String profileLink) {}
    public record ProjectDTO(String title, String projectLink, String description, String technologies) {}
    /**
     * startDate/endDate/isCurrent are what the Experience Relevance scoring factor
     * reads; duration is the legacy free text, still accepted and still displayed
     * when no structured dates were given.
     */
    public record WorkExperienceDTO(String company, String role,
                                    LocalDate startDate, LocalDate endDate, Boolean isCurrent,
                                    String duration, String description) {}
    public record AchievementDTO(String title, String description) {}

    /**
     * A student only ever supplies the claim. Every verification field on the
     * entity is set by the verification adapters or by a TnP reviewer, never by
     * this request -- a student who could post verificationStatus=VERIFIED_ISSUER
     * would be setting their own score.
     */
    public record CertificationDTO(String name, String issuer,
                                   LocalDate issuedOn, LocalDate expiresOn,
                                   String credentialId, String credentialUrl) {}
}
