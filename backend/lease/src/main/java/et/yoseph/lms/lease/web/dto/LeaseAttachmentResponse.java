package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "LeaseAttachment")
public record LeaseAttachmentResponse(
        long id,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        Instant uploadedAt) {
}

