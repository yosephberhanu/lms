package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "TenantUpsertRequest")
public record TenantUpsertRequest(
        @Size(max = 128) String externalPartyId,
        @NotBlank @Size(max = 255) String displayName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 64) String status
) {
}
