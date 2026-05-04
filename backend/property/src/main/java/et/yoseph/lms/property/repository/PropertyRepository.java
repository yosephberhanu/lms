package et.yoseph.lms.property.repository;

import et.yoseph.lms.property.domain.Property;
import et.yoseph.lms.property.domain.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.ownerships")
    List<Property> findAllWithOwnerships();

    @Query("""
            SELECT DISTINCT p
            FROM Property p
            LEFT JOIN FETCH p.ownerships
            WHERE (:propertyType IS NULL OR p.propertyType = :propertyType)
              AND (:city = '' OR LOWER(p.city) LIKE CONCAT('%', :city, '%'))
              AND (:country = '' OR LOWER(p.country) LIKE CONCAT('%', :country, '%'))
              AND (
                :q = '' OR
                LOWER(p.name) LIKE CONCAT('%', :q, '%') OR
                LOWER(p.addressLine1) LIKE CONCAT('%', :q, '%') OR
                LOWER(COALESCE(p.addressLine2, '')) LIKE CONCAT('%', :q, '%') OR
                LOWER(p.city) LIKE CONCAT('%', :q, '%') OR
                LOWER(p.stateOrProvince) LIKE CONCAT('%', :q, '%') OR
                LOWER(p.postalCode) LIKE CONCAT('%', :q, '%') OR
                LOWER(p.country) LIKE CONCAT('%', :q, '%') OR
                LOWER(COALESCE(p.description, '')) LIKE CONCAT('%', :q, '%')
              )
              AND (
                :ownerPartyId = '' OR EXISTS (
                    SELECT 1 FROM PropertyOwnership po
                    WHERE po.property.id = p.id AND po.ownerPartyId = :ownerPartyId
                )
              )
            """)
    List<Property> searchWithOwnerships(
            @Param("q") String q,
            @Param("city") String city,
            @Param("country") String country,
            @Param("propertyType") PropertyType propertyType,
            @Param("ownerPartyId") String ownerPartyId);

    @Query("SELECT DISTINCT p FROM Property p LEFT JOIN FETCH p.ownerships WHERE p.id = :id")
    Optional<Property> findDetailById(@Param("id") Long id);
}
