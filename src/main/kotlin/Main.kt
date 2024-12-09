package hello

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.rangeAsFlow
import org.springframework.data.redis.core.rightPushAndAwait
import org.springframework.web.reactive.function.server.*


@SpringBootApplication
class Application {

    @Bean
    fun http(@Autowired redisTemplate: ReactiveStringRedisTemplate) = coRouter {
        GET("/bars") {
            val bars = redisTemplate.opsForList().rangeAsFlow("bars", 0, -1)
            ServerResponse.ok().sse().bodyAndAwait(bars)
        }
        POST("/bars") { request ->
            val bar = request.awaitBody<String>()
            redisTemplate.opsForList().rightPushAndAwait("bars", bar)
            ServerResponse.ok().buildAndAwait()
        }
    }

}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
