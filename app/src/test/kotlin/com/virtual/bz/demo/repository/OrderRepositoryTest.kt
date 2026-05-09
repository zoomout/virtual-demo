package com.virtual.bz.demo.repository

import com.virtual.bz.demo.repository.entity.OrderEntity
import com.virtual.bz.demo.repository.entity.OrderStatusEntity
import com.virtual.bz.demo.utils.AuditingTestConfig
import com.virtual.bz.demo.utils.AuditingTestConfig.TestTimeData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.time.Instant

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = ["classpath:db/test_cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Import(AuditingTestConfig::class)
class OrderRepositoryTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val orderJpaRepository: OrderJpaRepository
) {

    @Test
    fun `should set audit timestamps when entity is saved and updated`() {
        // Given
        val fixedInstant = Instant.parse("2024-05-07T10:00:00Z")

        val order = OrderEntity(
            itemId = "item-123",
            status = OrderStatusEntity.PENDING
        )
        TestTimeData.time = fixedInstant

        // When
        orderJpaRepository.save(order)
        entityManager.flush()
        entityManager.clear()
        val reloadedOrder = orderJpaRepository.findById(order.id!!).get()


        // Then
        assertEquals(fixedInstant, reloadedOrder.createdAt, "createdAt should match the fixed instant")
        assertEquals(fixedInstant, reloadedOrder.updatedAt, "updatedAt should match the fixed instant")

        val advancedInstant = fixedInstant.plusSeconds(10)
        TestTimeData.time = advancedInstant

        // When
        reloadedOrder.status = OrderStatusEntity.PROCESSING
        orderJpaRepository.save(reloadedOrder)
        entityManager.flush()
        entityManager.clear()
        val reloadedUpdatedOrder = orderJpaRepository.findById(order.id!!).get()

        // Then
        assertEquals(fixedInstant, reloadedUpdatedOrder.createdAt, "createdAt should remain unchanged")
        assertEquals(advancedInstant, reloadedUpdatedOrder.updatedAt, "updatedAt should match the advanced instant")
    }

    @Test
    fun `should increment version when entity is updated`() {
        // Given
        val order = OrderEntity(
            itemId = "item-123",
            status = OrderStatusEntity.PENDING
        )
        val savedOrder = orderJpaRepository.save(order)
        entityManager.flush()
        val initialVersion = savedOrder.version

        // When
        savedOrder.status = OrderStatusEntity.PROCESSING
        val updatedProcessingOrder = orderJpaRepository.save(savedOrder)
        entityManager.flush()

        // Then
        assertEquals(initialVersion + 1, updatedProcessingOrder.version)

        // When
        updatedProcessingOrder.status = OrderStatusEntity.COMPLETED
        val savedCompletedOrder = orderJpaRepository.save(updatedProcessingOrder)
        entityManager.flush()

        // Then
        assertEquals(initialVersion + 2, savedCompletedOrder.version)
    }

    @Test
    fun `should mark as processing using native query and return the updated entity`() {
        // Given
        val order = OrderEntity(
            itemId = "item-123",
            status = OrderStatusEntity.PENDING
        )
        val savedOrder = orderJpaRepository.save(order)
        entityManager.flush()
        entityManager.clear()

        // When
        val updatedOrder = orderJpaRepository.markAsProcessing(savedOrder.id!!)

        // Then
        assertEquals(OrderStatusEntity.PROCESSING, updatedOrder?.status)
        assertEquals(savedOrder.version + 1, updatedOrder?.version)

        // Verify it was actually updated in DB
        entityManager.clear()
        val reloadedOrder = orderJpaRepository.findById(savedOrder.id!!).get()
        assertEquals(OrderStatusEntity.PROCESSING, reloadedOrder.status)
    }

    @Test
    fun `should return null when marking as processing if status is not pending`() {
        // Given
        val order = OrderEntity(
            itemId = "item-123",
            status = OrderStatusEntity.COMPLETED
        )
        val savedOrder = orderJpaRepository.save(order)
        entityManager.flush()
        entityManager.clear()

        // When
        val updatedOrder = orderJpaRepository.markAsProcessing(savedOrder.id!!)

        // Then
        assertEquals(null, updatedOrder)
    }

    @Test
    fun `should mark as completed using native query`() {
        // Given
        val order = OrderEntity(
            itemId = "item-123",
            status = OrderStatusEntity.PROCESSING
        )
        val savedOrder = orderJpaRepository.save(order)
        entityManager.flush()
        entityManager.clear()

        // When
        val updatedOrder = orderJpaRepository.markAsCompleted(savedOrder.id!!, "pay-123")

        // Then
        assertEquals(OrderStatusEntity.COMPLETED, updatedOrder?.status)
        assertEquals("pay-123", updatedOrder?.paymentId)
        assertEquals(savedOrder.version + 1, updatedOrder?.version)
    }

    @Test
    fun `should mark as failed using native query`() {
        // Given
        val order = OrderEntity(
            itemId = "item-123",
            status = OrderStatusEntity.PENDING
        )
        val savedOrder = orderJpaRepository.save(order)
        entityManager.flush()
        entityManager.clear()

        // When
        val updatedOrder = orderJpaRepository.markAsFailed(
            savedOrder.id!!,
            "PAYMENT_FAILURE",
            "pay-failed-123"
        )

        // Then
        assertEquals(OrderStatusEntity.FAILED, updatedOrder?.status)
        assertEquals("PAYMENT_FAILURE", updatedOrder?.failureReason?.name)
        assertEquals("pay-failed-123", updatedOrder?.paymentId)
    }
}
