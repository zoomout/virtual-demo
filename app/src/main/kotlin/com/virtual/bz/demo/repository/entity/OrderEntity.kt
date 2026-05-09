package com.virtual.bz.demo.repository.entity

import com.virtual.bz.demo.repository.id.GeneratedCustomUuid
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener::class)
class OrderEntity(
    @Id
    @Column(name = "id")
    @field:GeneratedCustomUuid
    var id: UUID? = null,
    @Column(name = "payment_id")
    var paymentId: String? = null,
    @Column(name = "item_id")
    var itemId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: OrderStatusEntity,
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    var failureReason: FailureReasonEntity? = null,
    @Version
    @Column(name = "version")
    var version: Long = 0,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as OrderEntity

        return id != null && id == other.id
    }

    override fun hashCode(): Int = OrderEntity::class.java.hashCode()

    override fun toString(): String =
        "OrderEntity(id=$id, status=$status, createdAt=$createdAt)"
}

enum class OrderStatusEntity {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}

enum class FailureReasonEntity {
    PAYMENT_FAILURE,
    INVENTORY_FAILURE,
    PAYMENT_AND_INVENTORY_FAILURE,
}
