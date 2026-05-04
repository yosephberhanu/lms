package et.yoseph.lms.property.web;

import et.yoseph.lms.property.PropertyServiceApplication;
import et.yoseph.lms.property.repository.OwnerRepository;
import et.yoseph.lms.property.web.dto.OwnerUpsertRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = PropertyServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OwnerControllerTest {

    @Autowired
    OwnerRepository ownerRepository;

    @LocalServerPort
    private int serverPort;

    private final RestTemplate restTemplate = restTemplateThatDoesNotThrowOnErrorStatus();

    private static RestTemplate restTemplateThatDoesNotThrowOnErrorStatus() {
        RestTemplate t = new RestTemplate();
        t.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
        return t;
    }

    @BeforeEach
    void setUp() {
        ownerRepository.deleteAll();
    }

    @Test
    void ownerCrud_happyPath() {
        OwnerUpsertRequest create = new OwnerUpsertRequest("user-1", "Alice Owner", "alice@example.com", "+251900000000");

        var createResp = restTemplate.postForEntity(url("/api/v1/owners"), create, Map.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        assertNotNull(createResp.getBody());
        assertNotNull(createResp.getBody().get("id"));
        assertEquals("user-1", createResp.getBody().get("partyId"));

        Long id = ((Number) createResp.getBody().get("id")).longValue();

        var getResp = restTemplate.getForEntity(url("/api/v1/owners/" + id), Map.class);
        assertEquals(HttpStatus.OK, getResp.getStatusCode());
        assertNotNull(getResp.getBody());
        assertEquals(id, ((Number) getResp.getBody().get("id")).longValue());
        assertEquals("Alice Owner", getResp.getBody().get("displayName"));

        OwnerUpsertRequest update = new OwnerUpsertRequest("user-1", "Alice Updated", "alice@example.com", "+251900000000");
        var updateResp = restTemplate.exchange(
                url("/api/v1/owners/" + id),
                HttpMethod.PUT,
                new HttpEntity<>(update),
                Map.class
        );
        assertEquals(HttpStatus.OK, updateResp.getStatusCode());
        assertNotNull(updateResp.getBody());
        assertEquals("Alice Updated", updateResp.getBody().get("displayName"));

        var listResp = restTemplate.getForEntity(url("/api/v1/owners"), List.class);
        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertNotNull(listResp.getBody());
        assertEquals(1, listResp.getBody().size());

        var deleteResp = restTemplate.exchange(url("/api/v1/owners/" + id), HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());

        var afterDelete = restTemplate.getForEntity(url("/api/v1/owners/" + id), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
        assertNotNull(afterDelete.getBody());
        assertEquals("Not Found", afterDelete.getBody().get("title"));
    }

    @Test
    void ownerFiltering_searchAndFieldFilters() {
        assertEquals(HttpStatus.CREATED,
                restTemplate.postForEntity(url("/api/v1/owners"),
                        new OwnerUpsertRequest("user-1001", "Sample Owner 1001", "o1@example.com", "111"),
                        Map.class).getStatusCode());
        assertEquals(HttpStatus.CREATED,
                restTemplate.postForEntity(url("/api/v1/owners"),
                        new OwnerUpsertRequest("org-2001", "Sample Org 2001", "org@example.com", "222"),
                        Map.class).getStatusCode());

        var qResp = restTemplate.getForEntity(url("/api/v1/owners?q=org-2001"), List.class);
        assertEquals(HttpStatus.OK, qResp.getStatusCode());
        assertNotNull(qResp.getBody());
        assertEquals(1, qResp.getBody().size());
        assertEquals("org-2001", ((Map<?, ?>) qResp.getBody().getFirst()).get("partyId"));

        var dnResp = restTemplate.getForEntity(url("/api/v1/owners?displayName=owner"), List.class);
        assertEquals(HttpStatus.OK, dnResp.getStatusCode());
        assertNotNull(dnResp.getBody());
        assertEquals(1, dnResp.getBody().size());
        assertEquals("user-1001", ((Map<?, ?>) dnResp.getBody().getFirst()).get("partyId"));

        var emailResp = restTemplate.getForEntity(url("/api/v1/owners?email=example.com"), List.class);
        assertEquals(HttpStatus.OK, emailResp.getStatusCode());
        assertNotNull(emailResp.getBody());
        assertEquals(2, emailResp.getBody().size());
    }

    @Test
    void ownerCreate_duplicatePartyId_returns400() {
        OwnerUpsertRequest create = new OwnerUpsertRequest("user-dup", "Dup", null, null);

        assertEquals(HttpStatus.CREATED, restTemplate.postForEntity(url("/api/v1/owners"), create, Map.class).getStatusCode());

        var dupResp = restTemplate.postForEntity(url("/api/v1/owners"), create, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, dupResp.getStatusCode());
        assertNotNull(dupResp.getBody());
        assertEquals("Bad Request", dupResp.getBody().get("title"));
    }

    @Test
    void ownerUpdate_changePartyIdToExisting_returns400() {
        var a = restTemplate.postForEntity(
                url("/api/v1/owners"),
                new OwnerUpsertRequest("user-a", "A", null, null),
                Map.class
        );
        assertEquals(HttpStatus.CREATED, a.getStatusCode());
        assertNotNull(a.getBody());
        long aId = ((Number) a.getBody().get("id")).longValue();

        assertEquals(HttpStatus.CREATED, restTemplate.postForEntity(
                url("/api/v1/owners"),
                new OwnerUpsertRequest("user-b", "B", null, null),
                Map.class
        ).getStatusCode());

        var badUpdate = restTemplate.exchange(
                url("/api/v1/owners/" + aId),
                HttpMethod.PUT,
                new HttpEntity<>(new OwnerUpsertRequest("user-b", "A", null, null)),
                Map.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, badUpdate.getStatusCode());
        assertNotNull(badUpdate.getBody());
        assertEquals("Bad Request", badUpdate.getBody().get("title"));
    }

    private String url(String path) {
        return "http://127.0.0.1:" + serverPort + path;
    }
}

