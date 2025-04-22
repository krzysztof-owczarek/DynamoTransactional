package pl.krzysztofowczarek

import org.springframework.beans.factory.ObjectFactory
import org.springframework.stereotype.Component

@Component
class DynamoTransactionManagerPrototypeFactory(
    private val factory: ObjectFactory<DynamoTransactionManager>
) {
    fun create() = factory.`object`
}