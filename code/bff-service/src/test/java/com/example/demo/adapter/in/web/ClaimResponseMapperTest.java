package com.example.demo.adapter.in.web;

import com.example.demo.application.auth.ClaimsServicePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimResponseMapperTest {

    private final ClaimResponseMapper mapper = new ClaimResponseMapperImpl();

    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void should_MapClaimRecord_ToResponse() {
        var record = new ClaimsServicePort.ClaimRecord(
                CLAIM_ID, USER_ID,
                LocalDate.of(2024, 1, 15),
                "London, UK",
                "Car accident",
                BigDecimal.valueOf(1500.50),
                "SUBMITTED",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        var dto = mapper.toResponse(record);

        assertThat(dto.getClaimId()).isEqualTo(CLAIM_ID);
        assertThat(dto.getUserId()).isEqualTo(USER_ID);
        assertThat(dto.getIncidentLocation()).isEqualTo("London, UK");
        assertThat(dto.getDescription()).isEqualTo("Car accident");
        assertThat(dto.getClaimAmount()).isEqualTo(1500.50);
        assertThat(dto.getStatus().getValue()).isEqualTo("SUBMITTED");
    }

    @Test
    void should_MapClaimSummaryRecord_ToSummaryResponse() {
        var record = new ClaimsServicePort.ClaimSummaryRecord(
                CLAIM_ID,
                LocalDate.of(2024, 1, 15),
                "Car accident",
                BigDecimal.valueOf(2000.00),
                "APPROVED",
                OffsetDateTime.now()
        );

        var dto = mapper.toSummaryResponse(record);

        assertThat(dto.getClaimId()).isEqualTo(CLAIM_ID);
        assertThat(dto.getClaimAmount()).isEqualTo(2000.00);
        assertThat(dto.getStatus().getValue()).isEqualTo("APPROVED");
    }

    @Test
    void should_HandleNull_ClaimAmount() {
        var record = new ClaimsServicePort.ClaimRecord(
                CLAIM_ID, USER_ID,
                LocalDate.of(2024, 1, 15),
                "London",
                "desc",
                null,
                "SUBMITTED",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        var dto = mapper.toResponse(record);

        assertThat(dto.getClaimAmount()).isNull();
    }
}
