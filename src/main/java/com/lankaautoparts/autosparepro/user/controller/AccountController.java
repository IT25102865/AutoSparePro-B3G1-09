package com.lankaautoparts.autosparepro.user.controller;

import com.lankaautoparts.autosparepro.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String account(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        return "account";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword,
                                  Model model, Principal principal) {
        model.addAttribute("username", principal.getName());

        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("error", "New password must be at least 6 characters.");
            return "account";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirmation do not match.");
            return "account";
        }

        boolean updated = userService.changePassword(principal.getName(), currentPassword, newPassword);
        if (!updated) {
            model.addAttribute("error", "Current password is incorrect.");
            return "account";
        }

        model.addAttribute("success", "Password updated successfully.");
        return "account";
    }
}
