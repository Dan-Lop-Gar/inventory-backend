package com.inventory.grpc.interceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class JwtGrpcInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTH_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> INTERNAL_SECRET_KEY =
            Metadata.Key.of("x-internal-secret", Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<String> USER_KEY = Context.key("userId");

    private static final Set<String> PUBLIC_METHODS = Set.of(
            "grpc.reflection.v1.ServerReflection/ServerReflectionInfo",
            "grpc.reflection.v1alpha.ServerReflection/ServerReflectionInfo",
            "grpc.health.v1.Health/Check",
            "grpc.health.v1.Health/Watch"
    );

    @Value("${grpc.internal.secret:internal-secret}")
    private String internalSecret;

    private final JwtDecoder jwtDecoder;

    public JwtGrpcInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();

        if (PUBLIC_METHODS.contains(methodName)) {
            log.debug("Método público, sin autenticación: {}", methodName);
            return next.startCall(call, headers);
        }

        String providedSecret = headers.get(INTERNAL_SECRET_KEY);

        if (internalSecret.equals(providedSecret)) {
            log.debug("Llamada interna permitida: {}", methodName);
            return next.startCall(call, headers);
        }

        log.debug("Llamada externa, requiere JWT: {}", methodName);

        String authHeader = headers.get(AUTH_KEY);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("JWT requerido para acceso externo"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        try {
            String token = authHeader.substring(7);
            var jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject();

            Context ctx = Context.current().withValue(USER_KEY, userId);
            return Contexts.interceptCall(ctx, call, headers, next);

        } catch (JwtException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Token inválido"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
    }
}