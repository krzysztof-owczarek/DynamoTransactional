/*
 * MIT License
 *
 * Copyright (c) [2025] [Krzysztof Owczarek]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pl.krzysztofowczarek

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DynamoTransactionalManager is single usage, scoped functional wrapper on Dynamo Transaction call.
 * It means that it can execute one transaction only.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DynamoTransactionManager(
    dynamoTransactionRequestBuilderFactory: DynamoTransactionRequestBuilderFactory,
    private val enhancedClient: DynamoDbEnhancedClient
) {

    val transactionId: TransactionId = TransactionId.create()

    private val transactionRequestBuilder = dynamoTransactionRequestBuilderFactory.builder()

    private val transactionClosed = AtomicBoolean(false)

    /**
     * Transactional request cannot be empty.
     * Boolean will be switched to true after first element will be added to the request.
     */
    private val committable = AtomicBoolean(false)

    fun <T> save(table: DynamoDbTable<T>, entity: T) {
        if (transactionClosed.get()) {
            throw DynamoTransactionExceptions.TransactionAlreadyCommittedException(transactionId)
        }

        transactionRequestBuilder.addPutItem(
            table,
            entity
        )

        committable.set(true)
    }

    fun <T> delete(table: DynamoDbTable<T>, entity: T) {
        if (transactionClosed.get()) {
            throw DynamoTransactionExceptions.TransactionAlreadyCommittedException(transactionId)
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
                throw DynamoTransactionExceptions.TransactionRequestEmptyException(transactionId)
            }
        } else {
            throw DynamoTransactionExceptions.TransactionAlreadyCommittedException(transactionId)
        }
    }
}