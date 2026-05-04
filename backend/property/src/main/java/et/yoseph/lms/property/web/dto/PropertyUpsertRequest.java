package et.yoseph.lms.property.web.dto;

import et.yoseph.lms.property.domain.PropertyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "PropertyUpsertRequest")
public record PropertyUpsertRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String stateOrProvince,
        @NotBlank @Size(max = 32) String postalCode,
        @NotBlank @Size(max = 120) String country,
        @NotNull PropertyType propertyType,
        @Size(max = 2000) String description,
        @Valid List<OwnershipUpsertRequest> ownerships
) {
}
