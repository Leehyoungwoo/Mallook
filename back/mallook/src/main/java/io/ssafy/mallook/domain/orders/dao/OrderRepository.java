package io.ssafy.mallook.domain.orders.dao;

import io.ssafy.mallook.domain.member.entity.Member;
import io.ssafy.mallook.domain.orders.entity.Orders;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {


    Slice<Orders> findByIdLessThanAndMemberOrderByIdDesc(Long id, Member member, Pageable pageable);

    Long countByMember(Member member);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Orders o
            set o.status = false
            where o.id in :deleteList and o.status = true 
            """)
    void deleteOrder(@Param("deleteList") List<Long> deleteList);

    @Query("SELECT max (o.id) from Orders o")
    Long findMaxOrderId();

}
