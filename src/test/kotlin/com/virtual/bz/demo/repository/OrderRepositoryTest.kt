package com.virtual.bz.demo.repository

import com.virtual.bz.demo.repository.entity.OrderEntity
import com.virtual.bz.demo.repository.entity.OrderStatusEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.util.*

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = ["classpath:db/test_cleanup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderRepositoryTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val orderJpaRepository: OrderJpaRepository
) {

    @Test
    fun `should increment version when entity is updated`() {
        // Given
        val order = OrderEntity(
            id = UUID.randomUUID(),
            itemId = "item-123",
            status = OrderStatusEntity.PENDING
        )
        val savedOrder = orderJpaRepository.save(order)
        entityManager.flush()
        val initialVersion = savedOrder.version

        // When
        val orderToUpdate = savedOrder.copy(status = OrderStatusEntity.PROCESSING)
        val updatedOrder = orderJpaRepository.save(orderToUpdate)
        entityManager.flush()

        // Then
        assertEquals(initialVersion + 1, updatedOrder.version)
    }
}
