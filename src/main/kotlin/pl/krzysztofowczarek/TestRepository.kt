package pl.krzysztofowczarek

import org.springframework.stereotype.Repository

@Repository
class TestRepository(private val dynamoDbDao: DynamoDbDao<TestEntity>) {

    fun save(entity: TestEntity) {
        dynamoDbDao.save(entity)
    }

    fun delete(entity: TestEntity) {
        dynamoDbDao.delete(entity)
    }
}