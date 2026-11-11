package com.example.demo.adapter.in.web;

import com.example.demo.application.auth.AuthenticationFacade;
import com.example.demo.application.auth.ClaimsServicePort;
import com.example.demo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimsController.class)
@Import({SecurityConfig.class, ClaimResponseMapperImpl.class, ErrorTypeConstants.class,
        com.example.demo.config.TestRestClientConfig.class})
class ClaimsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationFacade authenticationFacade;

    @MockitoBean
    private ClaimsServicePort claimsServicePort;

    private static final UUID CLAIM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private ClaimsServicePort.ClaimRecord sampleClaim() {
        return new ClaimsServicePort.ClaimRecord(
                CLAIM_ID, USER_ID,
                LocalDate.of(2024, 1, 15),
                "London, UK",
                "Car accident",
                BigDecimal.valueOf(1500.00),
                "SUBMITTED",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Test
    @WithMockUser
    void should_Return201_When_SubmitClaim() throws Exception {
        when(authenticationFacade.getJwtTokenValue()).thenReturn("fake-jwt");
        when(claimsServicePort.submitClaim(any(), any(), any(), any(), any())).thenReturn(sampleClaim());

        mockMvc.perform(post("/api/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "incidentDate": "2024-01-15",
                                    "incidentLocation": "London, UK",
                                    "description": "Car accident",
                                    "claimAmount": 1500.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimId").value(CLAIM_ID.toString()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser
    void should_Return200_When_ListClaims() throws Exception {
        when(authenticationFacade.getJwtTokenValue()).thenReturn("fake-jwt");
        when(authenticationFacade.getCurrentUserId()).thenReturn(USER_ID);
        when(claimsServicePort.listClaims(any(), eq(USER_ID))).thenReturn(List.of(
                new ClaimsServicePort.ClaimSummaryRecord(
                        CLAIM_ID,
                        LocalDate.of(2024, 1, 15),
                        "Car accident",
                        BigDecimal.valueOf(1500.00),
                        "SUBMITTED",
                        OffsetDateTime.now()
                )
        ));

        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value(CLAIM_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser
    void should_Return200_When_GetClaim() throws Exception {
        when(authenticationFacade.getJwtTokenValue()).thenReturn("fake-jwt");
        when(claimsServicePort.getClaim(any(), eq(CLAIM_ID))).thenReturn(sampleClaim());

        mockMvc.perform(get("/api/claims/{id}", CLAIM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(CLAIM_ID.toString()));
    }

    @Test
    void should_Return401_When_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isUnauthorized());
    }
}
