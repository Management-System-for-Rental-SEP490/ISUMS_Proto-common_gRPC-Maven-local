package common.kafkas;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class KafkaListenerHelper {

    public String extractMessageId(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader("messageId");
        if (header != null) return new String(header.value(), StandardCharsets.UTF_8);
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }

    public void setupMDC(ConsumerRecord<String, String> record, String messageId) {
        MDC.put("messageId", messageId);
        MDC.put("topic",     record.topic());
        MDC.put("partition", String.valueOf(record.partition()));
        MDC.put("offset",    String.valueOf(record.offset()));
        putHeader(record, "x-request-id", "requestId");
        putHeader(record, "x-correlation-id", "correlationId");
        putHeader(record, "actor-user-id", "userId");
        putHeader(record, "actor-role", "role");
        putTraceparent(record);
    }

    public void clearMDC() {
        MDC.clear();
    }

    private void putHeader(ConsumerRecord<String, String> record, String headerName, String mdcName) {
        Header header = record.headers().lastHeader(headerName);
        if (header != null) {
            MDC.put(mdcName, new String(header.value(), StandardCharsets.UTF_8));
        }
    }

    private void putTraceparent(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader("traceparent");
        if (header == null) {
            return;
        }
        String value = new String(header.value(), StandardCharsets.UTF_8);
        String[] parts = value.split("-");
        if (parts.length >= 4) {
            MDC.put("traceId", parts[1]);
            MDC.put("spanId", parts[2]);
        }
    }
}
