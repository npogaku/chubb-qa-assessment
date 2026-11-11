package com.example.demo.adapter.out.keycloak;

import com.example.demo.application.auth.ExternalServiceException;
import com.example.demo.application.auth.UserAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KeycloakAdminAdapterTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private RolesResource rolesResource;

    private KeycloakAdminAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KeycloakAdminAdapter(keycloak, "demo-app");
        when(keycloak.realm("demo-app")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);
    }

    @Test
    void should_ReturnUserId_When_UserCreatedSuccessfully() {
        var newUserId = UUID.randomUUID();
        var locationUri = URI.create("http://keycloak/auth/admin/realms/demo-app/users/" + newUserId);
        var createResponse = Response.created(locationUri).build();

        when(usersResource.create(any())).thenReturn(createResponse);

        var roleResource = mock(RoleResource.class);
        var userResource = mock(UserResource.class);
        var roleMappingResource = mock(RoleMappingResource.class);
        var roleScopeResource = mock(RoleScopeResource.class);
        var roleRepresentation = new RoleRepresentation();

        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
        when(usersResource.get(newUserId.toString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        var result = adapter.createUser("test@example.com", "password", "Test User", "CLAIMANT");

        assertThat(result).isEqualTo(newUserId);
    }

    @Test
    void should_Throw_UserAlreadyExistsException_When_409Returned() {
        var conflictResponse = Response.status(409).build();
        when(usersResource.create(any())).thenReturn(conflictResponse);

        assertThatThrownBy(() -> adapter.createUser("test@example.com", "password", "Test User", "CLAIMANT"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void should_Throw_ExternalServiceException_When_NonSuccessStatus() {
        var errorResponse = Response.serverError().build();
        when(usersResource.create(any())).thenReturn(errorResponse);

        assertThatThrownBy(() -> adapter.createUser("test@example.com", "password", "Test User", "CLAIMANT"))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void should_ExtractFirstName_FromFullName() {
        var newUserId = UUID.randomUUID();
        var locationUri = URI.create("http://keycloak/auth/admin/realms/demo-app/users/" + newUserId);
        var createResponse = Response.created(locationUri).build();

        when(usersResource.create(any())).thenReturn(createResponse);

        var roleResource = mock(RoleResource.class);
        var userResource = mock(UserResource.class);
        var roleMappingResource2 = mock(RoleMappingResource.class);
        var roleScopeResource = mock(RoleScopeResource.class);
        var roleRepresentation = new RoleRepresentation();

        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
        when(usersResource.get(newUserId.toString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource2);
        when(roleMappingResource2.realmLevel()).thenReturn(roleScopeResource);

        // single name — should default last name to "User"
        var result = adapter.createUser("test@example.com", "password", "John", "claimant");

        assertThat(result).isEqualTo(newUserId);
    }

    @Test
    void should_Throw_ExternalServiceException_When_LocationHeaderMissing() {
        var responseWithoutLocation = Response.status(201).build();
        when(usersResource.create(any())).thenReturn(responseWithoutLocation);

        assertThatThrownBy(() -> adapter.createUser("test@example.com", "password", "Test User", "CLAIMANT"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("location header missing");
    }
}
