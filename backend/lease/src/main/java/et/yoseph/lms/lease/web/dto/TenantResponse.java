package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "TenantResponse")
public record TenantResponse(
        Long id,
        String externalPartyId,
        String displayName,
        String email,
        String phone,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
