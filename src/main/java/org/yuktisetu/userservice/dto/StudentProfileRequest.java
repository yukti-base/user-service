package org.yuktisetu.userservice.dto;

import java.util.Date;
import java.util.List;

public record StudentProfileRequest(
        Date dateOfBirth,
        String address,

        String institution,
        String degree,
        String branch,
        Double cgpa,
        Integer graduationYear,
        Double tenthPercentage,
        Double twelfthPercentage,
        Double coCubesScore,
        Double compositeScore,

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
