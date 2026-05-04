package et.yoseph.lms.lease.integration;

import et.yoseph.lms.lease.service.BadRequestException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Synchronous REST client for the Property service (spec: Lease → Property).
 */
@Component
public class PropertyClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public PropertyClient(RestClient propertyServiceRestClient) {
        this.restClient = propertyServiceRestClient;
    }

    /**
     * @return property display name, or empty string if missing in payload
     */
    public String fetchPropertyName(long propertyId) {
        try {
            Map<String, Object> root = restClient.get()
                    .uri("/api/v1/properties/{id}", propertyId)
                    .retrieve()
                    .body(MAP_TYPE);
            if (root == null) {
                return "";
            }
            Object name = root.get("name");
            return name != null ? name.toString() : "";
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                throw new BadRequestException("Property not found: " + propertyId);
            }
            throw new BadRequestException("Property service error: " + e.getMessage());
        }
    }

    public void assertPropertyExists(long propertyId) {
        fetchPropertyName(propertyId);
    }

    /**
     * Property ids where {@code ownerPartyId} appears on at least one ownership row
     * (same semantics as {@code GET /api/v1/properties?ownerPartyId=}).
     */
    public List<Long> fetchPropertyIdsForOwnerParty(String ownerPartyId) {
        try {
            List<Map<String, Object>> rows = restClient.get()
                    .uri("/api/v1/properties?ownerPartyId={party}", ownerPartyId.trim())
                    .retrieve()
                    .body(LIST_OF_MAPS_TYPE);
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            Set<Long> idSet = new LinkedHashSet<>();
            for (Map<String, Object> row : rows) {
                Object idObj = row.get("id");
                if (idObj instanceof Number n) {
                    idSet.add(n.longValue());
                }
            }
            return new ArrayList<>(idSet);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                return List.of();
            }
            throw new BadRequestException("Property service error: " + e.getMessage());
        }
    }
}
