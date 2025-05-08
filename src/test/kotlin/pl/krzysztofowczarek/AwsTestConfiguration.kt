package pl.krzysztofowczarek

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

@TestConfiguration
class AwsTestConfiguration {

    @Bean
    fun localstackCredentialsProvider(): AwsCredentialsProvider {
        val basicCredentials = AwsBasicCredentials.create("key", "secret")
        return StaticCredentialsProvider.create(basicCredentials)
    }

    @Bean
    fun dynamoDbClient(awsProperties: AwsProperties, credentialsProvider: AwsCredentialsProvider)
            = DynamoDbClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(awsProperties.region))
                .endpointOverride(URI.create(awsProperties.endpoint!!))
                .build()

    @Bean
    fun dynamoDbEnhancedClient(dynamoDbClient: DynamoDbClient)
            = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build()

}