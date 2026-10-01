package com.mymindmirror.backend.security.services;

import com.mymindmirror.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {

    private UUID userId;
    private User user;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        userDetails = UserDetailsImpl.build(user);
    }

    @Test
    void build_ShouldCreateUserDetailsWithCorrectFields() {
        assertThat(userDetails.getId()).isEqualTo(userId);
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getEmail()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void isAccountNonExpired_ShouldReturnTrue() {
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    void isAccountNonLocked_ShouldReturnTrue() {
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_ShouldReturnTrue() {
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isEnabled_ShouldReturnTrue() {
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void equals_SameId_ShouldReturnTrue() {
        UserDetailsImpl sameId = new UserDetailsImpl(userId, "different", "diff@test.com", "pass", null);
        assertThat(userDetails.equals(sameId)).isTrue();
    }

    @Test
    void equals_DifferentId_ShouldReturnFalse() {
        UUID otherId = UUID.randomUUID();
        UserDetailsImpl other = new UserDetailsImpl(otherId, "testuser", "test@example.com", "hashedPassword", null);
        assertThat(userDetails.equals(other)).isFalse();
    }

    @Test
    void equals_Null_ShouldReturnFalse() {
        assertThat(userDetails.equals(null)).isFalse();
    }

    @Test
    void equals_SameObject_ShouldReturnTrue() {
        assertThat(userDetails.equals(userDetails)).isTrue();
    }

    @Test
    void equals_DifferentClass_ShouldReturnFalse() {
        assertThat(userDetails.equals("some string")).isFalse();
    }

    @Test
    void hashCode_ShouldBeConsistentWithId() {
        int hashCode1 = userDetails.hashCode();
        int hashCode2 = userDetails.hashCode();
        assertThat(hashCode1).isEqualTo(hashCode2);
        // Two instances with same ID should have same hash code
        UserDetailsImpl sameId = new UserDetailsImpl(userId, "different", "diff@test.com", "pass", null);
        assertThat(sameId.hashCode()).isEqualTo(userDetails.hashCode());
    }

    @Test
    void equals_BothNullId_ShouldReturnTrue() {
        UserDetailsImpl a = new UserDetailsImpl(null, "user1", "a@test.com", "pass", null);
        UserDetailsImpl b = new UserDetailsImpl(null, "user2", "b@test.com", "pass", null);
        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void hashCode_WithNullId_ShouldNotThrow() {
        UserDetailsImpl a = new UserDetailsImpl(null, "user", "test@test.com", "pass", null);
        assertThat(a.hashCode()).isNotNull();  // shouldn't NPE
    }
}