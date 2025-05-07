package pl.krzysztofowczarek

import org.springframework.stereotype.Service

@Service
class RepositoryMethodInvoker(private val testRepository: TestRepository) {

    @DynamoWriteTransaction
    fun saveAndCommit(entity: TestEntity) {
        testRepository.save(entity)
    }

    @DynamoWriteTransaction
    fun deleteAndCommit(entity: TestEntity) {
        testRepository.delete(entity)
    }
}