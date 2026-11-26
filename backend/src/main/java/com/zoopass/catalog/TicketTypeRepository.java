package com.zoopass.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link TicketType}; searches use Specifications. */
public interface TicketTypeRepository extends JpaRepository<TicketType, Long>, JpaSpecificationExecutor<TicketType> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
