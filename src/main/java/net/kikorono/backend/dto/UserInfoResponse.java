package net.kikorono.backend.dto;

/**
 * A basic user information DTO (data-transfer-object).
 * Contains a user ID, a username, and an email address.
 */
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;

    public UserInfoResponse(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
