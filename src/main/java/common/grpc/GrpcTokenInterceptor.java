package common.grpc;

import io.grpc.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

public class GrpcTokenInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRACEPARENT_KEY =
            Metadata.Key.of("traceparent", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRACESTATE_KEY =
            Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> BAGGAGE_KEY =
            Metadata.Key.of("baggage", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> CORRELATION_ID_KEY =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ACTOR_USER_ID_KEY =
            Metadata.Key.of("actor-user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ACTOR_ROLE_KEY =
            Metadata.Key.of("actor-role", Metadata.ASCII_STRING_MARSHALLER);

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private String getTokenUri() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    private final RestClient restClient = RestClient.create();

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth instanceof JwtAuthenticationToken jwt) {
                    headers.put(AUTHORIZATION_KEY, "Bearer " + jwt.getToken().getTokenValue());
                } else {
                    headers.put(AUTHORIZATION_KEY, "Bearer " + getServiceAccountToken());
                }

                propagateObservabilityHeaders(headers);
                super.start(responseListener, headers);
            }
        };
    }

    private void propagateObservabilityHeaders(Metadata headers) {
        putIfPresent(headers, REQUEST_ID_KEY, MDC.get("requestId"));
        putIfPresent(headers, CORRELATION_ID_KEY, MDC.get("correlationId"));
        putIfPresent(headers, ACTOR_USER_ID_KEY, MDC.get("userId"));
        putIfPresent(headers, ACTOR_ROLE_KEY, MDC.get("role"));
        putIfPresent(headers, TRACESTATE_KEY, MDC.get("tracestate"));
        putIfPresent(headers, BAGGAGE_KEY, MDC.get("baggage"));

        String traceId = MDC.get("traceId");
        String spanId = MDC.get("spanId");
        if (traceId != null && !traceId.isBlank() && spanId != null && !spanId.isBlank()
                && headers.get(TRACEPARENT_KEY) == null) {
            headers.put(TRACEPARENT_KEY, "00-" + traceId + "-" + spanId + "-01");
        }
    }

    private void putIfPresent(Metadata headers, Metadata.Key<String> key, String value) {
        if (value != null && !value.isBlank() && headers.get(key) == null) {
            headers.put(key, value);
        }
    }

    private synchronized String getServiceAccountToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        Map<?, ?> response = restClient.post()
                .uri(getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials&client_id=" + clientId
                        + "&client_secret=" + clientSecret)
                .retrieve()
                .body(Map.class);

        assert response != null;
        cachedToken = (String) response.get("access_token");
        int expiresIn = (Integer) response.get("expires_in");
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 30);

        return cachedToken;
    }
}
