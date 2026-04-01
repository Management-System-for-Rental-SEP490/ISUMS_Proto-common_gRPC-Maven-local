package common.paginations.configurations;

import java.time.Duration;
import java.util.Map;

public interface IsumCacheConfigurer {
    Map<String, Duration> cacheTtls();
}
