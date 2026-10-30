package com.example.order.adapters.web.security;

import com.example.order.domain.value.Origin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** The origin of the current request is the {@code sub} claim of the validated client token. */
@Component
public class AuthenticatedOrigin {

    public Origin current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return Origin.of(token.getToken().getSubject());
        }
        throw new IllegalStateException("No client token on the current request");
    }
}
