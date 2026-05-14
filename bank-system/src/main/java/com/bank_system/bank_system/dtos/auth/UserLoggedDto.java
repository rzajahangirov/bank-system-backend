package com.bank_system.bank_system.dtos.auth;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoggedDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String roleName;
    private boolean isActive;
}
