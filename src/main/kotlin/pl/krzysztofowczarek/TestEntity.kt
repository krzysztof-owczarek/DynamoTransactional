package pl.krzysztofowczarek

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class TestEntity(
    @get:DynamoDbPartitionKey
    var partitionKey: String? = null,
    var value: String? = null
)