package common.grpc;

import com.isums.assetservice.grpc.AssetServiceGrpc;
import com.isums.houseservice.grpc.HouseServiceGrpc;
import com.isums.houseservice.grpc.TenantServiceGrpc;
import com.isums.issueservice.grpc.IssueServiceGrpc;
import com.isums.paymentservice.grpc.PaymentServiceGrpc;
import com.isums.userservice.grpc.UserServiceGrpc;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.GrpcChannelFactory;

@AutoConfiguration
@ConditionalOnClass(GrpcChannelFactory.class)
public class IssumsGrpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GrpcTokenInterceptor grpcTokenInterceptor() {
        return new GrpcTokenInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("spring.grpc.client.channels.house.address")
    public HouseServiceGrpc.HouseServiceBlockingStub houseStub(
            GrpcChannelFactory channels, GrpcTokenInterceptor tokenInterceptor) {
        return HouseServiceGrpc.newBlockingStub(channels.createChannel("house"))
                .withInterceptors(tokenInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("spring.grpc.client.channels.house.address")
    public TenantServiceGrpc.TenantServiceBlockingStub tenantStub(
            GrpcChannelFactory channels, GrpcTokenInterceptor tokenInterceptor) {
        return TenantServiceGrpc.newBlockingStub(channels.createChannel("house"))
                .withInterceptors(tokenInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("spring.grpc.client.channels.asset.address")
    public AssetServiceGrpc.AssetServiceBlockingStub assetStub(
            GrpcChannelFactory channels, GrpcTokenInterceptor tokenInterceptor) {
        return AssetServiceGrpc.newBlockingStub(channels.createChannel("asset"))
                .withInterceptors(tokenInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("spring.grpc.client.channels.user.address")
    public UserServiceGrpc.UserServiceBlockingStub userStub(
            GrpcChannelFactory channels, GrpcTokenInterceptor tokenInterceptor) {
        return UserServiceGrpc.newBlockingStub(channels.createChannel("user"))
                .withInterceptors(tokenInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("spring.grpc.client.channels.payment.address")
    public PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub(
            GrpcChannelFactory channels, GrpcTokenInterceptor tokenInterceptor) {
        return PaymentServiceGrpc.newBlockingStub(channels.createChannel("payment"))
                .withInterceptors(tokenInterceptor);
    }

//    // ── contract-service ──────────────────────────────────────────────────
//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty("spring.grpc.client.channels.contract.address")
//    public ContractServiceGrpc.ContractServiceBlockingStub contractStub(
//            GrpcChannelFactory channels, GrpcTokenInterceptor tokenInterceptor) {
//        return ContractServiceGrpc.newBlockingStub(channels.createChannel("contract"))
//                .withInterceptors(tokenInterceptor);
//    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("spring.grpc.client.channels.issue.address")
    public IssueServiceGrpc.IssueServiceBlockingStub issueStub(
            GrpcChannelFactory channels, GrpcTokenInterceptor tokenInterceptor) {
        return IssueServiceGrpc.newBlockingStub(channels.createChannel("issue"))
                .withInterceptors(tokenInterceptor);
    }
}