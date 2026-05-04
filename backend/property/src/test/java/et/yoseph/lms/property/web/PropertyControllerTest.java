package et.yoseph.lms.property.web;

import et.yoseph.lms.property.PropertyServiceApplication;
import et.yoseph.lms.property.domain.PropertyType;
import et.yoseph.lms.property.repository.PropertyRepository;
import et.yoseph.lms.property.web.dto.PropertyUpsertRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = PropertyServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PropertyControllerTest {

    @Autowired
    PropertyRepository propertyRepository;

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
        propertyRepository.deleteAll();
    }

    @Test
    void propertyFiltering_q_city_country_type() {
        PropertyUpsertRequest p1 = new PropertyUpsertRequest(
                "Sunrise Apartments",
                "123 Main St",
                null,
                "Addis Ababa",
                "Addis Ababa",
                "1000",
                "ET",
                PropertyType.RESIDENTIAL,
                "Nice residential property",
                null
        );
        PropertyUpsertRequest p2 = new PropertyUpsertRequest(
                "Bole Commercial Plaza",
                "45 Airport Rd",
                null,
                "Addis Ababa",
                "Addis Ababa",
                "1000",
                "ET",
                PropertyType.COMMERCIAL,
                "Retail and offices",
                null
        );
        PropertyUpsertRequest p3 = new PropertyUpsertRequest(
                "Gondar Land Lot",
                "Lot 7",
                null,
                "Gondar",
                "Amhara",
                "2000",
                "ET",
                PropertyType.LAND,
                "Land parcel",
                null
        );

        assertEquals(HttpStatus.CREATED, restTemplate.postForEntity(url("/api/v1/properties"), p1, Object.class).getStatusCode());
        assertEquals(HttpStatus.CREATED, restTemplate.postForEntity(url("/api/v1/properties"), p2, Object.class).getStatusCode());
        assertEquals(HttpStatus.CREATED, restTemplate.postForEntity(url("/api/v1/properties"), p3, Object.class).getStatusCode());

        var resType = restTemplate.getForEntity(url("/api/v1/properties?propertyType=RESIDENTIAL"), List.class);
        assertEquals(HttpStatus.OK, resType.getStatusCode());
        assertNotNull(resType.getBody());
        assertEquals(1, resType.getBody().size());
        assertEquals("Sunrise Apartments", ((java.util.Map<?, ?>) resType.getBody().getFirst()).get("name"));

        var resCity = restTemplate.getForEntity(url("/api/v1/properties?city=gond"), List.class);
        assertEquals(HttpStatus.OK, resCity.getStatusCode());
        assertNotNull(resCity.getBody());
        assertEquals(1, resCity.getBody().size());
        assertEquals("Gondar Land Lot", ((java.util.Map<?, ?>) resCity.getBody().getFirst()).get("name"));

        var resCountry = restTemplate.getForEntity(url("/api/v1/properties?country=et"), List.class);
        assertEquals(HttpStatus.OK, resCountry.getStatusCode());
        assertNotNull(resCountry.getBody());
        assertEquals(3, resCountry.getBody().size());

        var resQ = restTemplate.getForEntity(url("/api/v1/properties?q=airport"), List.class);
        assertEquals(HttpStatus.OK, resQ.getStatusCode());
        assertNotNull(resQ.getBody());
        assertEquals(1, resQ.getBody().size());
        assertEquals("Bole Commercial Plaza", ((java.util.Map<?, ?>) resQ.getBody().getFirst()).get("name"));
    }

    private String url(String path) {
        return "http://127.0.0.1:" + serverPort + path;
    }
}

