package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "LeaseWriteRequest")
public record LeaseWriteRequest(
        @NotNull Long propertyId,
        /** Primary key of a {@link Tenant} managed by this service. */
        @NotNull Long tenantId,
        @Size(max = 128) String ownerId,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyRent,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @DecimalMin("0.0") BigDecimal depositAmount,
        @Size(max = 255) String paymentSchedule
) {
}
