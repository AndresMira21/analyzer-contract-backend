package com.acl.backend.config;

import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    @Primary
    public MongoClient mongoClient() {
        try {
            log.info("Configurando MongoDB client con SSL personalizado");
            
            // Crear TrustManager que acepta todos los certificados
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Acepta todos los certificados de cliente
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Acepta todos los certificados de servidor
                    }
                }
            };

            // Inicializar SSLContext con el TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Construir configuración de MongoDB con SSL
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(mongoUri))
                    .applyToSslSettings(builder -> {
                        builder.enabled(true)
                               .invalidHostNameAllowed(true)
                               .context(sslContext);
                    })
                    .applyToSocketSettings(builder -> {
                        builder.connectTimeout(15, TimeUnit.SECONDS)
                               .readTimeout(30, TimeUnit.SECONDS);
                    })
                    .applyToClusterSettings(builder -> {
                        builder.serverSelectionTimeout(15, TimeUnit.SECONDS);
                    })
                    .applyToConnectionPoolSettings(builder -> {
                        builder.maxSize(10)
                               .minSize(2)
                               .maxWaitTime(10, TimeUnit.SECONDS)
                               .maxConnectionIdleTime(60, TimeUnit.SECONDS);
                    })
                    .build();

            MongoClient client = MongoClients.create(settings);
            
            log.info("✓ MongoDB client configurado exitosamente");
            return client;

        } catch (Exception e) {
            log.error("✗ Error configurando MongoDB client: {}", e.getMessage(), e);
            throw new RuntimeException("Error al configurar MongoDB", e);
        }
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        // Extraer nombre de base de datos del URI
        String dbName = extractDatabaseName(mongoUri);
        log.info("Usando base de datos: {}", dbName);
        return new SimpleMongoClientDatabaseFactory(mongoClient, dbName);
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    private String extractDatabaseName(String uri) {
        try {
            // Extraer nombre de DB del URI
            // Formato: mongodb+srv://user:pass@host/DATABASE?params
            int start = uri.lastIndexOf('/') + 1;
            int end = uri.indexOf('?', start);
            
            if (end == -1) {
                return uri.substring(start);
            }
            return uri.substring(start, end);
        } catch (Exception e) {
            log.warn("No se pudo extraer nombre de DB del URI, usando 'analizador_contratos'");
            return "analizador_contratos";
        }
    }
}