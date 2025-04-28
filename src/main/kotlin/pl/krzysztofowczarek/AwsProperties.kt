package pl.krzysztofowczarek

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
    val endpoint: String? = null,
    val region: String? = null
)
