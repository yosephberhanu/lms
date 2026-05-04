package et.yoseph.lms.gateway.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class WelcomePageController {

    private static final Resource WELCOME = new ClassPathResource("static/welcome/index.html");

    @GetMapping(value = "/welcome-page", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<Resource> welcome() {
        return Mono.just(WELCOME);
    }
}
