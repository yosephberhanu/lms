package et.yoseph.lms.lease.repository;

import et.yoseph.lms.lease.domain.Lease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LeaseRepository extends JpaRepository<Lease, Long>, JpaSpecificationExecutor<Lease> {

    long countByTenant_Id(Long tenantId);

    @Query("""
            SELECT DISTINCT l FROM Lease l
            LEFT JOIN FETCH l.statusHistory
            LEFT JOIN FETCH l.tenant
            WHERE l.id = :id
            """)
    Optional<Lease> findDetailById(@Param("id") Long id);
}
