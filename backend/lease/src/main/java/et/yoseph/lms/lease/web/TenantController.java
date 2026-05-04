package et.yoseph.lms.lease.web;

import et.yoseph.lms.lease.service.TenantService;
import et.yoseph.lms.lease.web.dto.TenantResponse;
import et.yoseph.lms.lease.web.dto.TenantUpsertRequest;
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
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @Operation(summary = "List tenants (search / filter)")
    public List<TenantResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String externalPartyId,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return tenantService.list(q, externalPartyId, displayName, email, phone);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by id")
    public TenantResponse get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create tenant")
    public TenantResponse create(@Valid @RequestBody TenantUpsertRequest request) {
        return tenantService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace tenant")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody TenantUpsertRequest request) {
        return tenantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete tenant")
    public void delete(@PathVariable Long id) {
        tenantService.delete(id);
    }
}
