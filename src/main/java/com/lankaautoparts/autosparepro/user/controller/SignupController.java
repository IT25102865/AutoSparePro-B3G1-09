package com.lankaautoparts.autosparepro.user.controller;

import com.lankaautoparts.autosparepro.user.model.Role;
import com.lankaautoparts.autosparepro.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SignupController {

    private final UserService userService;

    public SignupController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          Model model) {
        String cleanUsername = username == null ? "" : username.trim();

        if (cleanUsername.isEmpty() || password == null || password.isEmpty()) {
            model.addAttribute("error", "Username and password are required.");
            return "signup";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters.");
            return "signup";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "signup";
        }
        if (userService.usernameExists(cleanUsername)) {
            model.addAttribute("error", "That username is already taken.");
            return "signup";
        }

        userService.createUser(cleanUsername, password, Role.USER);
        return "redirect:/login?registered=true";
    }
}
