package org.yuktisetu.userservice.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yuktisetu.core.response.YuktiSetuResponse;
import org.yuktisetu.core.security.UserPrincipal;
import org.yuktisetu.profile.model.StudentDocumentType;
import org.yuktisetu.userservice.document.StudentDocumentService;
import org.yuktisetu.userservice.dto.StudentDocumentResponse;

import java.util.List;

/**
 * Student documents: the student's own uploads, plus the TnP read path.
 *
 * The owner endpoints take the student from the principal and never from a path
 * variable -- same discipline as the rest of the fleet, because an endpoint
 * accepting a userId would let any authenticated user read another student's
 * identity documents.
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class StudentDocumentController {

    private static final String TNP_ROLES =
            "hasAnyAuthority('ROLE_IT_ADMIN','ROLE_TNP_SUPER_ADMIN','ROLE_TNP_COLLEGE_ADMIN'," +
            "'ROLE_TNP_COORDINATOR','ROLE_HOD','ROLE_FACULTY_DEPT_COORDINATOR')";

    private final StudentDocumentService documentService;

    /**
     * Synchronous upload: the file goes to storage and the stored key and URL come
     * straight back, so the caller never has to poll or guess when it is ready.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<YuktiSetuResponse<StudentDocumentResponse>> upload(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestPart("file") @NotNull MultipartFile file,
            @RequestParam("docType") StudentDocumentType docType,
            @RequestParam(value = "primary", required = false, defaultValue = "false") boolean primary) {

        log.info("POST /documents studentUserId={} type={} size={}",
                actor.userId(), docType, file.getSize());

        return ResponseEntity.status(HttpStatus.CREATED).body(YuktiSetuResponse.success(
                documentService.upload(actor, docType, file, primary), "Document uploaded"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<YuktiSetuResponse<List<StudentDocumentResponse>>> myDocuments(
            @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(YuktiSetuResponse.success(
                documentService.myDocuments(actor), "Documents fetched"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<YuktiSetuResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        documentService.delete(actor, id);
        return ResponseEntity.ok(YuktiSetuResponse.success(null, "Document deleted"));
    }

    /**
     * The TnP read path. Every call writes an issuance row before returning a
     * URL -- with public delivery, this log is the whole accountability story.
     */
    @GetMapping("/student/{studentUserId}")
    @PreAuthorize(TNP_ROLES)
    public ResponseEntity<YuktiSetuResponse<List<StudentDocumentResponse>>> forStudent(
            @AuthenticationPrincipal UserPrincipal actor, @PathVariable Long studentUserId) {

        log.info("GET /documents/student/{} requested by actorId={}", studentUserId, actor.userId());
        return ResponseEntity.ok(YuktiSetuResponse.success(
                documentService.documentsForStudent(actor, studentUserId), "Documents fetched"));
    }
}
