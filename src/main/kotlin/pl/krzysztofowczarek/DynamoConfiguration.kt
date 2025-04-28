package pl.krzysztofowczarek

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

private const val DYNAMO_TEST_TABLE = "TestTable"

@Configuration
class DynamoConfiguration {

    @Bean
    fun testDynamoDbDao(enhancedClient: DynamoDbEnhancedClient): DynamoDbDao<TestEntity> {
        val table = enhancedClient.table(
            DYNAMO_TEST_TABLE,
            TableSchema.fromBean(TestEntity::class.java)
        )

        return DynamoDbDao<TestEntity>(table)
    }
}