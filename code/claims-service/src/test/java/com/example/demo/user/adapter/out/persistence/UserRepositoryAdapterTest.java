package com.example.demo.user.adapter.out.persistence;

import com.example.demo.fixtures.UserTestBuilder;
import com.example.demo.user.domain.Email;
import com.example.demo.user.domain.User;
import com.example.demo.user.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRepositoryAdapterTest {

    private UserJpaRepository jpaRepository;
    private UserPersistenceMapper mapper;
    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(UserJpaRepository.class);
        mapper = mock(UserPersistenceMapper.class);
        adapter = new UserRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void should_SaveAndReturnUser() {
        var user = UserTestBuilder.aUser().build();
        var entity = new UserJpaEntity();
        var savedEntity = new UserJpaEntity();
        when(mapper.toJpaEntity(user)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(user);

        User result = adapter.save(user);

        assertThat(result).isEqualTo(user);
        verify(jpaRepository).save(entity);
    }

    @Test
    void should_ReturnUser_When_FindByIdFound() {
        UUID userId = UUID.randomUUID();
        var entity = new UserJpaEntity();
        var user = UserTestBuilder.aUser().withUserId(userId).build();
        when(jpaRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        Optional<User> result = adapter.findById(UserId.of(userId));

        assertThat(result).contains(user);
    }

    @Test
    void should_ReturnEmpty_When_FindByIdNotFound() {
        UUID userId = UUID.randomUUID();
        when(jpaRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = adapter.findById(UserId.of(userId));

        assertThat(result).isEmpty();
    }

    @Test
    void should_ReturnUser_When_FindByEmailFound() {
        var email = Email.of("alice@example.com");
        var entity = new UserJpaEntity();
        var user = UserTestBuilder.aUser().withEmail("alice@example.com").build();
        when(jpaRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        Optional<User> result = adapter.findByEmail(email);

        assertThat(result).contains(user);
    }

    @Test
    void should_ReturnTrue_When_ExistsByEmail() {
        var email = Email.of("exists@example.com");
        when(jpaRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThat(adapter.existsByEmail(email)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_EmailNotExists() {
        var email = Email.of("none@example.com");
        when(jpaRepository.existsByEmail("none@example.com")).thenReturn(false);

        assertThat(adapter.existsByEmail(email)).isFalse();
    }

    @Test
    void should_ReturnAllUsers_When_FindAll() {
        var entity = new UserJpaEntity();
        var user = UserTestBuilder.aUser().build();
        when(jpaRepository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        List<User> result = adapter.findAll();

        assertThat(result).containsExactly(user);
    }

    @Test
    void should_ReturnCount() {
        when(jpaRepository.count()).thenReturn(7L);

        assertThat(adapter.count()).isEqualTo(7L);
    }
}
