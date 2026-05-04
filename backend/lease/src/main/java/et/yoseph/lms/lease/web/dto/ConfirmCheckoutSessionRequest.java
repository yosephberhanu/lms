package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ConfirmCheckoutSessionRequest")
public record ConfirmCheckoutSessionRequest(
        @NotBlank
        @Schema(description = "Stripe Checkout Session id (cs_...)", example = "cs_test_a1b2c3")
        String sessionId
) {
}

