package com.example.demo.adapter.in.web;

import com.example.claims.adapter.in.web.generated.model.ClaimStatusDTO;
import com.example.claims.adapter.in.web.generated.model.UpdateClaimStatusRequestDTO;
import com.example.demo.application.usecases.ListAllClaimsUseCase;
import com.example.demo.application.usecases.UpdateClaimStatusCommand;
import com.example.demo.application.usecases.UpdateClaimStatusUseCase;
import com.example.demo.domain.claim.Claim;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.fixtures.ClaimTestBuilder;
import com.example.demo.fixtures.UserTestBuilder;
import com.example.demo.user.domain.UserId;
import com.example.demo.user.domain.UserRepository;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminClaimsControllerTest {

    private ListAllClaimsUseCase listAllClaimsUseCase;
    private UpdateClaimStatusUseCase updateClaimStatusUseCase;
    private UserRepository userRepository;
    private ClaimWebMapper claimWebMapper;
    private Tracer tracer;
    private AdminClaimsController controller;

    @BeforeEach
    void setUp() {
        listAllClaimsUseCase = mock(ListAllClaimsUseCase.class);
        updateClaimStatusUseCase = mock(UpdateClaimStatusUseCase.class);
        userRepository = mock(UserRepository.class);
        claimWebMapper = mock(ClaimWebMapper.class);
        tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        controller = new AdminClaimsController(listAllClaimsUseCase, updateClaimStatusUseCase,
                userRepository, claimWebMapper, tracer);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_ReturnAllClaims_When_NoFilter() {
        Claim claim = ClaimTestBuilder.aClaim().build();
        when(listAllClaimsUseCase.execute(any())).thenReturn(List.of(claim));
        var user = UserTestBuilder.aUser().withUserId(claim.getUserId()).build();
        when(userRepository.findById(UserId.of(claim.getUserId()))).thenReturn(Optional.of(user));

        var response = controller.listAllClaims(Optional.empty(), Optional.empty());

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void should_UseUnknownUser_When_UserNotFound() {
        Claim claim = ClaimTestBuilder.aClaim().build();
        when(listAllClaimsUseCase.execute(any())).thenReturn(List.of(claim));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        var response = controller.listAllClaims(Optional.empty(), Optional.empty());

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getUserName()).isEqualTo("Unknown User");
        assertThat(response.getBody().get(0).getUserEmail()).isEqualTo("unknown@example.com");
    }

    @Test
    void should_FilterByStatus_When_StatusProvided() {
        Claim claim = ClaimTestBuilder.aClaim().build();
        when(listAllClaimsUseCase.execute(any())).thenReturn(List.of(claim));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        var response = controller.listAllClaims(Optional.of(ClaimStatusDTO.SUBMITTED), Optional.empty());

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void should_FilterByUserId_When_UserIdProvided() {
        UUID userId = UUID.randomUUID();
        Claim claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        when(listAllClaimsUseCase.execute(any())).thenReturn(List.of(claim));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        var response = controller.listAllClaims(Optional.empty(), Optional.of(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void should_FilterByStatusAndUserId_When_BothProvided() {
        UUID userId = UUID.randomUUID();
        Claim claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        when(listAllClaimsUseCase.execute(any())).thenReturn(List.of(claim));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        var response = controller.listAllClaims(
                Optional.of(ClaimStatusDTO.SUBMITTED), Optional.of(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void should_UpdateClaimStatus_When_AdminAuthenticated() {
        UUID adminUserId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        setJwtAuthentication(adminUserId);

        Claim updatedClaim = ClaimTestBuilder.aClaim().withClaimId(claimId).build();
        when(updateClaimStatusUseCase.execute(any(UpdateClaimStatusCommand.class))).thenReturn(updatedClaim);
        when(claimWebMapper.toResponse(updatedClaim)).thenReturn(new com.example.claims.adapter.in.web.generated.model.ClaimResponseDTO());

        var requestDTO = new UpdateClaimStatusRequestDTO();
        requestDTO.setNewStatus(ClaimStatusDTO.APPROVED);

        var response = controller.updateClaimStatus(claimId, requestDTO);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void should_ThrowIllegalStateException_When_NoAuthenticationForUpdateStatus() {
        SecurityContextHolder.clearContext();

        var requestDTO = new UpdateClaimStatusRequestDTO();
        requestDTO.setNewStatus(ClaimStatusDTO.APPROVED);

        assertThatThrownBy(() -> controller.updateClaimStatus(UUID.randomUUID(), requestDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user found in security context");
    }

    private void setJwtAuthentication(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }
}
