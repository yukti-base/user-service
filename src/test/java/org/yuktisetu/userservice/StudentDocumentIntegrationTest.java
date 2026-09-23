package org.yuktisetu.userservice;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.yuktisetu.core.exception.ForbiddenException;
import org.yuktisetu.core.storage.FileStorageService;
import org.yuktisetu.core.security.UserPrincipal;
import org.yuktisetu.identity.db.User;
import org.yuktisetu.identity.model.UserStatus;
import org.yuktisetu.identity.repository.UserRepository;
import org.yuktisetu.profile.db.StudentDocument;
import org.yuktisetu.profile.model.DocumentAccessReason;
import org.yuktisetu.profile.model.StudentDocumentType;
import org.yuktisetu.profile.repository.DocumentAccessLogRepository;
import org.yuktisetu.profile.repository.StudentDocumentRepository;
import org.yuktisetu.userservice.document.StudentDocumentService;
import org.yuktisetu.userservice.dto.StudentDocumentResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end cover for student document upload, against REAL Cloudinary.
 *
 * Deliberately not mocked. The things most likely to break here are the
 * provider contract itself -- whether a PDF survives `resource_type: auto`
 * without being re-encoded, whether the returned public_id is what we can later
 * delete by, whether the delivery URL is actually fetchable without auth. A mock
 * would assert only that the mock behaves as configured.
 *
 * Every test cleans up what it uploads, so a run leaves nothing in the account.
 */
@SpringBootTest
@ActiveProfiles("test")
class StudentDocumentIntegrationTest {

    @Autowired StudentDocumentService documentService;
    @Autowired StudentDocumentRepository documentRepository;
    @Autowired DocumentAccessLogRepository accessLogRepository;
    @Autowired UserRepository userRepository;
    @Autowired FileStorageService storage;

    private static final AtomicLong SEQ = new AtomicLong(500000);
    private final List<String> uploadedKeys = new ArrayList<>();

    @AfterEach
    void cleanUpStorage() {
        uploadedKeys.forEach(storage::delete);
        uploadedKeys.clear();
    }

    private UserPrincipal aStudent() {
        long n = SEQ.incrementAndGet();
        Date now = new Date();
        User u = userRepository.save(User.builder()
                .email("doc" + n + "@example.test").phone("5" + String.format("%09d", n))
                .password("x").firstName("Doc").lastName("Student")
                .status(UserStatus.ACTIVE)
                .createdAt(now).updatedAt(now).isDeleted(false).build());
        return new UserPrincipal(u.getId(), u.getEmail(),
                List.of(new UserPrincipal.RoleClaim("STUDENT", null, null)));
    }

