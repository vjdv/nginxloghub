package net.vjdv.nginxloghub.service;

import lombok.extern.slf4j.Slf4j;
import net.vjdv.nginxloghub.NginxloghubApplication;
import net.vjdv.nginxloghub.config.Config;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ServerListener {

    private final Executor executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private final int port;

    public ServerListener(Config config) {
        this.port = config.getPort();
        executor.execute(this::startServer);
    }

    private void startServer() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] receiveData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String syslogMessage = new String(receivePacket.getData());
                // Escribir el mensaje syslog en un archivo
                log.info("Message received: {}", syslogMessage);
            }
        }
        catch (IOException ex) {
            log.info("Error starting server", ex);
        }
    }

}
