package com.example.file_storage.controller.common;

import com.example.file_storage.dto.UserCreateUpdateDTO;
import com.example.file_storage.dto.UserDTO;
import com.example.file_storage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PublicAuthorizationController {

    private static final Logger log = LoggerFactory.getLogger(PublicAuthorizationController.class);
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Autowired
    public PublicAuthorizationController(UserService userService,
                                         AuthenticationManager authenticationManager
                                         ) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/auth/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO registrate(@Valid @RequestBody UserCreateUpdateDTO dto,
                              HttpServletRequest request,
                              HttpServletResponse response) {

        log.info("Sign-up attempt");
        userService.register(dto);

        String userName = dto.getUserName();
        String password = dto.getPassword();

        Authentication auth = new UsernamePasswordAuthenticationToken(userName, password);
        Authentication authResult = authenticationManager.authenticate(auth);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);

        return new UserDTO(dto.getUserName());
    }

    @PostMapping("/auth/sign-in")
    public UserDTO authorize(@RequestBody @Valid UserCreateUpdateDTO dto,
                             HttpServletRequest request,
                             HttpServletResponse response) {

        String userName = dto.getUserName();
        String password = dto.getPassword();

        Authentication auth = new UsernamePasswordAuthenticationToken(userName, password);
        Authentication authResult = authenticationManager.authenticate(auth);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);

        return new UserDTO(dto.getUserName());
    }
}
