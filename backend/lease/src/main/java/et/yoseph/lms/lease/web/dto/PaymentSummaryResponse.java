package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Payment schedule + recorded payments for a lease.
 */
@Schema(name = "LeasePaymentsResponse")
public record PaymentSummaryResponse(List<LeasePaymentItemResponse> items) {
}
