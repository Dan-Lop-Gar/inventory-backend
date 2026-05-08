package com.inventory.config;

import com.inventory.grpc.interceptor.JwtGrpcInterceptor;
import io.grpc.netty.NettyServerBuilder;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Slf4j
@Configuration
public class GrpcServerConfig {

    @Value("${spring.grpc.server.port:9900}")
    private int grpcPort;

    @Bean
    @Primary
    public NettyGrpcServerFactory nettyGrpcServerFactory(
            GrpcServiceDiscoverer serviceDiscoverer,
            GrpcServiceConfigurer serviceConfigurer,
            List<ServerBuilderCustomizer<NettyServerBuilder>> customizers) {

        String address = "*:" + grpcPort;
        log.info("Registrando NettyGrpcServerFactory con address: {}", address);

        NettyGrpcServerFactory factory = new NettyGrpcServerFactory(address, customizers, null, null, null);

        serviceDiscoverer.findServices().stream()
                .map(serviceSpec -> serviceConfigurer.configure(serviceSpec, factory))
                .forEach(factory::addService);

        return factory;
    }

    @Bean(name = "nettyGrpcServerLifecycle")
    public GrpcServerLifecycle nettyGrpcServerLifecycle(
            NettyGrpcServerFactory factory,
            GrpcServerProperties properties,
            ApplicationEventPublisher eventPublisher) {
        log.info("Creando GrpcServerLifecycle");
        return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
    }

    @Bean
    public ServerBuilderCustomizer<NettyServerBuilder> serverBuilderCustomizer() {
        return builder -> {
            builder.executor(Executors.newVirtualThreadPerTaskExecutor());
            builder.maxInboundMessageSize(10 * 1024 * 1024);
            builder.maxInboundMetadataSize(64 * 1024);
            log.info("gRPC builder customizado");
        };
    }

    @Bean
    @Primary
    public GrpcSecurity grpcSecurityEnablement(GrpcSecurity grpc) throws Exception {
        return grpc.authorizeRequests(auth -> auth.allRequests().permitAll());
    }

    @Bean
    @GlobalServerInterceptor
    public JwtGrpcInterceptor jwtGrpcInterceptor(JwtDecoder jwtDecoder) {
        return new JwtGrpcInterceptor(jwtDecoder);
    }
}