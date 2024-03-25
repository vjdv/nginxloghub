package net.vjdv.nginxloghub.dto;

import java.time.LocalDateTime;

public record LogEntry(
        String host,
        String ip,
        String user,
        LocalDateTime dateTime,
        String method,
        String path,
        String protocol,
        int status,
        int size,
        String referer,
        String userAgent,
        double upstreamTime,
        double requestTime
) {

    public String resource() {
        return method + " " + path;
    }

}
