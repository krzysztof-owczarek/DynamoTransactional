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

import org.springframework.stereotype.Service

@Service
class RepositoryMethodInvoker(
    private val testRepository: TestRepository,
    private val nestedRepositoryMethodInvoker: NestedRepositoryMethodInvoker
) {

    @DynamoWriteTransaction
    fun saveAndCommit(entity: TestEntity) {
        testRepository.save(entity)
    }

    @DynamoWriteTransaction
    fun deleteAndCommit(entity: TestEntity) {
        testRepository.delete(entity)
    }

    @DynamoWriteTransaction
    fun nestedSaveAndCommitPropagationRequired(entity1: TestEntity, entity2: TestEntity) {
        testRepository.save(entity1)
        nestedRepositoryMethodInvoker.saveAndCommit(entity2)
    }

    @DynamoWriteTransaction
    fun nestedSaveAndCommitPropagationRequiresNew(entity1: TestEntity, entity2: TestEntity) {
        testRepository.save(entity1)
        nestedRepositoryMethodInvoker.saveAndCommitRequiresNew(entity2)
    }
}