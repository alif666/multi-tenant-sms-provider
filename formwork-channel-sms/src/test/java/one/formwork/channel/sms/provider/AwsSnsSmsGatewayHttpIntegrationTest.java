package one.formwork.channel.sms.provider;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import one.formwork.channel.sms.api.SmsChannelProperties;
import one.formwork.channel.sms.api.SmsMessage;
import one.formwork.channel.sms.api.SmsResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class AwsSnsSmsGatewayHttpIntegrationTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void send_UsesSignedHttpGetAndEncodedQueryString() throws Exception {
        AtomicReference<HttpExchange> seen = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> handle(exchange, seen));
        server.start();

        SmsChannelProperties.AwsSnsProperties props = new SmsChannelProperties.AwsSnsProperties();
        props.setRegion("us-east-1");
        AwsSnsSmsGateway gateway = new AwsSnsSmsGateway(
                props,
                WebClient.builder().build(),
                "AKIDEXAMPLE",
                "secretKeyExample",
                "http://127.0.0.1:" + server.getAddress().getPort());

        SmsResult result = gateway.send(new SmsMessage("+4915112345678", "Hello world", UUID.randomUUID()));

        assertTrue(result.isSuccess());
        HttpExchange exchange = seen.get();
        assertNotNull(exchange);
        assertEquals("GET", exchange.getRequestMethod());
        assertTrue(exchange.getRequestURI().getRawQuery().contains("Message=Hello%20world"));
        assertEquals("127.0.0.1:" + server.getAddress().getPort(), exchange.getRequestHeaders().getFirst("Host"));
        assertNotNull(exchange.getRequestHeaders().getFirst("Authorization"));
        assertNotNull(exchange.getRequestHeaders().getFirst("x-amz-date"));
    }

    private static void handle(HttpExchange exchange, AtomicReference<HttpExchange> seen) throws IOException {
        seen.set(exchange);
        byte[] body = "<PublishResponse><PublishResult><MessageId>msg-123</MessageId></PublishResult></PublishResponse>"
                .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
