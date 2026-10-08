package com.lankaautoparts.autosparepro.user.controller;

import com.lankaautoparts.autosparepro.user.model.Role;
import com.lankaautoparts.autosparepro.user.model.User;
import com.lankaautoparts.autosparepro.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("roles", Role.values());
        return "users";
    }

    @PostMapping("/add")
    public String add(@RequestParam String username,
                       @RequestParam String password,
                       @RequestParam Role role,
                       RedirectAttributes redirectAttributes) {
        String cleanUsername = username == null ? "" : username.trim();

        if (cleanUsername.isEmpty() || password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Username and password are required.");
            return "redirect:/admin/users";
        }
        // Guard against the unique-constraint crash: without this check, a
        // duplicate username reaches the database and blows up with an
        // uncaught DataIntegrityViolationException (white-label error page).
        if (userService.usernameExists(cleanUsername)) {
            redirectAttributes.addFlashAttribute("error", "That username is already taken.");
            return "redirect:/admin/users";
        }

        userService.createUser(cleanUsername, password, role);
        redirectAttributes.addFlashAttribute("success", "User \"" + cleanUsername + "\" created.");
        return "redirect:/admin/users";
    }

    /**
     * Previously there was no way to change a user's role after creation —
     * the only admin action available was delete. This adds that missing
     * "Edit Role" capability, with a guard so an admin can't accidentally
     * strip their own admin access and lock themselves out.
     */
    @PostMapping("/{id}/edit-role")
    public String editRole(@PathVariable Long id,
                            @RequestParam Role role,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        User target = userService.getUserById(id);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        if (target.getUsername().equals(principal.getName()) && role != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "You cannot remove your own admin role.");
            return "redirect:/admin/users";
        }

        userService.updateRole(id, role);
        redirectAttributes.addFlashAttribute("success", "Role updated for \"" + target.getUsername() + "\".");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User target = userService.getUserById(id);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        if (target.getUsername().equals(principal.getName())) {
            redirectAttributes.addFlashAttribute("error", "You cannot delete your own account.");
            return "redirect:/admin/users";
        }
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
