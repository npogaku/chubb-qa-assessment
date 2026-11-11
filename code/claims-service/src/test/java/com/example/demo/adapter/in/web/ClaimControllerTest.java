package com.example.demo.adapter.in.web;

import com.example.claims.adapter.in.web.generated.model.ClaimResponseDTO;
import com.example.claims.adapter.in.web.generated.model.ClaimSummaryResponseDTO;
import com.example.claims.adapter.in.web.generated.model.CreateClaimRequestDTO;
import com.example.demo.application.usecases.CreateClaimUseCase;
import com.example.demo.application.usecases.GetClaimUseCase;
import com.example.demo.application.usecases.ListClaimsUseCase;
import com.example.demo.fixtures.ClaimTestBuilder;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimControllerTest {

    private CreateClaimUseCase createClaimUseCase;
    private GetClaimUseCase getClaimUseCase;
    private ListClaimsUseCase listClaimsUseCase;
    private ClaimWebMapper claimWebMapper;
    private Tracer tracer;
    private ClaimController controller;

    @BeforeEach
    void setUp() {
        createClaimUseCase = mock(CreateClaimUseCase.class);
        getClaimUseCase = mock(GetClaimUseCase.class);
        listClaimsUseCase = mock(ListClaimsUseCase.class);
        claimWebMapper = mock(ClaimWebMapper.class);
        tracer = mock(Tracer.class);
        controller = new ClaimController(createClaimUseCase, getClaimUseCase, listClaimsUseCase, claimWebMapper, tracer);
    }

    @Test
    void should_CreateClaim_And_Return201() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = buildJwt(userId);
        var claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        var responseDTO = new ClaimResponseDTO();

        when(tracer.currentSpan()).thenReturn(null);
        when(createClaimUseCase.execute(any())).thenReturn(claim);
        when(claimWebMapper.toResponse(claim)).thenReturn(responseDTO);

        var request = new CreateClaimRequestDTO();
        request.setIncidentDate(LocalDate.now().minusDays(1));
        request.setIncidentLocation("Main St");
        request.setDescription("Pipe burst");
        request.setClaimAmount(5000.0);

        var response = controller.createClaim(jwt, request);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(createClaimUseCase).execute(any());
    }

    @Test
    void should_CreateClaim_WithSpanTags_When_SpanPresent() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = buildJwt(userId);
        var claim = ClaimTestBuilder.aClaim().withUserId(userId).build();
        var responseDTO = new ClaimResponseDTO();
        Span span = mock(Span.class);
        when(span.tag(any(), any())).thenReturn(span);

        when(tracer.currentSpan()).thenReturn(span);
        when(createClaimUseCase.execute(any())).thenReturn(claim);
        when(claimWebMapper.toResponse(claim)).thenReturn(responseDTO);

        var request = new CreateClaimRequestDTO();
        request.setIncidentDate(LocalDate.now().minusDays(1));
        request.setIncidentLocation("Main St");
        request.setDescription("Pipe burst");
        request.setClaimAmount(5000.0);

        var response = controller.createClaim(jwt, request);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.CREATED.value());
        verify(span).tag("userId", userId.toString());
    }

    @Test
    void should_GetClaim_And_Return200() {
        UUID userId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        Jwt jwt = buildJwt(userId);
        var claim = ClaimTestBuilder.aClaim().withClaimId(claimId).withUserId(userId).build();
        var responseDTO = new ClaimResponseDTO();

        when(tracer.currentSpan()).thenReturn(null);
        when(getClaimUseCase.execute(any())).thenReturn(claim);
        when(claimWebMapper.toResponse(claim)).thenReturn(responseDTO);

        var response = controller.getClaim(jwt, claimId);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(getClaimUseCase).execute(any());
    }

    @Test
    void should_ListClaims_And_Return200() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = buildJwt(userId);
        var claim1 = ClaimTestBuilder.aClaim().withUserId(userId).build();
        var claim2 = ClaimTestBuilder.aClaim().withUserId(userId).build();
        var summaryDTO = new ClaimSummaryResponseDTO();

        when(tracer.currentSpan()).thenReturn(null);
        when(listClaimsUseCase.execute(any())).thenReturn(List.of(claim1, claim2));
        when(claimWebMapper.toSummaryResponse(any())).thenReturn(summaryDTO);

        var response = controller.listClaims(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).hasSize(2);
        verify(listClaimsUseCase).execute(any());
    }

    @Test
    void should_ListClaims_WithSpanTags_When_SpanPresent() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = buildJwt(userId);
        Span span = mock(Span.class);
        when(span.tag(any(), any())).thenReturn(span);

        when(tracer.currentSpan()).thenReturn(span);
        when(listClaimsUseCase.execute(any())).thenReturn(List.of());

        var response = controller.listClaims(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        verify(span).tag("userId", userId.toString());
    }

    private Jwt buildJwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
