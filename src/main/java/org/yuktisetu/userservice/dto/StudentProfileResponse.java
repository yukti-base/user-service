package org.yuktisetu.userservice.dto;

import java.util.Date;
import java.util.List;

public record StudentProfileResponse(
        Long id,
        Long userId,
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
        List<StudentProfileRequest.CodingProfileDTO> codingProfiles,
        List<StudentProfileRequest.ProfessionalProfileDTO> professionalProfiles,
        List<StudentProfileRequest.ProjectDTO> projects,
        List<StudentProfileRequest.WorkExperienceDTO> workExperiences,
        List<StudentProfileRequest.AchievementDTO> achievements
) {}
