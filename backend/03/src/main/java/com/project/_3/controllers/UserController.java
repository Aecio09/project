package com.project._3.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class UserController {

    @RequestMapping("/")
    public String home(){
        return "Testando";
    }

    @RequestMapping("/user")
    public Principal user(Principal user){
        return user;
    }
}
