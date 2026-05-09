package com.virtual.bz.demo.repository.id

import com.fasterxml.uuid.Generators
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.IdentifierGenerator

class CustomUuidGenerator : IdentifierGenerator {
    override fun generate(
        session: SharedSessionContractImplementor,
        owner: Any
    ): Any = Generators.timeBasedEpochGenerator().generate()
}
