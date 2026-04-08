package common.paginations.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "isum.cache")
public class IsumCacheProperties {

    private Duration defaultTtl = Duration.ofHours(23);
    private Map<String, Duration> namespaces = new HashMap<>();

    public Duration getTtlFor(String namespace) {
        return namespaces.getOrDefault(namespace, defaultTtl);
    }
}