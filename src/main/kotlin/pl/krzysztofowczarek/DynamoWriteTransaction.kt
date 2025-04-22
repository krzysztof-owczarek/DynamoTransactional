package pl.krzysztofowczarek

annotation class DynamoWriteTransaction(
    val propagation: DynamoTransactionPropagation = DynamoTransactionPropagation.REQUIRED
)
