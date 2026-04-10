package com.project._3.controllers;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
@Controller
public class ViewController {

    @GetMapping("/perfil")
    public String profile(Authentication authentication, Model model) {

        if (authentication == null) {
            return "redirect:/login";
        }


        if (authentication instanceof OAuth2AuthenticationToken token) {
            model.addAttribute("name", token.getPrincipal().getAttribute("name"));
            model.addAttribute("email", token.getPrincipal().getAttribute("email"));
            model.addAttribute("photo", token.getPrincipal().getAttribute("picture"));
        } else {
            model.addAttribute("name", authentication.getName());
            model.addAttribute("email", authentication.getName()); // No form login, o name costuma ser o email/username
            model.addAttribute("photo", "https://via.placeholder.com/150");
        }

        return "user-profile";
    }

@GetMapping("/login")
    public String login(){
        return "custom-login";

    }
}
