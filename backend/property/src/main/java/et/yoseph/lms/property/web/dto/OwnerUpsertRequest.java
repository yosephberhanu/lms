package et.yoseph.lms.property.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "OwnerUpsertRequest")
public record OwnerUpsertRequest(
        @NotBlank @Size(max = 128) String partyId,
        @NotBlank @Size(max = 255) String displayName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone
) {
}

