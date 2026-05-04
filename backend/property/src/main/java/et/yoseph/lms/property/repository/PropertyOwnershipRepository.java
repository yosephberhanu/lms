package et.yoseph.lms.property.repository;

import et.yoseph.lms.property.domain.PropertyOwnership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyOwnershipRepository extends JpaRepository<PropertyOwnership, Long> {

    List<PropertyOwnership> findByPropertyIdOrderByIdAsc(Long propertyId);

    Optional<PropertyOwnership> findByPropertyIdAndId(Long propertyId, Long id);
}
