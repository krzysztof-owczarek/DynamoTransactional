package pl.krzysztofowczarek

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable

class DynamoDbDao<T> (
    private val table: DynamoDbTable<T>
) {
    private val transactionMangerRef = DynamoTransactionManagerScopedRef.scopedValue

    fun save(entity: T) {
        if (transactionMangerRef.isBound) {
            transactionMangerRef.get().save(table, entity)
        } else {
            table.putItem(entity)
        }
    }

    fun delete(entity: T) {
        if (transactionMangerRef.isBound) {
            transactionMangerRef.get().delete(table, entity)
        } else {
            table.deleteItem(entity)
        }
    }

    fun commit() {
        if (transactionMangerRef.isBound) {
            transactionMangerRef.get().commit()
        }
    }
}