package et.yoseph.lms.lease.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Published after a lease is persisted (in-process; message broker can subscribe later).
 */
public record LeaseCreatedEvent(
        long leaseId,
        long propertyId,
        long tenantId,
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt
) {
}
