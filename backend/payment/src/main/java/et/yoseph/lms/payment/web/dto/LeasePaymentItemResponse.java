package et.yoseph.lms.payment.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "LeasePaymentItem")
public record LeasePaymentItemResponse(
        @Schema(example = "2026-04") String period,
        @Schema(example = "1200.00") BigDecimal amount,
        @Schema(example = "usd") String currency,
        @Schema(example = "PAID") String status,
        Instant paidAt,
        Long paymentId
) {
}

