package com.example.file_storage.controller;

import com.example.file_storage.dto.UserDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {


    @GetMapping("/user/me")
    public UserDTO getUser(@AuthenticationPrincipal UserDetails userDetails){

        return new UserDTO(userDetails.getUsername());
    }


}
