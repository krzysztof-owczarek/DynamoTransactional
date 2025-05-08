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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest

class DynamoTransactionManagerTest {

    private val enhancedClient = mockk<DynamoDbEnhancedClient>(relaxed = true)
    private val factory = mockk<DynamoTransactionRequestBuilderFactory>()
    private val builder = mockk<TransactWriteItemsEnhancedRequest.Builder>()

    private val testTable = mockk<DynamoDbTable<TestEntity>>()

    private lateinit var transactionManager : DynamoTransactionManager

    @BeforeEach
    fun setUp() {
        every { builder.addPutItem(any(), any<DynamoDbTable<Any>>()) } returns builder
        every { builder.addDeleteItem(any(), any<DynamoDbTable<Any>>()) } returns builder
        every { builder.build() } returns mockk()

        every { factory.builder() } returns builder

        transactionManager = DynamoTransactionManager(factory, enhancedClient)
    }

    @Test
    fun `calls putItem on builder on save and commit without exception`() {
        val entity = TestEntity("key1", "val1")

        transactionManager.save(testTable, entity)

        assertThatCode { transactionManager.commit() }
            .doesNotThrowAnyException()

        verifyOrder {
            builder.addPutItem(testTable, entity)
            builder.build()
            enhancedClient.transactWriteItems(any<TransactWriteItemsEnhancedRequest>())
        }
    }

    @Test
    fun `calls deleteItem on builder on save and commit without exception`() {
        val entity = TestEntity("key1", "val1")

        transactionManager.delete(testTable, entity)

        assertThatCode { transactionManager.commit() }
            .doesNotThrowAnyException()

        verifyOrder {
            builder.addDeleteItem(testTable, entity)
            builder.build()
            enhancedClient.transactWriteItems(any<TransactWriteItemsEnhancedRequest>())
        }
    }

    @Test
    fun `does not allow committing transaction that was already committed`() {
        val entity = TestEntity("key1", "val1")

        transactionManager.save(testTable, entity)
        transactionManager.commit()

        assertThatThrownBy { transactionManager.commit() }
            .isInstanceOf(DynamoTransactionExceptions.TransactionAlreadyCommittedException::class.java)
    }

    @Test
    fun `does not allow committing empty transaction`() {
        assertThatThrownBy { transactionManager.commit() }
            .isInstanceOf(DynamoTransactionExceptions.TransactionRequestEmptyException::class.java)
    }
}