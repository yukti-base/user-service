package org.yuktisetu.userservice.dto;

import java.util.Date;
import java.util.List;

// institution/degree/branch are read-only here: institution/branch are the
// student's College.name/Department.name (via their active UserRoleAssignment),
// degree is UserRoleAssignment.degree -- none of the three are columns on
// StudentProfile anymore. cgpa/graduationYear/tenthPercentage/
// twelfthPercentage/sem1-8Gpa are StudentProfile columns, but admin-set only
// (not part of StudentProfileRequest) -- the frontend should render all of
// these as read-only and only send coCubesScore back on PATCH.
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
        Double sem1Gpa,
        Double sem2Gpa,
        Double sem3Gpa,
        Double sem4Gpa,
        Double sem5Gpa,
        Double sem6Gpa,
        Double sem7Gpa,
        Double sem8Gpa,
        Double coCubesScore,
        Double compositeScore,

        List<String> skills,
        List<StudentProfileRequest.CodingProfileDTO> codingProfiles,
        List<StudentProfileRequest.ProfessionalProfileDTO> professionalProfiles,
        List<StudentProfileRequest.ProjectDTO> projects,
        List<StudentProfileRequest.WorkExperienceDTO> workExperiences,
        List<StudentProfileRequest.AchievementDTO> achievements
) {}
