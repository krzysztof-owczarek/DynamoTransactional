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