/*
 * MIT License
 *
 * Copyright (c) [2025] [Krzysztof Owczarek]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

    private val errorOrRuntimeExceptionThrownFromNestedTransaction = ThreadLocal.withInitial { false }

    private val log = KotlinLogging.logger {}

    private val dynamoTransactionManagerScopedRef = DynamoTransactionManagerScopedRef.scopedValue

    @Around("@annotation(dynamoWriteTransaction)")
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

            DynamoTransactionPropagation.REQUIRES_NEW -> {
                if (dynamoTransactionManagerScopedRef.isBound) {
                   return startNestedTransaction(dynamoTransactionManagerScopedRef, joinPoint)
                }

                log.debug { "[Transaction#${dynamoTransactionManagerScopedRef.get().transactionId}] " +
                        "No transaction in progress. Starting new transaction." }
                startTransaction(dynamoTransactionManagerScopedRef, joinPoint)
            }
        }
    }

    private fun startNestedTransaction(
        dynamoTransactionManagerScopedRef: ScopedValue<DynamoTransactionManager>,
        joinPoint: ProceedingJoinPoint
    ) {
        log.debug { "[Transaction#${dynamoTransactionManagerScopedRef.get().transactionId}] " +
                "Starting nested transaction." }

        ScopedValue.where(dynamoTransactionManagerScopedRef, dynamoTransactionManagerPrototypeFactory.create()).call<Any, Throwable> {
            try {
                return@call joinPoint.proceed().also {
                    dynamoTransactionManagerScopedRef.get().commit()
                }
            } catch (e: Throwable) {
                handleException(dynamoTransactionManagerScopedRef, e, isNestedTransactionException = true)
            }
        }
    }

    private fun handleException(
        dynamoTransactionManagerScopedRef: ScopedValue<DynamoTransactionManager>,
        e: Throwable,
        isNestedTransactionException: Boolean = false
    ) {
        when (e) {
            is Error, is RuntimeException -> {
                log.error(e) { "[Transaction#${dynamoTransactionManagerScopedRef.get().transactionId}] " +
                        "Exception occurred. Transaction will not be committed: ${e.cause }" }
                if (isNestedTransactionException) {
                    errorOrRuntimeExceptionThrownFromNestedTransaction.set(true)
                }
                throw e
            }
        }
    }

    private fun startTransaction(
        dynamoTransactionManagerScopedRef: ScopedValue<DynamoTransactionManager>,
        joinPoint: ProceedingJoinPoint
    ) = ScopedValue.where(dynamoTransactionManagerScopedRef, dynamoTransactionManagerPrototypeFactory.create()).call<Any, Throwable> {
        log.debug { "[Transaction#${dynamoTransactionManagerScopedRef.get().transactionId}] Starting transaction." }
        try {
            return@call joinPoint.proceed().also {
                dynamoTransactionManagerScopedRef.get().commit()
            }
        } catch (e: Throwable) {
            handleException(dynamoTransactionManagerScopedRef, e)
        } finally {
            // commit if exception comes from nested transaction
            if (errorOrRuntimeExceptionThrownFromNestedTransaction.get()) {
                log.debug { "[Transaction#${dynamoTransactionManagerScopedRef.get().transactionId}] " +
                        "Exception thrown from nested transaction. Committing outer transaction." }
                dynamoTransactionManagerScopedRef.get().commit()
            }
        }
    }
}