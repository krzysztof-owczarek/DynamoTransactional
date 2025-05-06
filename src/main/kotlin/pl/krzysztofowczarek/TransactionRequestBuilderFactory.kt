package pl.krzysztofowczarek

import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest

@Component
class TransactionRequestBuilderFactory {
    fun builder(): TransactWriteItemsEnhancedRequest.Builder = TransactWriteItemsEnhancedRequest.builder()
}