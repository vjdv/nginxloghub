package net.vjdv.nginxloghub.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.vjdv.nginxloghub.NginxloghubApplication;
import net.vjdv.nginxloghub.config.Config;
import net.vjdv.nginxloghub.dto.LogEntry;
import net.vjdv.nginxloghub.util.DTFormats;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ServerListener {

    private static final Pattern pattern = Pattern.compile("^(\\S+) (\\S+) - (\\S+) \\[(\\S+) \\S+] \"(\\S+) (\\S+) (\\S+)\" (\\d+) (\\d+) \"(.+)\" \"(.+)\" (\\S+) (\\S+)$");
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private final int port;
    private boolean running = true;

    public ServerListener(Config config) {
        this.port = config.getPort();
        executor.execute(this::startServer);
    }

    private void startServer() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] receiveData = new byte[1024];
            while (running) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String syslogMessage = new String(receivePacket.getData());
                //cuts message
                int index = syslogMessage.indexOf("nginx: ");
                if (index == -1) {
                    log.warn("Invalid syslog message: {}", syslogMessage);
                    continue;
                }
                String line = syslogMessage.substring(index + 7);
                //writes message to log
                log.debug("Line received: {}", line);
                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    log.warn("Invalid log line: {}", line);
                    continue;
                }
                //parse log line
                try {
                    LogEntry logEntry = getLogEntry(matcher);
                    //ignoring some resources
                    if ("POST /xapis/generar-estadisticas".equals(logEntry.resource())
                            || "axios status checker".equals(logEntry.userAgent())
                            || logEntry.userAgent().contains("Uptime-Kuma")) {
                        continue;
                    }
                    //write log line to file
                    LogWriter.append(logEntry);
                }
                catch (Exception ex) {
                    log.error("Error parsing log line", ex);
                }
            }
        }
        catch (IOException ex) {
            log.info("Error starting server", ex);
        }
    }

    private LogEntry getLogEntry(Matcher matcher) {
        String host = matcher.group(1);
        String ip = matcher.group(2);
        String user = matcher.group(3);
        String dateTimeStr = matcher.group(4);
        String method = matcher.group(5);
        String path = matcher.group(6);
        String protocol = matcher.group(7);
        String statusStr = matcher.group(8);
        String sizeStr = matcher.group(9);
        String referer = matcher.group(10);
        String userAgent = matcher.group(11);
        String upstreamTimeStr = matcher.group(12);
        String requestTimeStr = matcher.group(13);
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DTFormats.NGINX);
        int status = Integer.parseInt(statusStr);
        int size = Integer.parseInt(sizeStr);
        double upstreamTime = Double.parseDouble(upstreamTimeStr);
        double requestTime = Double.parseDouble(requestTimeStr);
        return new LogEntry(host, ip, user, dateTime, method, path, protocol, status, size, referer, userAgent, upstreamTime, requestTime);
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdown();
    }

}
