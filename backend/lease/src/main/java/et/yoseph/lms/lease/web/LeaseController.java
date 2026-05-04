package et.yoseph.lms.lease.web;

import et.yoseph.lms.lease.domain.LeaseStatus;
import et.yoseph.lms.lease.integration.PaymentClient;
import et.yoseph.lms.lease.service.LeaseService;
import et.yoseph.lms.lease.web.dto.DocumentSummaryResponse;
import et.yoseph.lms.lease.web.dto.CreateCheckoutSessionRequest;
import et.yoseph.lms.lease.web.dto.CreateCheckoutSessionResponse;
import et.yoseph.lms.lease.web.dto.ConfirmCheckoutSessionRequest;
import et.yoseph.lms.lease.web.dto.LeaseResponse;
import et.yoseph.lms.lease.web.dto.LeaseWriteRequest;
import et.yoseph.lms.lease.web.dto.LeasePaymentItemResponse;
import et.yoseph.lms.lease.web.dto.PaymentSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leases")
@Tag(name = "Leases")
public class LeaseController {

    private final LeaseService leaseService;
    private final PaymentClient paymentClient;

    public LeaseController(LeaseService leaseService, PaymentClient paymentClient) {
        this.leaseService = leaseService;
        this.paymentClient = paymentClient;
    }

    @GetMapping
    @Operation(summary = "List leases (optional filters)")
    public List<LeaseResponse> list(
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) LeaseStatus status,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String propertyOwnerPartyId) {
        return leaseService.list(propertyId, tenantId, status, ownerId, propertyOwnerPartyId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lease by id")
    public LeaseResponse get(@PathVariable long id) {
        return leaseService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create lease (starts in DRAFT); emits LeaseCreatedEvent in-process")
    public LeaseResponse create(@Valid @RequestBody LeaseWriteRequest request) {
        return leaseService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace lease (allowed in DRAFT or PENDING_APPROVAL)")
    public LeaseResponse update(@PathVariable long id, @Valid @RequestBody LeaseWriteRequest request) {
        return leaseService.update(id, request);
    }

    @PostMapping("/{id}/submit-for-approval")
    @Operation(summary = "Move lease from DRAFT to PENDING_APPROVAL")
    public LeaseResponse submitForApproval(@PathVariable long id) {
        return leaseService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve-as-owner")
    @Operation(summary = "Record owner approval (pending leases only). Requires X-Owner-Party-Id to match lease owner_id.")
    public LeaseResponse approveAsOwner(
            @PathVariable long id,
            @RequestHeader("X-Owner-Party-Id") String ownerPartyId) {
        return leaseService.approveAsOwner(id, ownerPartyId);
    }

    @PostMapping("/{id}/approve-as-tenant")
    @Operation(summary = "Record tenant approval (pending leases only). Requires X-Tenant-Id to match lease tenant row id.")
    public LeaseResponse approveAsTenant(
            @PathVariable long id,
            @RequestHeader("X-Tenant-Id") long tenantId) {
        return leaseService.approveAsTenant(id, tenantId);
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate an ACTIVE lease")
    public LeaseResponse terminate(@PathVariable long id) {
        return leaseService.terminate(id);
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "Payments for this lease (placeholder until Payment service is wired)")
    public PaymentSummaryResponse payments(
            @PathVariable long id,
            @RequestHeader("X-Tenant-Id") long tenantId,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String bearerToken) {
        Map<String, Object> root = paymentClient.fetchPayments(id, tenantId, bearerToken != null ? bearerToken : "");
        Object rawItems = root != null ? root.get("items") : null;
        if (!(rawItems instanceof List<?> items)) {
            return new PaymentSummaryResponse(List.of());
        }
        List<LeasePaymentItemResponse> out = items.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(m -> new LeasePaymentItemResponse(
                        m.get("period") != null ? m.get("period").toString() : "",
                        m.get("amount") != null ? new BigDecimal(m.get("amount").toString()) : BigDecimal.ZERO,
                        m.get("currency") != null ? m.get("currency").toString() : "usd",
                        m.get("status") != null ? m.get("status").toString() : "PENDING",
                        m.get("paidAt") != null ? Instant.parse(m.get("paidAt").toString()) : null,
                        m.get("paymentId") instanceof Number n ? n.longValue() : null
                ))
                .toList();
        return new PaymentSummaryResponse(out);
    }

    @PostMapping("/{id}/payments/checkout-session")
    @Operation(summary = "Create Stripe Checkout Session for a given lease-month (tenant only)")
    public CreateCheckoutSessionResponse createCheckoutSession(
            @PathVariable long id,
            @RequestHeader("X-Tenant-Id") long tenantId,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String bearerToken,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        Map<String, Object> root = paymentClient.createCheckoutSession(
                id,
                tenantId,
                bearerToken != null ? bearerToken : "",
                Map.of("period", request.period()));
        if (root == null) {
            throw new et.yoseph.lms.lease.service.BadRequestException("Payment service did not return a response");
        }
        return new CreateCheckoutSessionResponse(
                root.get("url") != null ? root.get("url").toString() : "",
                root.get("stripeCheckoutSessionId") != null ? root.get("stripeCheckoutSessionId").toString() : "",
                root.get("paymentId") instanceof Number n ? n.longValue() : null
        );
    }

    @PostMapping("/{id}/payments/confirm")
    @Operation(summary = "Confirm Stripe Checkout Session and refresh payment state (tenant only)")
    public void confirmCheckoutSession(
            @PathVariable long id,
            @RequestHeader("X-Tenant-Id") long tenantId,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String bearerToken,
            @Valid @RequestBody ConfirmCheckoutSessionRequest request) {
        paymentClient.confirmCheckoutSession(
                id,
                tenantId,
                bearerToken != null ? bearerToken : "",
                Map.of("sessionId", request.sessionId()));
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "Documents for this lease (placeholder until Document service is wired)")
    public DocumentSummaryResponse documents(@PathVariable long id) {
        return leaseService.documents(id);
    }
}
