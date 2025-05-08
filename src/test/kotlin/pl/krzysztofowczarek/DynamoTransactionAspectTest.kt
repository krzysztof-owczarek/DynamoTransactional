package pl.krzysztofowczarek

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
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

    @BeforeEach
    fun setUp() {
        every { transactionManagerFactory.create() }.returns(transactionManager)
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
    }
}