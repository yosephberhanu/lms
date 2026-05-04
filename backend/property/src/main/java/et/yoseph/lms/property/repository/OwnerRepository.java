package et.yoseph.lms.property.repository;

import et.yoseph.lms.property.domain.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long>, JpaSpecificationExecutor<Owner> {
    boolean existsByPartyId(String partyId);

    Optional<Owner> findByPartyId(String partyId);
}

