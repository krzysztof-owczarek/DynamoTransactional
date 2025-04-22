package pl.krzysztofowczarek

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DynamoTransactionalManager is single usage, scoped functional wrapper on Dynamo Transaction call.
 * It means that it can execute one transaction only.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE) // TODO: Write why
class DynamoTransactionManager(
    transactionRequestBuilderFactory: TransactionRequestBuilderFactory,
    private val enhancedClient: DynamoDbEnhancedClient
) {

    val transactionId: String = UUID.randomUUID().toString()

    private val transactionRequestBuilder = transactionRequestBuilderFactory.builder()

    private val transactionClosed = AtomicBoolean(false)

    /**
     * Transactional request cannot be empty.
     * Boolean will be switched to true after first element will be added to the request.
     */
    private val committable = AtomicBoolean(false)

    fun <T> save(table: DynamoDbTable<T>, entity: T) {
        if (transactionClosed.get()) {
            throw RuntimeException("[Transaction#$transactionId] Transaction has been already closed.")
        }

        transactionRequestBuilder.addPutItem(
            table,
            entity
        )

        committable.set(true)
    }

    fun <T> delete(table: DynamoDbTable<T>, entity: T) {
        if (transactionClosed.get()) {
            throw RuntimeException("[Transaction#$transactionId] Transaction has been already closed.")
        }

        transactionRequestBuilder.addDeleteItem(
            table,
            entity
        )

        committable.set(true)
    }

    fun commit() {
        if (transactionClosed.compareAndSet(false, true)) {
            if (committable.get()) {
                enhancedClient.transactWriteItems(transactionRequestBuilder.build())
            } else {
                throw RuntimeException("[Transaction#$transactionId] Transaction is not committable" +
                        " because transactional request is empty.")
            }
        } else {
            throw RuntimeException("[Transaction#$transactionId] Transaction has been already closed.")
        }
    }
}