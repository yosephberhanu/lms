package et.yoseph.lms.keycloakadmin.web;

import et.yoseph.lms.keycloakadmin.service.KeycloakUserAdminService;
import et.yoseph.lms.keycloakadmin.web.dto.AdminUserResponse;
import et.yoseph.lms.keycloakadmin.web.dto.RealmRoleResponse;
import et.yoseph.lms.keycloakadmin.web.dto.UserCreateRequest;
import et.yoseph.lms.keycloakadmin.web.dto.UserUpdateRequest;
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
@RequestMapping("/api/admin/v1/users")
public class UserAdminController {

    private final KeycloakUserAdminService keycloakUserAdminService;

    public UserAdminController(KeycloakUserAdminService keycloakUserAdminService) {
        this.keycloakUserAdminService = keycloakUserAdminService;
    }

    @GetMapping
    public List<AdminUserResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int first,
            @RequestParam(defaultValue = "50") int max,
            @RequestParam(defaultValue = "false") boolean includeRoles) {
        return keycloakUserAdminService.list(search, first, max, includeRoles);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable String id) {
        return keycloakUserAdminService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(@Valid @RequestBody UserCreateRequest request) {
        String id = keycloakUserAdminService.create(request);
        return keycloakUserAdminService.get(id);
    }

    @PutMapping("/{id}")
    public AdminUserResponse update(@PathVariable String id, @Valid @RequestBody UserUpdateRequest request) {
        keycloakUserAdminService.update(id, request);
        return keycloakUserAdminService.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        keycloakUserAdminService.delete(id);
    }
}
