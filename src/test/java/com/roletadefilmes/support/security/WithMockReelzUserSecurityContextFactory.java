package com.roletadefilmes.support.security;

import com.roletadefilmes.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;
import java.util.UUID;

public class WithMockReelzUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockReelzUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockReelzUser annotation) {
        var principal = new AuthenticatedUser(UUID.fromString(annotation.userId()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
