package et.yoseph.lms.payment.integration;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaseSnapshot(
        long leaseId,
        long tenantId,
        long propertyId,
        String propertyNameSnapshot,
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate
) {
}

