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