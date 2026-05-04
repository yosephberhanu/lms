package et.yoseph.lms.property.web;

import et.yoseph.lms.property.domain.PropertyType;
import et.yoseph.lms.property.service.PropertyService;
import et.yoseph.lms.property.web.dto.PropertyResponse;
import et.yoseph.lms.property.web.dto.PropertyUpsertRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@Tag(name = "Properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    @Operation(summary = "List properties (supports search/filtering)")
    public List<PropertyResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) String ownerPartyId
    ) {
        return propertyService.list(q, city, country, propertyType, ownerPartyId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get property by id")
    public PropertyResponse get(@PathVariable Long id) {
        return propertyService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create property (optional nested ownership rows)")
    public PropertyResponse create(@Valid @RequestBody PropertyUpsertRequest request) {
        return propertyService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace property and ownership rows")
    public PropertyResponse update(@PathVariable Long id, @Valid @RequestBody PropertyUpsertRequest request) {
        return propertyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete property")
    public void delete(@PathVariable Long id) {
        propertyService.delete(id);
    }
}
