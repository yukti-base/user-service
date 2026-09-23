package org.yuktisetu.userservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.yuktisetu.core.storage.FileStorageService;
import org.yuktisetu.core.storage.StoredFile;

import java.net.HttpURLConnection;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The assertion that actually matters, and the one I should have written first:
 * a delivery URL must be fetchable by someone with no credentials.
 *
 * Upload succeeding proves nothing on its own. Cloudinary blocks anonymous
 * delivery of PDF and ZIP files by default, so a resume can upload cleanly and
 * still 401 for the recruiter opening it from an exported sheet -- the exact
 * failure the public-delivery policy exists to prevent, and invisible from the
 * upload side. The service sidesteps that block by keeping the extension out of
 * the stored public_id; this test is what proves the sidestep still works.
 *
 * It also asserts the Content-Disposition filename, because "downloads" and
 * "downloads as something openable" are different outcomes and only one of them
 * is useful to an HR contact working through an applicant sheet.
 */
@SpringBootTest
@ActiveProfiles("test")
class PublicDeliveryProbeTest {

    @Autowired FileStorageService storage;

    /** A credential-free GET, exactly as a recruiter's browser would issue it. */
    private record Delivery(int status, String disposition) {}

    private Delivery fetchAnonymously(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
        c.setInstanceFollowRedirects(true);
        c.setRequestMethod("GET");
        c.setConnectTimeout(15000);
        c.setReadTimeout(15000);
        int code = c.getResponseCode();   // no auth header, no cookie, no signature
        String disposition = c.getHeaderField("Content-Disposition");
        c.disconnect();
        return new Delivery(code, disposition);
    }

    @Test
    @DisplayName("a PDF uploaded through the service is fetchable with no credentials")
    void uploadedPdfIsPubliclyFetchable() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "probe_resume.pdf", "application/pdf",
                "%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n".getBytes());

        StoredFile stored = storage.upload(pdf, "probe");
        System.out.println("PROBE_URL=" + stored.url());
        System.out.println("PROBE_KEY=" + stored.storageKey());
        try {
            Delivery got = fetchAnonymously(stored.url());
            System.out.println("PROBE_STATUS=" + got.status());
            System.out.println("PROBE_DISPOSITION=" + got.disposition());

            assertEquals(200, got.status(),
                    "Anonymous fetch of " + stored.url() + " must return 200. A 401 means the stored "
                            + "public_id has picked up a .pdf extension, which makes Cloudinary apply its "
                            + "'PDF and ZIP files delivery' restriction -- every resume link handed to a "
                            + "recruiter would be dead while uploads kept reporting success.");

            assertNotNull(got.disposition(), "no Content-Disposition means the fl_attachment flag was dropped");
            assertEquals("attachment; filename=\"probe_resume.pdf\"", got.disposition(),
                    "The recruiter must receive the student's filename. An unnamed or extensionless "
                            + "download still returns 200, so this is the only assertion that catches it.");
        } finally {
            storage.delete(stored.storageKey());
        }
    }
}
