package com.inventory.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class HazelcastConfig {

    public static final String CACHE_PRODUCTS    = "products";
    public static final String CACHE_CATEGORIES  = "categories";
    public static final String CACHE_SUPPLIERS   = "suppliers";
    public static final String CACHE_REPORTS     = "reports";

    @Value("${hazelcast.client.cluster-name:inventory-cluster}")
    private String clusterName;

    @Value("${hazelcast.client.network.address:hazelcast-inventory:5701}")
    private String hazelcastAddress;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        ClientConfig config = new ClientConfig();
        config.setClusterName(clusterName);
        config.getNetworkConfig().addAddress(hazelcastAddress);

        // Desactivamos la serialización Compact para que use Serializable de Java
        config.getSerializationConfig().setEnableCompression(false);

        // TTL por cache
        config.addNearCacheConfig(
                new com.hazelcast.config.NearCacheConfig(CACHE_PRODUCTS)
                        .setTimeToLiveSeconds(300) // 5 minutos
        );
        config.addNearCacheConfig(
                new com.hazelcast.config.NearCacheConfig(CACHE_CATEGORIES)
                        .setTimeToLiveSeconds(3600)   // 1 hora
        );
        config.addNearCacheConfig(
                new com.hazelcast.config.NearCacheConfig(CACHE_SUPPLIERS)
                        .setTimeToLiveSeconds(600)    // 10 minutos
        );
        config.addNearCacheConfig(
                new com.hazelcast.config.NearCacheConfig(CACHE_REPORTS)
                        .setTimeToLiveSeconds(120)    // 2 minutos
        );

        return HazelcastClient.newHazelcastClient(config);
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        return new HazelcastCacheManager(hazelcastInstance);
    }
}
