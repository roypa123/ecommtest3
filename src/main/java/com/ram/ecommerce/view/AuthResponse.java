package com.ram.ecommerce.view;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
