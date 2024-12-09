package hello

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.data.redis.core.setAndAwait
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToFlow
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers

@Configuration
@Testcontainers
class RedisTestConfiguration {
    @Bean
    @ServiceConnection(name = "redis")
    fun valkey(): GenericContainer<*> = GenericContainer("valkey/valkey:8.0.1").withExposedPorts(6379)
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Tests {

    @Test
    fun `redis operations test`(@Autowired redisTemplate: ReactiveStringRedisTemplate) = runBlocking {
        redisTemplate.opsForValue().setAndAwait("test-key", "test-value")
        val value = redisTemplate.opsForValue().getAndAwait("test-key")
        assertEquals("test-value", value)
    }

    @Test
    fun `should post and get bars`(@LocalServerPort port: Int) = runBlocking {
        val client = WebClient.create("http://localhost:$port")

        val testBars = listOf("test foo", "test bar", "test baz")

        testBars.forEach {
            client.post()
                .uri("/bars")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(it)
                .awaitExchange { response ->
                    assertEquals(HttpStatus.OK, response.statusCode())
                }
        }

        val result = client.get()
            .uri("/bars")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .awaitExchange { response ->
                assertEquals(HttpStatus.OK, response.statusCode())
                response.bodyToFlow<String>().toList()
            }

        assertEquals(result, testBars)
    }

}
