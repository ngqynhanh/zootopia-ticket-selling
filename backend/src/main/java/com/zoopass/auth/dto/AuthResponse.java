package com.zoopass.auth.dto;

import com.zoopass.user.dto.UserResponse;

/**
 * Returned by login and refresh. The refresh token is not in the body: it is set as an
 * HttpOnly cookie that JavaScript cannot read.
 *
 * @param accessToken JWT to send as {@code Authorization: Bearer <token>}
 * @param tokenType   always "Bearer"
 * @param expiresIn   access-token lifetime in seconds
 * @param user        the logged-in user
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
