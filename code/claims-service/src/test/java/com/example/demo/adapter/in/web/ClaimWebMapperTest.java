package com.example.demo.adapter.in.web;

import com.example.claims.adapter.in.web.generated.model.ClaimResponseDTO;
import com.example.claims.adapter.in.web.generated.model.ClaimSummaryResponseDTO;
import com.example.demo.domain.claim.ClaimStatus;
import com.example.demo.fixtures.ClaimTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mapstruct.factory.Mappers.getMapper;

class ClaimWebMapperTest {

    private ClaimWebMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = getMapper(ClaimWebMapper.class);
    }

    @Test
    void should_MapClaimToResponse() {
        var claim = ClaimTestBuilder.aClaim().build();

        ClaimResponseDTO dto = mapper.toResponse(claim);

        assertThat(dto.getClaimId()).isEqualTo(claim.getClaimId());
        assertThat(dto.getUserId()).isEqualTo(claim.getUserId());
        assertThat(dto.getIncidentDate()).isEqualTo(claim.getIncidentDate());
        assertThat(dto.getIncidentLocation()).isEqualTo(claim.getIncidentLocation());
        assertThat(dto.getDescription()).isEqualTo(claim.getDescription());
        assertThat(dto.getClaimAmount()).isEqualTo(claim.getClaimAmount().doubleValue());
        assertThat(dto.getStatus().getValue()).isEqualTo(ClaimStatus.SUBMITTED.name());
    }

    @Test
    void should_MapClaimToSummaryResponse() {
        var claim = ClaimTestBuilder.aClaim().build();

        ClaimSummaryResponseDTO dto = mapper.toSummaryResponse(claim);

        assertThat(dto.getClaimId()).isEqualTo(claim.getClaimId());
        assertThat(dto.getClaimAmount()).isEqualTo(claim.getClaimAmount().doubleValue());
        assertThat(dto.getStatus().getValue()).isEqualTo(ClaimStatus.SUBMITTED.name());
    }

    @Test
    void should_TruncateDescription_When_LongerThan100Chars() {
        String longDesc = "A".repeat(150);
        var claim = ClaimTestBuilder.aClaim().withDescription(longDesc).build();

        ClaimSummaryResponseDTO dto = mapper.toSummaryResponse(claim);

        assertThat(dto.getDescription()).hasSize(100);
    }

    @Test
    void should_NotTruncateDescription_When_ShorterThan100Chars() {
        String shortDesc = "Short description";
        var claim = ClaimTestBuilder.aClaim().withDescription(shortDesc).build();

        ClaimSummaryResponseDTO dto = mapper.toSummaryResponse(claim);

        assertThat(dto.getDescription()).isEqualTo(shortDesc);
    }

    @Test
    void should_MapInstantToOffsetDateTime_When_NotNull() {
        Instant now = Instant.now();

        OffsetDateTime result = mapper.toOffsetDateTime(now);

        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void should_ReturnNull_When_InstantIsNull() {
        assertThat(mapper.toOffsetDateTime(null)).isNull();
    }
}
