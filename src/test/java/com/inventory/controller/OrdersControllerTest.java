package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventory.config.SecurityConfig;
import com.inventory.dto.CreateOrderRequest;
import com.inventory.dto.OrderLineRequest;
import com.inventory.dto.OrderResponse;
import com.inventory.dto.PagedResponse;
import com.inventory.exception.OrderNotFoundException;
import com.inventory.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrdersController.class,
        excludeAutoConfiguration = {
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(SecurityConfig.class)
@DisplayName("OrdersController Tests")
class OrdersControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean
    OrderService orderService;
    @MockitoBean
    JwtDecoder jwtDecoder;
    @MockitoBean
    private CacheManager cacheManager;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())  // para LocalDate/LocalDateTime
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    @Test
    @WithMockUser(roles = "BUYER")
    @DisplayName("debe retornar lista paginada de órdenes")
    void shouldReturnPagedOrders() throws Exception {
        PagedResponse<OrderResponse> response = new PagedResponse<>(
                List.of(), 0, 20, 0L, 0, true, true
        );

        when(orderService.findAll(any())).thenReturn(response);

        mockMvc.perform(get("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("debe retornar 401 sin autenticación")
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "NONE")
    @DisplayName("debe retornar 403 para rol sin permiso")
    void shouldReturn403ForInvalidRole() throws Exception {
        // WAREHOUSE no tiene permiso para ver órdenes en este test
        // ajusta según tus roles reales
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    @DisplayName("debe crear orden con request válido")
    void shouldCreateOrder() throws Exception {
        UUID supplierId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                supplierId,
                "Nota de prueba",
                List.of(new OrderLineRequest(
                        productId, 10, new BigDecimal("100.00")
                ))
        );

        OrderResponse mockResponse = mock(OrderResponse.class);
        when(orderService.create(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(orderService).create(any());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    @DisplayName("debe retornar 400 con request inválido")
    void shouldReturn400WithInvalidRequest() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                null, null, List.of()  // supplierId null y lines vacío
        );

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    @DisplayName("debe aprobar orden existente")
    void shouldApproveOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse mockResponse = mock(OrderResponse.class);

        when(orderService.approve(orderId)).thenReturn(mockResponse);

        mockMvc.perform(put("/api/orders/{id}/approve", orderId)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(orderService).approve(orderId);
    }

    @Test
    @WithMockUser(roles = "BUYER")
    @DisplayName("debe retornar 404 cuando orden no existe")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(orderService.approve(orderId))
                .thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(put("/api/orders/{id}/approve", orderId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}