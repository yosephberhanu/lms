package et.yoseph.lms.property.web;

import et.yoseph.lms.property.service.PropertyOwnershipService;
import et.yoseph.lms.property.web.dto.OwnershipResponse;
import et.yoseph.lms.property.web.dto.OwnershipUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/ownerships")
@Tag(name = "Property ownership")
public class PropertyOwnershipController {

    private final PropertyOwnershipService ownershipService;

    public PropertyOwnershipController(PropertyOwnershipService ownershipService) {
        this.ownershipService = ownershipService;
    }

    @GetMapping
    @Operation(summary = "List ownership rows for a property")
    public List<OwnershipResponse> list(@PathVariable Long propertyId) {
        return ownershipService.listForProperty(propertyId);
    }

    @GetMapping("/{ownershipId}")
    @Operation(summary = "Get ownership row")
    public OwnershipResponse get(@PathVariable Long propertyId, @PathVariable Long ownershipId) {
        return ownershipService.get(propertyId, ownershipId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add ownership row")
    public OwnershipResponse create(
            @PathVariable Long propertyId,
            @Valid @RequestBody OwnershipUpsertRequest request) {
        return ownershipService.create(propertyId, request);
    }

    @PutMapping("/{ownershipId}")
    @Operation(summary = "Update ownership row")
    public OwnershipResponse update(
            @PathVariable Long propertyId,
            @PathVariable Long ownershipId,
            @Valid @RequestBody OwnershipUpsertRequest request) {
        return ownershipService.update(propertyId, ownershipId, request);
    }

    @DeleteMapping("/{ownershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete ownership row")
    public void delete(@PathVariable Long propertyId, @PathVariable Long ownershipId) {
        ownershipService.delete(propertyId, ownershipId);
    }
}
