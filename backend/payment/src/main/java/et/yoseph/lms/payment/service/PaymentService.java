package et.yoseph.lms.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import et.yoseph.lms.payment.domain.LeasePayment;
import et.yoseph.lms.payment.domain.LeasePaymentStatus;
import et.yoseph.lms.payment.integration.LeaseClient;
import et.yoseph.lms.payment.integration.LeaseSnapshot;
import et.yoseph.lms.payment.repository.LeasePaymentRepository;
import et.yoseph.lms.payment.web.dto.CreateCheckoutSessionResponse;
import et.yoseph.lms.payment.web.dto.LeasePaymentItemResponse;
import et.yoseph.lms.payment.web.dto.PaymentSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PaymentService {

    private final LeaseClient leaseClient;
    private final LeasePaymentRepository paymentRepository;
    private final StripePaymentProperties props;

    public PaymentService(LeaseClient leaseClient, LeasePaymentRepository paymentRepository, StripePaymentProperties props) {
        this.leaseClient = leaseClient;
        this.paymentRepository = paymentRepository;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public PaymentSummaryResponse payments(long leaseId, long actingTenantId) {
        LeaseSnapshot lease = leaseClient.fetchLease(leaseId);
        if (lease.tenantId() != actingTenantId) {
            throw new ForbiddenException("X-Tenant-Id does not match this lease's tenant");
        }

        List<LeasePayment> recorded = paymentRepository.findByLeaseIdOrderByPeriodAsc(leaseId);
        Map<String, LeasePayment> byPeriod = new LinkedHashMap<>();
        for (LeasePayment p : recorded) {
            if (StringUtils.hasText(p.getPeriod())) {
                byPeriod.put(p.getPeriod(), p);
            }
        }

        String currency = normalizeCurrency(props.currency());
        BigDecimal rent = lease.monthlyRent();
        List<String> periods = periodsForLease(lease.startDate(), lease.endDate());
        List<LeasePaymentItemResponse> items = periods.stream().map(period -> {
            LeasePayment p = byPeriod.get(period);
            if (p == null) {
                return new LeasePaymentItemResponse(period, rent, currency, LeasePaymentStatus.PENDING.name(), null, null);
            }
            return new LeasePaymentItemResponse(
                    p.getPeriod(),
                    p.getAmount(),
                    p.getCurrency(),
                    p.getStatus().name(),
                    p.getPaidAt(),
                    p.getId()
            );
        }).toList();

        return new PaymentSummaryResponse(items);
    }

    @Transactional
    public CreateCheckoutSessionResponse createCheckoutSession(long leaseId, long actingTenantId, String period) {
        LeaseSnapshot lease = leaseClient.fetchLease(leaseId);
        if (lease.tenantId() != actingTenantId) {
            throw new ForbiddenException("X-Tenant-Id does not match this lease's tenant");
        }

        YearMonth ym = parsePeriod(period);
        ensurePeriodInLease(ym, lease.startDate(), lease.endDate());

        if (!StringUtils.hasText(props.secretKey())) {
            throw new BadRequestException("Stripe is not configured (PAYMENT_STRIPE_SECRET_KEY is empty)");
        }
        Stripe.apiKey = props.secretKey();

        String currency = normalizeCurrency(props.currency());
        BigDecimal amount = lease.monthlyRent();
        long amountCents = toCents(amount);

        LeasePayment payment = new LeasePayment();
        payment.setLeaseId(leaseId);
        payment.setTenantId(actingTenantId);
        payment.setPeriod(ym.toString());
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(LeasePaymentStatus.PENDING);
        LeasePayment saved = paymentRepository.save(payment);

        String successUrl = ensureSessionIdPlaceholder(applyTemplate(props.successUrlTemplate(), leaseId));
        String cancelUrl = applyTemplate(props.cancelUrlTemplate(), leaseId);

        String propertyName = StringUtils.hasText(lease.propertyNameSnapshot())
                ? lease.propertyNameSnapshot()
                : "Property #" + lease.propertyId();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Rent (" + ym + ") — " + propertyName)
                                        .build())
                                .build())
                        .build())
                .putMetadata("leaseId", String.valueOf(leaseId))
                .putMetadata("tenantId", String.valueOf(actingTenantId))
                .putMetadata("period", ym.toString())
                .putMetadata("paymentId", String.valueOf(saved.getId()))
                .build();

        try {
            Session session = Session.create(params);
            saved.setStripeCheckoutSessionId(session.getId());
            paymentRepository.save(saved);
            return new CreateCheckoutSessionResponse(session.getUrl(), session.getId(), saved.getId());
        } catch (StripeException e) {
            throw new BadRequestException("Unable to create Stripe Checkout Session: " + e.getMessage());
        }
    }

    @Transactional
    public void confirmCheckoutSessionPaid(long leaseId, long actingTenantId, String sessionId) {
        if (!StringUtils.hasText(props.secretKey())) {
            throw new BadRequestException("Stripe is not configured (PAYMENT_STRIPE_SECRET_KEY is empty)");
        }
        if (!StringUtils.hasText(sessionId)) {
            throw new BadRequestException("Missing sessionId");
        }
        Stripe.apiKey = props.secretKey();

        LeaseSnapshot lease = leaseClient.fetchLease(leaseId);
        if (lease.tenantId() != actingTenantId) {
            throw new ForbiddenException("X-Tenant-Id does not match this lease's tenant");
        }

        LeasePayment payment = paymentRepository.findByStripeCheckoutSessionId(sessionId).orElse(null);
        if (payment == null) {
            throw new NotFoundException("Payment not found for session: " + sessionId);
        }
        if (!payment.getLeaseId().equals(leaseId) || !payment.getTenantId().equals(actingTenantId)) {
            throw new ForbiddenException("Payment does not match lease/tenant");
        }
        if (payment.getStatus() == LeasePaymentStatus.PAID) {
            return;
        }

        final Session session;
        try {
            session = Session.retrieve(sessionId);
        } catch (StripeException e) {
            throw new BadRequestException("Unable to retrieve Stripe session: " + e.getMessage());
        }

        // Stripe sets paymentStatus to "paid" when funds are captured.
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new BadRequestException("Stripe session is not paid yet (payment_status=" + session.getPaymentStatus() + ")");
        }

        payment.setStatus(LeasePaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        if (StringUtils.hasText(session.getPaymentIntent())) {
            payment.setStripePaymentIntentId(session.getPaymentIntent());
        }
        paymentRepository.save(payment);
    }

    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        if (!StringUtils.hasText(props.webhookSecret())) {
            throw new BadRequestException("Stripe webhook is not configured (PAYMENT_STRIPE_WEBHOOK_SECRET is empty)");
        }
        if (!StringUtils.hasText(signatureHeader)) {
            throw new ForbiddenException("Missing Stripe-Signature header");
        }
        Stripe.apiKey = props.secretKey();

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, props.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new ForbiddenException("Invalid Stripe signature");
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        StripeObject obj = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(obj instanceof Session session)) {
            return;
        }

        String sessionId = session.getId();
        if (!StringUtils.hasText(sessionId)) {
            return;
        }

        LeasePayment payment = paymentRepository.findByStripeCheckoutSessionId(sessionId).orElse(null);
        if (payment == null) {
            return;
        }
        if (payment.getStatus() == LeasePaymentStatus.PAID) {
            return;
        }

        payment.setStatus(LeasePaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        if (StringUtils.hasText(session.getPaymentIntent())) {
            payment.setStripePaymentIntentId(session.getPaymentIntent());
        }
        paymentRepository.save(payment);
    }

    private static String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return "usd";
        }
        return currency.trim().toLowerCase(Locale.ROOT);
    }

    private static YearMonth parsePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            throw new BadRequestException("Missing period");
        }
        try {
            return YearMonth.parse(period.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("period must be YYYY-MM");
        }
    }

    private static void ensurePeriodInLease(YearMonth ym, LocalDate start, LocalDate end) {
        YearMonth s = YearMonth.from(start);
        YearMonth e = YearMonth.from(end);
        if (ym.isBefore(s) || ym.isAfter(e)) {
            throw new BadRequestException("period must be within lease term (" + s + " to " + e + ")");
        }
    }

    private static List<String> periodsForLease(LocalDate start, LocalDate end) {
        YearMonth cur = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        while (!cur.isAfter(last)) {
            out.add(cur.toString());
            cur = cur.plusMonths(1);
        }
        return out;
    }

    private static long toCents(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        return scaled.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    private static String applyTemplate(String template, long leaseId) {
        if (!StringUtils.hasText(template)) {
            return "http://localhost/tenant/leases/" + leaseId;
        }
        return template.replace("{leaseId}", String.valueOf(leaseId));
    }

    private static String ensureSessionIdPlaceholder(String url) {
        // Stripe recommends passing the session id back via success_url:
        // https://.../return?session_id={CHECKOUT_SESSION_ID}
        if (url.contains("{CHECKOUT_SESSION_ID}")) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "session_id={CHECKOUT_SESSION_ID}";
    }
}

