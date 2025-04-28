package pl.krzysztofowczarek

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@EnableAspectJAutoProxy
@SpringBootApplication
@EnableConfigurationProperties(AwsProperties::class)
class Application {
    fun main(args: Array<String>) {
        runApplication<Application>(*args)
    }
}