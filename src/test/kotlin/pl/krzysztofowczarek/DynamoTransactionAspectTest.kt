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

import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile

@SpringBootTest
@Testcontainers
@Import(AwsTestConfiguration::class)
class DynamoTransactionAspectTest {

    @MockkBean
    private lateinit var transactionManagerFactory: DynamoTransactionManagerPrototypeFactory

    @Autowired
    private lateinit var repositoryMethodInvoker: RepositoryMethodInvoker

    private val transactionManager = mockk<DynamoTransactionManager>(relaxed = true)
    private val otherTransactionManager = mockk<DynamoTransactionManager>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { transactionManagerFactory.create() }
            .returns(transactionManager)
            .andThen(otherTransactionManager)
    }

    companion object {
        @Container
        @JvmStatic
        private val localstack: LocalStackContainer = LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.5.0")
        )
            .withServices(LocalStackContainer.Service.DYNAMODB)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("init-dynamo-table.sh"),
                "/etc/localstack/init/ready.d/"
            )

        @DynamicPropertySource
        @JvmStatic
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("aws.endpoint") {
                localstack.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
            }

            registry.add("aws.region") {
                localstack.region
            }
        }
    }

    @Test
    fun `adds item to the transaction and commits it`() {
        val entity = TestEntity("key1", "val1")

        repositoryMethodInvoker.saveAndCommit(entity)

        verifyOrder {
            transactionManager.save(any(), any<TestEntity>())
            transactionManager.commit()
        }
    }

    @Test
    fun `adds delete item to the transaction and commits it`() {
        val entity = TestEntity("key1", "val1")

        repositoryMethodInvoker.deleteAndCommit(entity)

        verifyOrder {
            transactionManager.delete(any(), any<TestEntity>())
            transactionManager.commit()
        }
    }

    @Test
    fun `reuses existing transaction when nested methods`() {
        val entity1 = TestEntity("key1", "val1")
        val entity2 = TestEntity("key2", "val2")

        repositoryMethodInvoker.nestedSaveAndCommitPropagationRequired(entity1, entity2)

        verifyOrder {
            transactionManager.save(any(), entity1)
            transactionManager.save(any(), entity2)
            transactionManager.commit()
        }

        verify { otherTransactionManager wasNot Called }
    }

    @Test
    fun `outer and inner transactions save and commit from different managers`() {
        val entity1 = TestEntity("key1", "val1")
        val entity2 = TestEntity("key2", "val2")
        // and
        val outerManager = mockk<DynamoTransactionManager>(relaxed = true)
        val innerManager = mockk<DynamoTransactionManager>(relaxed = true)
        // and
        every { transactionManagerFactory.create() }
            .returns(outerManager)
            .andThen(innerManager)

        repositoryMethodInvoker.nestedSaveAndCommitPropagationRequiresNew(entity1, entity2)

        verifyOrder {
            outerManager.save(any(), entity1)
            innerManager.save(any(), entity2)
            innerManager.commit()
            outerManager.commit()
        }
    }

    @Test
    fun `requires new performs as requires when there is no active transaction available`() {
        val entity1 = TestEntity("key1", "val1")
        // and
        val outerManager = mockk<DynamoTransactionManager>(relaxed = true)
        val innerManager = mockk<DynamoTransactionManager>(relaxed = true)
        // and
        every { transactionManagerFactory.create() }
            .returns(outerManager)
            .andThen(innerManager)

        repositoryMethodInvoker.saveAndCommitPropagationRequiresNew(entity1)

        verifyOrder {
            outerManager.save(any(), entity1)
            outerManager.commit()
        }

        verify { innerManager wasNot Called }
    }

    @Test
    fun `runtime exception thrown from inner transactions does not stop outer transaction from committing`() {
        val entity1 = TestEntity("key1", "val1")
        val entity2 = TestEntity("key2", "val2")
        // and
        val outerManager = mockk<DynamoTransactionManager>(relaxed = true)
        val innerManager = mockk<DynamoTransactionManager>(relaxed = true)
        // and
        every { transactionManagerFactory.create() }
            .returns(outerManager)
            .andThen(innerManager)

        try {
            repositoryMethodInvoker.nestedSaveAndCommitPropagationRequiresNewThrowingRuntimeException(entity1, entity2)
        } catch (_: RuntimeException) {
            // expected
        }

        verifyOrder {
            outerManager.save(any(), entity1)
            innerManager.save(any(), entity2)
            outerManager.commit()
        }

        verify(exactly = 0) { innerManager.commit() }
    }
}