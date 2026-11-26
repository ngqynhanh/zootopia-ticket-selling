package com.zoopass.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link Combo}; searches use Specifications. */
public interface ComboRepository extends JpaRepository<Combo, Long>, JpaSpecificationExecutor<Combo> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
