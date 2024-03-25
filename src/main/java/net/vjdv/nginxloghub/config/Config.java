package net.vjdv.nginxloghub.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Configuration
public class Config {

    private static final AtomicReference<Config> instance = new AtomicReference<>();
    private final int port;
    private final Path logPath;

    public Config(Environment env) {
        port = Integer.parseInt(env.getProperty("server.port", "8050"));
        logPath = Path.of(env.getProperty("log.path", "."));
    }

    @PostConstruct
    public void init() {
        if (instance.get() != null) {
            throw new IllegalStateException("Config already initialized");
        }
        instance.set(this);
    }

    public static Config getInstance() {
        return instance.get();
    }

}
