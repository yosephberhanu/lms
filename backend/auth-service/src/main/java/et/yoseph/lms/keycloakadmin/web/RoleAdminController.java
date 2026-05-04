package et.yoseph.lms.keycloakadmin.web;

import et.yoseph.lms.keycloakadmin.service.KeycloakUserAdminService;
import et.yoseph.lms.keycloakadmin.web.dto.RealmRoleResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1/roles")
public class RoleAdminController {

    private final KeycloakUserAdminService keycloakUserAdminService;

    public RoleAdminController(KeycloakUserAdminService keycloakUserAdminService) {
        this.keycloakUserAdminService = keycloakUserAdminService;
    }

    @GetMapping
    public List<RealmRoleResponse> list(@RequestParam(required = false) String prefix) {
        return keycloakUserAdminService.listRealmRoles(prefix);
    }
}

