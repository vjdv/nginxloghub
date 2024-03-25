package net.vjdv.nginxloghub.util;

import java.time.format.DateTimeFormatter;

public interface DTFormats {
    DateTimeFormatter NGINX = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss");
    DateTimeFormatter STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("yyMMdd");
}
