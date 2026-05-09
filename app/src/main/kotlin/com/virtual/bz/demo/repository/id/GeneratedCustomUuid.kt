package com.virtual.bz.demo.repository.id

import org.hibernate.annotations.IdGeneratorType

@IdGeneratorType(CustomUuidGenerator::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class GeneratedCustomUuid