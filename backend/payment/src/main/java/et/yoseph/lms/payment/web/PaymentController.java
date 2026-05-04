package et.yoseph.lms.payment.web;

import et.yoseph.lms.payment.service.PaymentService;
import et.yoseph.lms.payment.web.dto.ConfirmCheckoutSessionRequest;
import et.yoseph.lms.payment.web.dto.CreateCheckoutSessionRequest;
import et.yoseph.lms.payment.web.dto.CreateCheckoutSessionResponse;
import et.yoseph.lms.payment.web.dto.PaymentSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leases")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{leaseId}/payments")
    @Operation(summary = "Payments for a lease (tenant scoped)")
    public PaymentSummaryResponse payments(
            @PathVariable long leaseId,
            @RequestHeader("X-Tenant-Id") long tenantId) {
        return paymentService.payments(leaseId, tenantId);
    }

    @PostMapping("/{leaseId}/payments/checkout-session")
    @Operation(summary = "Create Stripe Checkout Session for a lease-month (tenant scoped)")
    public CreateCheckoutSessionResponse createCheckoutSession(
            @PathVariable long leaseId,
            @RequestHeader("X-Tenant-Id") long tenantId,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        return paymentService.createCheckoutSession(leaseId, tenantId, request.period());
    }

    @PostMapping("/{leaseId}/payments/confirm")
    @Operation(summary = "Confirm Stripe Checkout Session and mark payment paid (fallback when webhook delays)")
    public void confirm(
            @PathVariable long leaseId,
            @RequestHeader("X-Tenant-Id") long tenantId,
            @Valid @RequestBody ConfirmCheckoutSessionRequest request) {
        paymentService.confirmCheckoutSessionPaid(leaseId, tenantId, request.sessionId());
    }
}

