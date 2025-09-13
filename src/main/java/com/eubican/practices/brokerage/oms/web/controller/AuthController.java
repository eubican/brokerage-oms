package com.eubican.practices.brokerage.oms.web.controller;

import com.eubican.practices.brokerage.oms.domain.model.constants.ControllerPaths;
import com.eubican.practices.brokerage.oms.security.TokenService;
import com.eubican.practices.brokerage.oms.web.dto.LoginRequest;
import com.eubican.practices.brokerage.oms.web.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ControllerPaths.API_V_1_AUTH)
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody LoginRequest request) {
        Jwt jwt = tokenService.createToken(request);
        return new LoginResponse("Bearer", jwt.getTokenValue(), jwt.getClaim(JwtClaimNames.EXP));
    }

}
