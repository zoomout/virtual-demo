package com.virtual.bz.demo.repository

import com.virtual.bz.demo.repository.entity.OrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderJpaRepository : JpaRepository<OrderEntity, UUID> {

    @Query(
        value = """
            UPDATE orders 
            SET status = 'PROCESSING', 
                version = version + 1, 
                updated_at = NOW() 
            WHERE id = :id AND status = 'PENDING' 
            RETURNING *
        """,
        nativeQuery = true
    )
    fun markAsProcessing(id: UUID): OrderEntity?

    @Query(
        value = """
            UPDATE orders 
            SET status = 'COMPLETED', 
                payment_id = :paymentId,
                version = version + 1, 
                updated_at = NOW() 
            WHERE id = :id AND status = 'PROCESSING' 
            RETURNING *
        """,
        nativeQuery = true
    )
    fun markAsCompleted(id: UUID, paymentId: String): OrderEntity?

    @Query(
        value = """
            UPDATE orders 
            SET status = 'FAILED', 
                failure_reason = :failureReason,
                payment_id = :paymentId,
                version = version + 1, 
                updated_at = NOW() 
            WHERE id = :id
            RETURNING *
        """,
        nativeQuery = true
    )
    fun markAsFailed(id: UUID, failureReason: String, paymentId: String?): OrderEntity?
}
