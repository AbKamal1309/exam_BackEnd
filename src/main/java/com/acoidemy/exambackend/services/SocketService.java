package com.acoidemy.exambackend.services;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocketService {

    private final SocketIOServer socketServer;

    public void sendJoinRequestNotification(Long groupId, String groupName, String userName) {
        NotificationDTO notification = new NotificationDTO(groupId, groupName, userName);
        socketServer.getBroadcastOperations().sendEvent("new_join_request", notification);
        log.info("Notification envoyée: nouvelle demande pour le groupe {}", groupName);
    }

    public void sendRequestAcceptedNotification(Long userId, String groupName) {
        NotificationDTO notification = new NotificationDTO(userId, groupName, null);
        socketServer.getBroadcastOperations().sendEvent("request_accepted", notification);
        log.info("Notification envoyée: demande acceptée pour l'utilisateur {}", userId);
    }

    // DTO pour les notifications
    @Getter
    public static class NotificationDTO implements Serializable {
        private final Long id;
        private final String groupName;
        private final String userName;

        public NotificationDTO(Long id, String groupName, String userName) {
            this.id = id;
            this.groupName = groupName;
            this.userName = userName;
        }

    }
}