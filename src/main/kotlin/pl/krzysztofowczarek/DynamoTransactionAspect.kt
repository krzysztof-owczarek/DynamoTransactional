package pl.krzysztofowczarek

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class DynamoTransactionAspect(
    private val dynamoTransactionManagerPrototypeFactory: DynamoTransactionManagerPrototypeFactory
) {

    private val log = KotlinLogging.logger {}

    private val dynamoTransactionManagerScopedRef = DynamoTransactionManagerScopedRef.scopedValue

    @Around("@annotation(writeTransaction")
    fun aroundWriteTransaction(
        joinPoint: ProceedingJoinPoint,
        dynamoWriteTransaction: DynamoWriteTransaction
    ): Any? {
        return when(dynamoWriteTransaction.propagation) {
            DynamoTransactionPropagation.REQUIRED -> {
                if (dynamoTransactionManagerScopedRef.isBound) {
                    log.debug { "[Transaction#${dynamoTransactionManagerScopedRef.get().transactionId}] " +
                            "Transaction already in progress..." }
                    return joinPoint.proceed()
                }

                startTransaction(dynamoTransactionManagerScopedRef, joinPoint)
            }
        }
    }

    private fun startTransaction(
        dynamoTransactionManagerScopedRef: ScopedValue<DynamoTransactionManager>,
        joinPoint: ProceedingJoinPoint
    ) = ScopedValue.where(dynamoTransactionManagerScopedRef, dynamoTransactionManagerPrototypeFactory.create()).call<Any, Throwable> {
        log.debug { "[Transaction#${dynamoTransactionManagerScopedRef.get().transactionId}] Starting transaction." }
        return@call joinPoint.proceed().also {
            dynamoTransactionManagerScopedRef.get().commit()
        }
    }
}