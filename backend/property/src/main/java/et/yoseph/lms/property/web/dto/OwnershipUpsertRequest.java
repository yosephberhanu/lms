package et.yoseph.lms.property.web.dto;

import et.yoseph.lms.property.domain.OwnershipRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "OwnershipUpsertRequest")
public record OwnershipUpsertRequest(
        @NotBlank @Size(max = 128) String ownerPartyId,
        @NotNull OwnershipRole role,
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        BigDecimal ownershipPercentage,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @Size(max = 1000) String notes
) {
}
