package pl.krzysztofowczarek

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DynamoWriteTransaction(
    val propagation: DynamoTransactionPropagation = DynamoTransactionPropagation.REQUIRED
)
