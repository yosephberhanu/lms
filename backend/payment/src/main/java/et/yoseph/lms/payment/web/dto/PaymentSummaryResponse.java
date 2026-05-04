package et.yoseph.lms.payment.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "LeasePaymentsResponse")
public record PaymentSummaryResponse(List<LeasePaymentItemResponse> items) {
}

