package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateCheckoutSessionResponse")
public record CreateCheckoutSessionResponse(
        String url,
        String stripeCheckoutSessionId,
        Long paymentId
) {
}

