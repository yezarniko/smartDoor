package com.smartdoor.domain;

import jakarta.persistence.*;

import static com.smartdoor.domain.Enums.UserRole;
import static com.smartdoor.domain.Enums.UserStatus;

@Entity
@Table(name = "users")
public class UserAccount extends BaseEntity {
    @Column(name = "public_id", nullable = false, unique = true, length = 40)
    private String publicId;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(length = 190)
    private String email;

    @Column(length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    protected UserAccount() {}

    public UserAccount(String publicId, String fullName, String email, String phone, UserRole role) {
        update(publicId, fullName, email, phone, role);
        this.status = UserStatus.ACTIVE;
    }

    public void update(String publicId, String fullName, String email, String phone, UserRole role) {
        this.publicId = publicId.trim();
        this.fullName = fullName.trim();
        this.email = blankToNull(email);
        this.phone = blankToNull(phone);
        this.role = role;
    }

    public void setStatus(UserStatus status) { this.status = status; }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public String getPublicId() { return publicId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
}

