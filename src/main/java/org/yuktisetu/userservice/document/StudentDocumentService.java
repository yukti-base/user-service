package org.yuktisetu.userservice.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yuktisetu.core.exception.BadRequestException;
import org.yuktisetu.core.exception.ForbiddenException;
import org.yuktisetu.core.exception.NotFoundException;
import org.yuktisetu.core.security.UserPrincipal;
import org.yuktisetu.core.storage.FileStorageService;
import org.yuktisetu.core.storage.StoredFile;
import org.yuktisetu.identity.db.User;
import org.yuktisetu.identity.repository.UserRepository;
import org.yuktisetu.profile.db.StudentDocument;
import org.yuktisetu.profile.model.DocumentAccessReason;
import org.yuktisetu.profile.model.StudentDocumentType;
import org.yuktisetu.profile.repository.StudentDocumentRepository;
import org.yuktisetu.userservice.dto.StudentDocumentResponse;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Student document management: upload, list, replace, delete.
 *
 * Every read path that returns a URL records an issuance row first. Delivery is
 * public, so this log is the only account of who was handed what -- it is not a
 * nice-to-have alongside access control, it IS the accountability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDocumentService {

    /** Only these travel with an application or an export. */
    private static final Set<StudentDocumentType> EMPLOYMENT_FACING =
            Set.of(StudentDocumentType.RESUME, StudentDocumentType.COVER_LETTER);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg", "image/png");

    private final StudentDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final DocumentAccessRecorder accessRecorder;

    @Transactional
    public StudentDocumentResponse upload(UserPrincipal actor, StudentDocumentType docType,
                                          MultipartFile file, boolean makePrimary) {
        try {
            if (docType == null) {
                throw new BadRequestException("A document type is required.");
            }
            String contentType = file == null ? null : file.getContentType();
            if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                throw new BadRequestException(
                        "Unsupported file type: " + contentType + ". Upload a PDF, Word document or image.");
            }

            User student = userRepository.findById(actor.userId())
                    .orElseThrow(() -> new NotFoundException("User", actor.userId()));

            // Upload BEFORE touching the database. A failed upload then leaves no
            // row pointing at a file that does not exist; the reverse ordering
            // would leave a broken document in the student's list.
            StoredFile stored = storage.upload(file, "students/" + actor.userId() + "/" + docType.name().toLowerCase());

            // A resume is primary unless told otherwise -- the common case is a
            // student replacing the resume they apply with.
            boolean primary = makePrimary || docType == StudentDocumentType.RESUME;
            if (primary) {
                documentRepository.clearPrimary(actor.userId(), docType);
            }

            StudentDocument doc = new StudentDocument();
            doc.setStudent(student);
            doc.setDocType(docType);
            doc.setFileName(stored.originalFileName());
            doc.setStorageKey(stored.storageKey());
            doc.setUrl(stored.url());
            doc.setContentType(stored.contentType());
            doc.setSizeBytes(stored.sizeBytes());
            doc.setPrimary(primary);
            doc.setUploadedAt(Instant.now());
            documentRepository.save(doc);

            log.info("Document uploaded: id={} studentUserId={} type={} primary={} key={}",
                    doc.getId(), actor.userId(), docType, primary, stored.storageKey());

            accessRecorder.record(doc, actor, DocumentAccessReason.OWNER_VIEW, "upload");
            return toResponse(doc);

        } catch (BadRequestException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Document upload failed for studentUserId={} type={}: {}",
                    actor.userId(), docType, e.getMessage(), e);
            throw new IllegalStateException("Could not upload the document. Please try again.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> myDocuments(UserPrincipal actor) {
        try {
            List<StudentDocument> docs = documentRepository
                    .findByStudent_IdAndIsDeletedFalseOrderByUploadedAtDesc(actor.userId());
            accessRecorder.recordAll(docs, actor, DocumentAccessReason.OWNER_VIEW, "self-list");
            return docs.stream().map(StudentDocumentService::toResponse).toList();
        } catch (Exception e) {
            log.error("Failed to list documents for studentUserId={}: {}", actor.userId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * A TnP role reading one student's documents. This is the path the audit log
     * exists for, so it records before returning anything.
     */
    @Transactional(readOnly = true)
    public List<StudentDocumentResponse> documentsForStudent(UserPrincipal actor, Long studentUserId) {
        try {
            List<StudentDocument> docs = documentRepository
                    .findByStudent_IdAndIsDeletedFalseOrderByUploadedAtDesc(studentUserId);
            accessRecorder.recordAll(docs, actor, DocumentAccessReason.ADMIN_VIEW,
                    "student:" + studentUserId);
            return docs.stream().map(StudentDocumentService::toResponse).toList();
        } catch (Exception e) {
            log.error("Failed to list documents of studentUserId={} for actorId={}: {}",
                    studentUserId, actor.userId(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void delete(UserPrincipal actor, Long documentId) {
        try {
            StudentDocument doc = documentRepository.findByIdAndIsDeletedFalse(documentId)
                    .orElseThrow(() -> new NotFoundException("Document", documentId));

            // Ownership is checked here rather than trusted from the path -- ids
            // are sequential and guessable.
            if (!doc.getStudentUserId().equals(actor.userId())) {
                log.warn("Access denied — userId={} attempted to delete documentId={} owned by userId={}",
                        actor.userId(), documentId, doc.getStudentUserId());
                throw new ForbiddenException("You do not have permission to delete this document.");
            }

            doc.setDeleted(true);
            doc.setDeletedAt(Instant.now());
            doc.setPrimary(false);
            documentRepository.save(doc);

            // Soft-deleted here, removed from storage too: the student asked for
            // it to be gone, and a public URL that outlives the deletion would
            // make that untrue.
            storage.delete(doc.getStorageKey());
            log.info("Document deleted: id={} studentUserId={}", documentId, actor.userId());

        } catch (NotFoundException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete documentId={} for actorId={}: {}",
                    documentId, actor.userId(), e.getMessage(), e);
            throw e;
        }
    }

    static StudentDocumentResponse toResponse(StudentDocument d) {
        return new StudentDocumentResponse(
                d.getId(), d.getStudentUserId(), d.getDocType(), d.getFileName(),
                d.getUrl(), d.getContentType(), d.getSizeBytes(), d.isPrimary(), d.getUploadedAt());
    }
}
