package et.yoseph.lms.property.web.dto;

import et.yoseph.lms.property.domain.OwnershipRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "OwnershipResponse")
public record OwnershipResponse(
        Long id,
        String ownerPartyId,
        OwnershipRole role,
        BigDecimal ownershipPercentage,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String notes
) {
}
