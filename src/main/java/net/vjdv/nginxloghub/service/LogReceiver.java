package net.vjdv.nginxloghub.service;

import lombok.extern.slf4j.Slf4j;
import net.vjdv.nginxloghub.dto.LogEntry;
import net.vjdv.nginxloghub.util.DTFormats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class LogReceiver implements Runnable {

    private static final Pattern pattern = Pattern.compile("^(\\S+) (\\S+) - (\\S+) \\[(\\S+) \\S+] \"(\\S+) (\\S+) (\\S+)\" (\\d+) (\\d+) \"(\\S+)\" \"(\\S+)\" (\\S+) (\\S+)$");
    private final Socket clientSocket;

    public LogReceiver(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            //example line: vjdv.net 54.42.6.24 - - [23/Mar/2024:15:42:54 -0600] "GET /giteax/ HTTP/1.1" 200 13823 "-" "Uptime-Kuma/1.23.11" 0.003 0.003
            String line;
            while ((line = in.readLine()) != null) {
                log.debug("Line received: {}", line);
                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    log.warn("Invalid log line: {}", line);
                    continue;
                }
                //parse log line
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
            clientSocket.close();
        }
        catch (IOException ex) {
            log.info("Error reading log line", ex);
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
}
