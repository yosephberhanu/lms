package et.yoseph.lms.payment.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "CreateCheckoutSessionRequest")
public record CreateCheckoutSessionRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "period must be YYYY-MM")
        @Schema(example = "2026-04")
        String period
) {
}

