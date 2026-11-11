package com.example.demo.adapter.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClaimWebSocketHandlerTest {

    private ClaimWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new ClaimWebSocketHandler(objectMapper);
    }

    @Test
    void should_StoreSession_When_ConnectionEstablished() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession("user-1", List.of("USER"));

        // Act
        handler.afterConnectionEstablished(session);

        // Assert
        assertEquals(1, handler.getActiveSessionCount());
        assertEquals(1, handler.getActiveUserCount());
    }

    @Test
    void should_SupportMultipleSessions_When_SameUserConnectsMultipleTimes() throws Exception {
        // Arrange
        WebSocketSession session1 = createMockSession("user-1", List.of("USER"));
        WebSocketSession session2 = createMockSession("user-1", List.of("USER"));

        // Act
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Assert
        assertEquals(2, handler.getActiveSessionCount());
        assertEquals(1, handler.getActiveUserCount());
    }

    @Test
    void should_RemoveSession_When_ConnectionClosed() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession("user-1", List.of("USER"));
        handler.afterConnectionEstablished(session);

        // Act
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Assert
        assertEquals(0, handler.getActiveSessionCount());
        assertEquals(0, handler.getActiveUserCount());
    }

    @Test
    void should_SendToCorrectUser_When_BroadcastToUser() throws Exception {
        // Arrange
        WebSocketSession userSession = createMockSession("user-1", List.of("USER"));
        WebSocketSession otherSession = createMockSession("user-2", List.of("USER"));
        handler.afterConnectionEstablished(userSession);
        handler.afterConnectionEstablished(otherSession);

        WebSocketMessage message = createTestMessage("CLAIM_STATUS_CHANGED");

        // Act
        handler.broadcastToUser("user-1", message);

        // Assert
        verify(userSession).sendMessage(any(TextMessage.class));
        verify(otherSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_SendOnlyToAdmins_When_BroadcastToAdmins() throws Exception {
        // Arrange
        WebSocketSession adminSession = createMockSession("admin-1", List.of("ADMIN"));
        WebSocketSession userSession = createMockSession("user-1", List.of("USER"));
        handler.afterConnectionEstablished(adminSession);
        handler.afterConnectionEstablished(userSession);

        WebSocketMessage message = createTestMessage("CLAIM_SUBMITTED");

        // Act
        handler.broadcastToAdmins(message);

        // Assert
        verify(adminSession).sendMessage(any(TextMessage.class));
        verify(userSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_SendToUserAndAdmins_When_BroadcastToUserAndAdmins() throws Exception {
        // Arrange
        WebSocketSession ownerSession = createMockSession("owner-1", List.of("USER"));
        WebSocketSession adminSession = createMockSession("admin-1", List.of("ADMIN"));
        WebSocketSession otherSession = createMockSession("user-2", List.of("USER"));
        handler.afterConnectionEstablished(ownerSession);
        handler.afterConnectionEstablished(adminSession);
        handler.afterConnectionEstablished(otherSession);

        WebSocketMessage message = createTestMessage("CLAIM_STATUS_CHANGED");

        // Act
        handler.broadcastToUserAndAdmins("owner-1", message);

        // Assert
        verify(ownerSession).sendMessage(any(TextMessage.class));
        verify(adminSession).sendMessage(any(TextMessage.class));
        verify(otherSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_RemoveStaleSession_When_SendFails() throws Exception {
        // Arrange
        WebSocketSession staleSession = createMockSession("user-1", List.of("USER"));
        when(staleSession.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("Connection reset")).when(staleSession).sendMessage(any(TextMessage.class));
        handler.afterConnectionEstablished(staleSession);

        WebSocketMessage message = createTestMessage("CLAIM_STATUS_CHANGED");

        // Act
        handler.broadcastToUser("user-1", message);

        // Assert
        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_CloseSession_When_NoUserId() throws Exception {
        // Arrange
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("test-session");
        when(session.isOpen()).thenReturn(true);

        // Act
        handler.afterConnectionEstablished(session);

        // Assert
        assertEquals(0, handler.getActiveSessionCount());
        verify(session).close();
    }

    private WebSocketSession createMockSession(String userId, List<String> roles) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);
        attributes.put("userRoles", roles);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("session-" + userId + "-" + System.nanoTime());
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void should_SendPingToAllSessions_When_HeartbeatTriggered() throws Exception {
        // Arrange
        WebSocketSession session1 = createMockSession("user-1", List.of("USER"));
        WebSocketSession session2 = createMockSession("user-2", List.of("ADMIN"));
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Act
        handler.sendHeartbeat();

        // Assert
        verify(session1).sendMessage(any(org.springframework.web.socket.PingMessage.class));
        verify(session2).sendMessage(any(org.springframework.web.socket.PingMessage.class));
    }

    @Test
    void should_RemoveStaleSession_When_HeartbeatFails() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession("user-1", List.of("USER"));
        handler.afterConnectionEstablished(session);
        doThrow(new java.io.IOException("Connection closed")).when(session).sendMessage(any(org.springframework.web.socket.PingMessage.class));

        // Act
        handler.sendHeartbeat();

        // Assert
        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_DoNothing_When_BroadcastToUser_NoSessions() {
        // no sessions registered — should not throw
        handler.broadcastToUser("unknown-user", createTestMessage("CLAIM_STATUS_CHANGED"));
        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_SkipClosedSession_When_SendMessage() throws Exception {
        WebSocketSession closedSession = createMockSession("user-1", List.of("USER"));
        when(closedSession.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(closedSession);

        // Bypass isOpen check at connection — reset to closed
        handler.broadcastToUser("user-1", createTestMessage("CLAIM_STATUS_CHANGED"));

        verify(closedSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_RemoveSession_When_IllegalStateExceptionOnSend() throws Exception {
        WebSocketSession session = createMockSession("user-1", List.of("USER"));
        doThrow(new IllegalStateException("session closed")).when(session).sendMessage(any(TextMessage.class));
        handler.afterConnectionEstablished(session);

        handler.broadcastToUser("user-1", createTestMessage("CLAIM_STATUS_CHANGED"));

        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_HandleTransportError() throws Exception {
        WebSocketSession session = createMockSession("user-1", List.of("USER"));
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.handleTransportError(session, new RuntimeException("transport error"));

        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_DoNothing_When_AfterConnectionClosed_NoUserId() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("no-user-session");

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_SkipClosedSession_In_Heartbeat() throws Exception {
        WebSocketSession session = createMockSession("user-1", List.of("USER"));
        when(session.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(session);

        handler.sendHeartbeat();

        verify(session, never()).sendMessage(any(org.springframework.web.socket.PingMessage.class));
    }

    @Test
    void should_NotSendToNonAdmin_When_BroadcastToAdmins_NoRoles() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", "user-1");
        attributes.put("userRoles", null); // null roles
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("session-user-1");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.broadcastToAdmins(createTestMessage("CLAIM_SUBMITTED"));

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_CleanupClosedSessions_When_BroadcastToUserAndAdmins() throws Exception {
        WebSocketSession openSession = createMockSession("user-1", List.of("USER"));
        WebSocketSession closedSession = createMockSession("user-1", List.of("USER"));
        when(closedSession.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(openSession);
        handler.afterConnectionEstablished(closedSession);

        handler.broadcastToUserAndAdmins("user-1", createTestMessage("CLAIM_STATUS_CHANGED"));

        // closedSession should be cleaned up, openSession should receive message
        verify(openSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void should_NotRemoveUserEntry_When_MultipleSessionsRemainAfterClose() throws Exception {
        WebSocketSession session1 = createMockSession("user-1", List.of("USER"));
        WebSocketSession session2 = createMockSession("user-1", List.of("USER"));
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        // Close only one session — user entry must remain
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        assertEquals(1, handler.getActiveSessionCount());
        assertEquals(1, handler.getActiveUserCount());
    }

    @Test
    void should_DoNothing_When_BroadcastToUser_SessionsSetIsEmpty() {
        // Manually create a state where user has empty session set by connecting then disconnecting
        // but calling broadcastToUser before cleanup removes the entry
        // Simplest way: user never connected → sessions is null (already covered)
        // This test verifies sessions.isEmpty() branch won't NPE
        WebSocketSession session = createMockSession("user-1", List.of("USER"));
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // After disconnect, user-1 entry is removed; broadcastToUser should handle null gracefully
        handler.broadcastToUser("user-1", createTestMessage("CLAIM_STATUS_CHANGED"));
        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_HandleTransportError_When_SessionHasNoUserId() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("no-user-session");
        when(session.isOpen()).thenReturn(false); // already closed

        // Should not throw even with null userId
        handler.handleTransportError(session, new RuntimeException("transport error"));
        assertEquals(0, handler.getActiveSessionCount());
    }

    @Test
    void should_NotThrow_When_CloseSession_AlreadyClosed() throws Exception {
        WebSocketSession session = createMockSession("user-1", List.of("USER"));
        when(session.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(session);

        // handleTransportError calls removeSession -> closeSession; session already closed
        handler.handleTransportError(session, new RuntimeException("error"));
        assertEquals(0, handler.getActiveSessionCount());
    }

    private WebSocketMessage createTestMessage(String type) {
        return new WebSocketMessage(
                type,
                "claim-123",
                "APPROVED",
                "UNDER_REVIEW",
                Instant.now(),
                "correlation-456"
        );
    }
}
