package com.bookverse;

import com.bookverse.config.DotenvLoader;
import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookVerseApplication {

    public static void main(String[] args) throws IOException {
        DotenvLoader.load();
        SpringApplication.run(BookVerseApplication.class, args);
    }
}

