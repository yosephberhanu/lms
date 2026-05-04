package et.yoseph.lms.property.web.dto;

import et.yoseph.lms.property.domain.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "PropertyResponse")
public record PropertyResponse(
        Long id,
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String stateOrProvince,
        String postalCode,
        String country,
        PropertyType propertyType,
        String description,
        Instant createdAt,
        Instant updatedAt,
        List<OwnershipResponse> ownerships
) {
}
