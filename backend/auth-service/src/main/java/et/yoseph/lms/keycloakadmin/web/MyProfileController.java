package et.yoseph.lms.keycloakadmin.web;

import et.yoseph.lms.keycloakadmin.service.KeycloakMyProfileService;
import et.yoseph.lms.keycloakadmin.web.dto.MyProfileResponse;
import et.yoseph.lms.keycloakadmin.web.dto.MyProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/v1/me")
public class MyProfileController {

    private final KeycloakMyProfileService profileService;

    public MyProfileController(KeycloakMyProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public MyProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return profileService.me(jwt);
    }

    @PutMapping
    public MyProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MyProfileUpdateRequest request) {
        return profileService.updateMe(jwt, request);
    }
}

