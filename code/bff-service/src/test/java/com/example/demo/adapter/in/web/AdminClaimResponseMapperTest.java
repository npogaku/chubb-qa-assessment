package com.example.demo.adapter.in.web;

import com.example.bff.adapter.out.claims.generated.model.AdminClaimResponseDTO;
import com.example.bff.adapter.out.claims.generated.model.ClaimStatusDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminClaimResponseMapperTest {

    private final AdminClaimResponseMapper mapper = new AdminClaimResponseMapperImpl();

    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private AdminClaimResponseDTO sampleAdminClaim() {
        return new AdminClaimResponseDTO()
                .claimId(CLAIM_ID)
                .userId(USER_ID)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .incidentLocation("London, UK")
                .description("Car accident")
                .claimAmount(1500.00)
                .status(ClaimStatusDTO.SUBMITTED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now());
    }

    @Test
    void should_MapAdminClaimResponseDTO_ToClaimResponseDTO() {
        var dto = mapper.toClaimResponse(sampleAdminClaim());

        assertThat(dto.getClaimId()).isEqualTo(CLAIM_ID);
        assertThat(dto.getUserId()).isEqualTo(USER_ID);
        assertThat(dto.getStatus().getValue()).isEqualTo("SUBMITTED");
    }

    @Test
    void should_MapList_ToClaimResponseDTOList() {
        var list = mapper.toClaimResponseList(List.of(sampleAdminClaim(), sampleAdminClaim()));

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getClaimId()).isEqualTo(CLAIM_ID);
    }

    @Test
    void should_HandleNull_Status() {
        var adminClaim = sampleAdminClaim().status(null);

        var dto = mapper.toClaimResponse(adminClaim);

        assertThat(dto.getStatus()).isNull();
    }

    @Test
    void should_MapClaimsServiceClaimResponseDTO_ToClaimResponseDTO() {
        var claimsServiceResponse = new com.example.bff.adapter.out.claims.generated.model.ClaimResponseDTO()
                .claimId(CLAIM_ID)
                .userId(USER_ID)
                .incidentDate(LocalDate.of(2024, 1, 15))
                .incidentLocation("London")
                .description("desc")
                .claimAmount(500.0)
                .status(ClaimStatusDTO.APPROVED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now());

        var dto = mapper.toClaimResponse(claimsServiceResponse);

        assertThat(dto.getStatus().getValue()).isEqualTo("APPROVED");
    }
}
