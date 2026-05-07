package com.virtual.bz.demo.repository

import com.virtual.bz.demo.repository.entity.OrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderJpaRepository : JpaRepository<OrderEntity, UUID>
