package com.virtual.bz.demo.utils

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@TestConfiguration
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
class AuditingTestConfig {

    @Bean
    fun testDateTimeProvider(): DateTimeProvider {
        return DateTimeProvider { Optional.of(mockTime.get()) }
    }

    companion object TestTimeData {
        private val mockTime = AtomicReference(Instant.now())
        var time: Instant
            get() = mockTime.get()
            set(value) = mockTime.set(value)
    }
}