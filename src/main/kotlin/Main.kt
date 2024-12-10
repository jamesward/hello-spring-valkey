package hello

import glide.api.GlideClient
import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.future.await
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.*


@SpringBootApplication
class Application {

    @Bean
    fun http(@Autowired glideClient: GlideClient) = coRouter {
        GET("/bars") {
            val bars = glideClient.lrange("bars", 0, -1).await()
            ServerResponse.ok().sse().bodyAndAwait(bars.asFlow())
        }
        POST("/bars") { request ->
            val bar = request.awaitBody<String>()
            glideClient.rpush("bars", arrayOf(bar)).await()
            ServerResponse.ok().buildAndAwait()
        }
    }

}

@Configuration
class GlideConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "glide", name = ["host", "port"])
    fun glideClientFromConfig(
        @Value("\${glide.host}") host: String,
        @Value("\${glide.port}") port: Int
    ): GlideClient {
        val address = NodeAddress.builder()
            .host(host)
            .port(port)
            .build()

        val config = GlideClientConfiguration.builder()
            .address(address)
            .build()

        // todo: can config be async?
        return GlideClient.createClient(config).get()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
