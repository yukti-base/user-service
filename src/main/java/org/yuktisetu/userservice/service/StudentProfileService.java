package org.yuktisetu.userservice.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yuktisetu.db.Achievement;
import org.yuktisetu.db.CodingProfile;
import org.yuktisetu.db.ProfessionalProfile;
import org.yuktisetu.db.Project;
import org.yuktisetu.db.StudentProfile;
import org.yuktisetu.db.User;
import org.yuktisetu.db.WorkExperience;
import org.yuktisetu.repository.StudentProfileRepository;
import org.yuktisetu.repository.UserRepository;
import org.yuktisetu.userservice.dto.StudentProfileRequest;
import org.yuktisetu.userservice.dto.StudentProfileResponse;
import org.yuktisetu.userservice.exception.UserServiceExceptions.ProfileNotFoundException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@AllArgsConstructor
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public StudentProfileResponse getProfile(Long userId) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);
        return toResponse(profile);
    }

    @Transactional
    public StudentProfileResponse updateProfile(Long userId, StudentProfileRequest req) {
        StudentProfile profile = studentProfileRepository.findByUserId(userId)
                .orElseGet(() -> createBlankProfile(userId));

        // Scalar fields — PATCH semantics: only overwrite if the caller actually sent it.
        if (req.dateOfBirth() != null) profile.setDateOfBirth(req.dateOfBirth());
        if (req.address() != null) profile.setAddress(req.address());
        if (req.institution() != null) profile.setInstitution(req.institution());
        if (req.degree() != null) profile.setDegree(req.degree());
        if (req.branch() != null) profile.setBranch(req.branch());
        if (req.cgpa() != null) profile.setCgpa(req.cgpa());
        if (req.graduationYear() != null) profile.setGraduationYear(req.graduationYear());
        if (req.tenthPercentage() != null) profile.setTenthPercentage(req.tenthPercentage());
        if (req.twelfthPercentage() != null) profile.setTwelfthPercentage(req.twelfthPercentage());
        if (req.coCubesScore() != null) profile.setCoCubesScore(req.coCubesScore());
        if (req.compositeScore() != null) profile.setCompositeScore(req.compositeScore());

        // Collections — whole-list-replace semantics: if the key was sent (even as []),
        // the entire list is replaced. orphanRemoval on the entity handles cleanup.
        if (req.skills() != null) {
            profile.getSkills().clear();
            profile.getSkills().addAll(req.skills());
        }
        if (req.codingProfiles() != null) {
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
        return toResponse(saved);
    }

    private StudentProfile createBlankProfile(Long userId) {
        User user = userRepository.getReferenceById(userId);
        Date now = new Date();
        StudentProfile blank = StudentProfile.builder()
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
        return studentProfileRepository.save(blank);
    }

    private StudentProfileResponse toResponse(StudentProfile p) {
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

        return new StudentProfileResponse(
                p.getId(),
                p.getUser().getId(),
                p.getDateOfBirth(),
                p.getAddress(),
                p.getInstitution(),
                p.getDegree(),
                p.getBranch(),
                p.getCgpa(),
                p.getGraduationYear(),
                p.getTenthPercentage(),
                p.getTwelfthPercentage(),
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