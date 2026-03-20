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
    }

    public void clearMDC() {
        MDC.clear();
    }
}