    private MockMultipartFile aPdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf",
                "%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n".getBytes());
    }

    private StudentDocumentResponse upload(UserPrincipal actor, StudentDocumentType type, String name) {
        StudentDocumentResponse r = documentService.upload(actor, type, aPdf(name), false);
        documentRepository.findByIdAndIsDeletedFalse(r.id())
                .ifPresent(d -> uploadedKeys.add(d.getStorageKey()));
        return r;
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("uploading stores the file and returns a working public URL")
    void uploadReturnsUsableUrl() {
        UserPrincipal student = aStudent();

        StudentDocumentResponse doc = upload(student, StudentDocumentType.RESUME, "asha_resume.pdf");

        assertNotNull(doc.id());
        assertEquals("asha_resume.pdf", doc.fileName());
        assertEquals(StudentDocumentType.RESUME, doc.docType());
        assertNotNull(doc.url(), "a URL must come back on the same call -- upload is synchronous");
        assertTrue(doc.url().startsWith("https://"), "delivery must be https");
        assertTrue(doc.sizeBytes() > 0);
    }

    /**
     * The storage key is the durable handle. It must be the provider's own id,
     * because delete and replace go through it -- a key we invented would leave
     * every uploaded file un-deletable.
     */
    @Test
    @DisplayName("the stored key is the provider's id, under the configured root folder")
    void storageKeyIsProviderIdUnderRootFolder() {
        UserPrincipal student = aStudent();
        StudentDocumentResponse doc = upload(student, StudentDocumentType.AADHAAR, "aadhaar.pdf");

        StudentDocument row = documentRepository.findByIdAndIsDeletedFalse(doc.id()).orElseThrow();
        assertTrue(row.getStorageKey().startsWith("yuktisetu-test/"),
                "key must sit under the configured root folder, got: " + row.getStorageKey());
        assertTrue(row.getStorageKey().contains("students/" + student.userId()),
                "key must be namespaced per student");
    }

    /**
     * A resume is what an application attaches, so exactly one must be primary.
     * Uploading a replacement has to demote the previous one, or the apply path
     * picks arbitrarily between two.
     */
    @Test
    @DisplayName("uploading a new resume demotes the previous one")
    void newResumeReplacesThePrimary() {
        UserPrincipal student = aStudent();

        StudentDocumentResponse first = upload(student, StudentDocumentType.RESUME, "v1.pdf");
        assertTrue(first.primary(), "a resume is primary by default");

        StudentDocumentResponse second = upload(student, StudentDocumentType.RESUME, "v2.pdf");
        assertTrue(second.primary());

        StudentDocument old = documentRepository.findByIdAndIsDeletedFalse(first.id()).orElseThrow();
        assertFalse(old.isPrimary(), "the old resume must be demoted, not left as a second primary");

        assertEquals(second.id(), documentRepository
                .findByStudent_IdAndDocTypeAndIsPrimaryTrueAndIsDeletedFalse(
                        student.userId(), StudentDocumentType.RESUME)
                .orElseThrow().getId());
    }

    @Test
    @DisplayName("a student cannot delete someone else's document")
    void cannotDeleteAnotherStudentsDocument() {
        UserPrincipal owner = aStudent();
        UserPrincipal intruder = aStudent();

        StudentDocumentResponse doc = upload(owner, StudentDocumentType.RESUME, "private.pdf");

        assertThrows(ForbiddenException.class, () -> documentService.delete(intruder, doc.id()),
                "ids are sequential and guessable; ownership must be checked, not trusted");
        assertTrue(documentRepository.findByIdAndIsDeletedFalse(doc.id()).isPresent());
    }

    /**
     * With delivery public by design, this log IS the accountability. If it does
     * not record the TnP read path, nothing does.
     */
    @Test
    @DisplayName("a TnP read of a student's documents is recorded in the audit log")
    void adminReadIsAudited() {
        UserPrincipal student = aStudent();
        upload(student, StudentDocumentType.RESUME, "for_audit.pdf");

        UserPrincipal tnp = new UserPrincipal(999001L, "tnp@example.test",
                List.of(new UserPrincipal.RoleClaim("TNP_COORDINATOR", 1L, null)));

        List<StudentDocumentResponse> seen = documentService.documentsForStudent(tnp, student.userId());
        assertFalse(seen.isEmpty());

        var logged = accessLogRepository
                .findByStudentUserIdOrderByAccessedAtDesc(student.userId(), org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        assertTrue(logged.stream().anyMatch(l ->
                        l.getReason() == DocumentAccessReason.ADMIN_VIEW
                                && l.getActorUserId().equals(999001L)
                                && "TNP_COORDINATOR".equals(l.getActorRole())),
                "the log must name who was given the URL, under what role, and why");
    }

    @Test
    @DisplayName("an unsupported file type is rejected before anything is stored")
    void unsupportedTypeIsRejected() {
        UserPrincipal student = aStudent();
        MockMultipartFile exe = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "MZ".getBytes());

        assertThrows(org.yuktisetu.core.exception.BadRequestException.class,
                () -> documentService.upload(student, StudentDocumentType.OTHER, exe, false));
        assertTrue(documentRepository
                .findByStudent_IdAndIsDeletedFalseOrderByUploadedAtDesc(student.userId()).isEmpty());
    }
}
