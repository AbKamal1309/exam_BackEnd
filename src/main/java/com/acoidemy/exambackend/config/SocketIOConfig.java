package com.acoidemy.exambackend.config;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SocketIOConfig {

    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(8087);
        config.setOrigin("http://localhost:4200");

        SocketIOServer server = new SocketIOServer(config);
        server.start();
        return server;
    }

}
