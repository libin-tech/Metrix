package com.bintech.metrix.dto.response;

import com.bintech.metrix.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {

    private String token;
    private Long userId;
    private String username;
    private UserRole role;
    private String nickname;
    private String avatar;
}
