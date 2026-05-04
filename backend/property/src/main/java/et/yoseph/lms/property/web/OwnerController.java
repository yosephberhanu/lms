package et.yoseph.lms.property.web;

import et.yoseph.lms.property.service.OwnerService;
import et.yoseph.lms.property.web.dto.OwnerResponse;
import et.yoseph.lms.property.web.dto.OwnerUpsertRequest;
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
@RequestMapping("/api/v1/owners")
@Tag(name = "Owners")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping
    @Operation(summary = "List owners (supports search/filtering)")
    public List<OwnerResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String partyId,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone
    ) {
        return ownerService.list(q, partyId, displayName, email, phone);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get owner by id")
    public OwnerResponse get(@PathVariable Long id) {
        return ownerService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create owner")
    public OwnerResponse create(@Valid @RequestBody OwnerUpsertRequest request) {
        return ownerService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace owner")
    public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody OwnerUpsertRequest request) {
        return ownerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete owner")
    public void delete(@PathVariable Long id) {
        ownerService.delete(id);
    }
}

