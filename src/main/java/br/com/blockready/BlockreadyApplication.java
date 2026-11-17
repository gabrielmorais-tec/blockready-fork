package br.com.blockready;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do BLOCKREADY.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BlockreadyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlockreadyApplication.class, args);
    }
}
