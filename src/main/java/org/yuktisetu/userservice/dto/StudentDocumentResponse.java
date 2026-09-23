package org.yuktisetu.userservice.dto;

import org.yuktisetu.profile.model.StudentDocumentType;

import java.time.Instant;

/**
 * A document as the owner or a TnP admin sees it.
 *
 * The URL is included directly: delivery is public by design, so there is no
 * second call to obtain a link. Every response carrying a URL is recorded in
 * document_access_logs before it leaves the service.
 */
public record StudentDocumentResponse(
        Long id,
        Long studentUserId,
        StudentDocumentType docType,
        String fileName,
        String url,
        String contentType,
        Long sizeBytes,
        boolean primary,
        Instant uploadedAt
) {}
