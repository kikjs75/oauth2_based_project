package com.portfolio.app.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, length = 20)
    private String provider = "LOCAL";

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.provider = "LOCAL";
        this.roles.add("ROLE_USER");
    }

    public User(String username, String provider, String providerId) {
        this.username = username;
        this.provider = provider;
        this.providerId = providerId;
        this.roles.add("ROLE_USER");
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public Set<String> getRoles() { return roles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
