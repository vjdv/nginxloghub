package net.vjdv.nginxloghub.service;

import lombok.extern.slf4j.Slf4j;
import net.vjdv.nginxloghub.NginxloghubApplication;
import net.vjdv.nginxloghub.config.Config;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ServerListener {

    private final Executor executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    public ServerListener() {
        executor.execute(this::startServer);
    }

    private void startServer() {
        int port = Config.getInstance().getPort();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Nginx log server started on port {}", port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.info("New connection from {}", clientSocket.getInetAddress().getHostAddress());
                LogReceiver logReceiver = new LogReceiver(clientSocket);
                executor.execute(logReceiver);
            }
        }
        catch (IOException ex) {
            log.info("Error starting server", ex);
        }
    }

}
