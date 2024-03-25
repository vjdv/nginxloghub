package net.vjdv.nginxloghub.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.vjdv.nginxloghub.config.Config;
import net.vjdv.nginxloghub.dto.LogEntry;
import net.vjdv.nginxloghub.util.DTFormats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

@Slf4j
public class LogWriter {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final Map<LocalDate, LogWriter> logs = new HashMap<>();
    private final BufferedWriter writer;
    private final LocalDate date;
    @Getter
    private boolean closed = false;
    @Getter
    private boolean zipped = false;

    static {
        executor.scheduleAtFixedRate(LogWriter::executeTasks, 5, 1, TimeUnit.MINUTES);
    }

    private LogWriter(LocalDate date) throws IOException {
        this.date = date;
        Path logPath = Config.getInstance().getLogPath().resolve("nginx-" + date.format(DTFormats.SHORT_DAY) + ".log");
        writer = new BufferedWriter(new FileWriter(logPath.toFile(), true));
        log.info("Log file created: {}", logPath);
    }

    public void write(String line) {
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        }
        catch (IOException e) {
            log.error("Error writing log line", e);
        }
    }

    public void close() {
        try {
            writer.close();
            closed = true;
        }
        catch (IOException ex) {
            log.error("Error closing log file", ex);
        }
    }

    public void zip() {
        try {
            File logFile = Config.getInstance().getLogPath().resolve("nginx-" + date.format(DTFormats.SHORT_DAY) + ".log").toFile();
            File zipFile = Config.getInstance().getLogPath().resolve("nginx-" + date.format(DTFormats.SHORT_DAY) + ".zip").toFile();
            // Create a buffer for reading the files
            byte[] buf = new byte[1024];
            // Create the ZIP file
            try (var zipOut = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
                zipOut.setLevel(Deflater.BEST_COMPRESSION);
                // Compress the files
                try (var in = new FileInputStream(logFile)) {
                    // Add ZIP entry to output stream.
                    zipOut.putNextEntry(new java.util.zip.ZipEntry(logFile.getName()));
                    // Transfer bytes from the file to the ZIP file
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        zipOut.write(buf, 0, len);
                    }
                    // Complete the entry
                    zipOut.closeEntry();
                }
            }
            zipped = true;
        }
        catch (IOException ex) {
            log.error("Error zipping log file", ex);
        }
    }

    public static void append(LogEntry entry) {
        executor.execute(() -> {
            var date = entry.dateTime().toLocalDate();
            if (!logs.containsKey(date)) {
                try {
                    LogWriter logWriter = new LogWriter(date);
                    logs.put(date, logWriter);
                }
                catch (IOException e) {
                    log.error("Error creating log file", e);
                }
            }
            LogWriter logWriter = logs.get(date);
            String line = String.format("%s %s - %s [%s] \"%s %s %s\" %d %d \"%s\" \"%s\" %.3f %.3f",
                    entry.host(), entry.ip(), entry.user(), entry.dateTime().format(DTFormats.STANDARD), entry.method(), entry.path(), entry.protocol(),
                    entry.status(), entry.size(), entry.referer(), entry.userAgent(), entry.upstreamTime(), entry.requestTime());
            logWriter.write(line);
        });
    }

    private static void executeTasks() {
        var now = LocalDateTime.now();
        //zips and process logs at midnight
        if (now.getHour() == 0 && (now.getMinute() > 6 && now.getMinute() <= 10)) {
            //zips logs
            logs.values().stream().filter(o -> o.isClosed() && !o.isZipped()).forEach(LogWriter::zip);
            //close logs
            logs.values().stream().filter(o -> !o.isClosed() && !o.isZipped()).forEach(LogWriter::close);
        }
    }

}
