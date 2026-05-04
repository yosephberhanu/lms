package et.yoseph.lms.property.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "OwnerResponse")
public record OwnerResponse(
        Long id,
        String partyId,
        String displayName,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
}

