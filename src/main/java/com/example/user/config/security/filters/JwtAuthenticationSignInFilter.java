package com.example.user.config.security.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.user.config.security.JWTSecurityConstants;
import com.example.user.config.security.SecurityMember;
import com.example.user.exception.SignInFailedException;
import com.example.user.payload.UserModel;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

public class JwtAuthenticationSignInFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;

    @Autowired
    public JwtAuthenticationSignInFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        UserModel model = null;
        try {
            model = new ObjectMapper().readValue(req.getInputStream(), UserModel.class);
            Objects.requireNonNull(model.getEmail());
            Objects.requireNonNull(model.getPassword());
        } catch (JsonParseException | JsonMappingException e) {
            throw new SignInFailedException("Invalid signin payload data.", e);
        } catch (NullPointerException e) {
            throw new SignInFailedException("'email' and 'password' data should be required.", e);
        }

        // Authenticate user
        return authenticationManager.authenticate(
                // Create login token
                new UsernamePasswordAuthenticationToken(
                        model.getEmail(),
                        model.getPassword(),
                        Collections.emptyList()
                )
        );
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        // setting JWT at response header
        SecurityMember member = (SecurityMember)auth.getPrincipal();

        long current = System.currentTimeMillis();
        Date issuedAt = new Date(current);
        Date expiredAt = new Date(current + JWTSecurityConstants.EXPIRATION_TIME);

        String token = JWT.create()
                .withIssuer("MyCoupon")
                .withAudience(Long.toString(member.getId()))
                .withSubject(member.getUsername())
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiredAt)
                .sign(Algorithm.HMAC512(JWTSecurityConstants.SECRET.getBytes()));

        res.addHeader(JWTSecurityConstants.HEADER_STRING, JWTSecurityConstants.TOKEN_PREFIX + token);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        PrintWriter writer = response.getWriter();
        if(failed instanceof SignInFailedException) {
            writer.println(failed.getLocalizedMessage());
        } else {
            writer.println("login failed.");
        }
    }
}
