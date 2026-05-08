package com.inventory.config;

import com.inventory.grpc.generated.NotificationServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Value("${grpc.internal.secret:internal-secret}")
    private String internalSecret;

    @Bean
    public NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceStub(
            GrpcChannelFactory channelFactory) {

        Channel channel = channelFactory.createChannel("notification-service");

        ClientInterceptor internalAuthInterceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method,
                    CallOptions callOptions,
                    Channel next) {

                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {

                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        headers.put(
                            Metadata.Key.of("x-internal-secret", Metadata.ASCII_STRING_MARSHALLER),
                            internalSecret
                        );
                        super.start(responseListener, headers);
                    }
                };
            }
        };

        Channel interceptedChannel = ClientInterceptors.intercept(channel, internalAuthInterceptor);
        return NotificationServiceGrpc.newBlockingStub(interceptedChannel);
    }

}
