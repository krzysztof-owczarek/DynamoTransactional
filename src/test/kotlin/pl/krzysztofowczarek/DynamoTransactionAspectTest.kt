package pl.krzysztofowczarek

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable

@SpringBootTest
@Testcontainers
@Import(AwsTestConfiguration::class)
class DynamoTransactionAspectTest {

    @MockkBean
    private lateinit var transactionManagerFactory: DynamoTransactionManagerPrototypeFactory

    private val transactionManager = mockk<DynamoTransactionManager>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { transactionManagerFactory.create() }.returns(transactionManager)
    }

    @Service
    class Invoker(private val testRepository: TestRepository) {

        @DynamoWriteTransaction
        fun saveAndCommit() {
            val entity = TestEntity("key1", "val1")
            testRepository.save(entity)
        }
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

    @Autowired
    private lateinit var testRepository: TestRepository

    @Test
    fun `adds item to the transaction and commits it`() {
        val testService = Invoker(testRepository)

        testService.saveAndCommit()

        verify { transactionManager.save(any<DynamoDbTable<TestEntity>>(), any()) }
        verify { transactionManager.commit() }
    }
}