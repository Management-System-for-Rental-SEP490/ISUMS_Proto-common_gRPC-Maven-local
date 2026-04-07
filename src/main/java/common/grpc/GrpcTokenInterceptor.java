package common.grpc;

import io.grpc.*;
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

                super.start(responseListener, headers);
            }
        };
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