package hello

import glide.api.GlideClient
import glide.api.models.configuration.GlideClientConfiguration
import glide.api.models.configuration.NodeAddress
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToFlow
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers


@Configuration
@Testcontainers
class GlideTestConfiguration {
    @Bean
    fun valkey(): GenericContainer<*> = GenericContainer("valkey/valkey:8.0.1").withExposedPorts(6379)

    @Bean
    fun glideClient(@Autowired valkey: GenericContainer<*>): GlideClient {
        val address = NodeAddress.builder()
            .host(valkey.host)
            .port(valkey.firstMappedPort)
            .build()

        val config = GlideClientConfiguration.builder()
            .address(address)
            .build()

        // todo: can config be async?
        return GlideClient.createClient(config).get()
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Tests {

    @Test
    fun `basic operations test`(@Autowired glideClient: GlideClient) = runBlocking {
        glideClient.set("test-key", "test-value").await()
        val value = glideClient.get("test-key").await()
        assertEquals("test-value", value)
    }

    @Test
    fun `should post and get bars`(@LocalServerPort port: Int) = runBlocking {
        val client = WebClient.create("http://localhost:$port")

        val testBars = listOf("test foo", "test bar", "test baz")

        testBars.forEach {
            client.post()
                .uri("/bars")
                .contentType(MediaType.TEXT_PLAIN)
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
