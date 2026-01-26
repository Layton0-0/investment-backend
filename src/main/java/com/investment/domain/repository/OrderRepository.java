package com.investment.domain.repository;

import com.investment.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    List<Order> findByAccountNo(String accountNo);
    
    List<Order> findByAccountNoAndStatus(String accountNo, Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.accountNo = :accountNo AND o.orderTime >= :startTime ORDER BY o.orderTime DESC")
    List<Order> findByAccountNoAndOrderTimeAfter(@Param("accountNo") String accountNo, 
                                                   @Param("startTime") LocalDateTime startTime);
    
    Optional<Order> findByIdAndAccountNo(String id, String accountNo);
}
