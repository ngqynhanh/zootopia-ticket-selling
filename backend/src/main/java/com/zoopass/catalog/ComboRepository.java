package com.zoopass.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link Combo}. */
public interface ComboRepository extends JpaRepository<Combo, Long>, JpaSpecificationExecutor<Combo> {

    Page<Combo> findByActiveTrue(Pageable pageable);

    Optional<Combo> findByIdAndActiveTrue(Long id);
}
