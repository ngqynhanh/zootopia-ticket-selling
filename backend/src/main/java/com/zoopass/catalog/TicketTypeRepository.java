package com.zoopass.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link TicketType}. */
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findByActiveTrueOrderByDisplayOrderAsc();

    List<TicketType> findByActiveTrueAndCategoryOrderByDisplayOrderAsc(TicketCategory category);

    Optional<TicketType> findByIdAndActiveTrue(Long id);
}
