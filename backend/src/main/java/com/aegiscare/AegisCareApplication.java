package com.aegiscare;

import com.aegiscare.config.StorageDataSourceInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class AegisCareApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AegisCareApplication.class)
                .initializers(new StorageDataSourceInitializer())
                .run(args);
    }
}
