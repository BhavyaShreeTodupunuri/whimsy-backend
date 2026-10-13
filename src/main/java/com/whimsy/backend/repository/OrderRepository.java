
package com.whimsy.backend.repository;

import com.whimsy.backend.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository
        extends JpaRepository<OrderRecord, Long> {

    List<OrderRecord> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}

