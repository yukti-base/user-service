package org.yuktisetu.userservice.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.yuktisetu.core.security.UserPrincipal;
import org.yuktisetu.profile.db.DocumentAccessLog;
import org.yuktisetu.profile.db.StudentDocument;
import org.yuktisetu.profile.model.DocumentAccessReason;
import org.yuktisetu.profile.repository.DocumentAccessLogRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Writes the issuance audit trail.
 *
 * REQUIRES_NEW, deliberately: the log must survive even when the surrounding
 * request later fails. If an admin's export blows up halfway through, the fact
 * that URLs were already produced for the first eighty students is exactly the
 * thing an audit needs to know, and rolling it back with the request would erase
 * it.
 *
 * A separate bean from the services that call it, so the @Transactional proxy
 * actually applies -- a self-invoked transactional method silently runs with no
 * transaction at all.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAccessRecorder {

    private final DocumentAccessLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(StudentDocument document, UserPrincipal actor,
                       DocumentAccessReason reason, String context) {
        recordAll(List.of(document), actor, reason, context);
    }

    /** One insert per document, one statement per batch -- never one round trip per row. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAll(Collection<StudentDocument> documents, UserPrincipal actor,
                          DocumentAccessReason reason, String context) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        try {
            Instant now = Instant.now();
            String role = actor == null || actor.roles() == null || actor.roles().isEmpty()
                    ? null : actor.roles().get(0).role();

            List<DocumentAccessLog> rows = documents.stream().map(d -> {
                DocumentAccessLog l = new DocumentAccessLog();
                l.setDocumentId(d.getId());
                l.setStudentUserId(d.getStudentUserId());
                l.setStorageKey(d.getStorageKey());
                l.setActorUserId(actor == null ? null : actor.userId());
                l.setActorRole(role);
                l.setReason(reason);
                l.setContext(context);
                l.setAccessedAt(now);
                return l;
            }).toList();

            logRepository.saveAll(rows);
            log.info("Document URLs issued: count={} reason={} actorId={} context={}",
                    rows.size(), reason, actor == null ? null : actor.userId(), context);

        } catch (Exception e) {
            // An audit failure must not deny a student their own resume. It is
            // logged loudly instead -- a gap in the trail is visible in the logs.
            log.error("FAILED to write document access log (reason={} actorId={}): {}",
                    reason, actor == null ? null : actor.userId(), e.getMessage(), e);
        }
    }
}
