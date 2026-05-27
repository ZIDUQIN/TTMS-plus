package com.ttms.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
    private List<String> permissions;
    private String theme;
}
