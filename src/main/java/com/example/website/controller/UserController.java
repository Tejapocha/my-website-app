package com.example.website.controller;

import com.example.website.model.User;
import com.example.website.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ✅ 1. Show Login Page
    // The path is handled by Spring Security, but we map it here to return the view
    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; 
    }
    // ✅ 2. Show Registration Form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Pass a new User object to the form for Thymeleaf data binding
        model.addAttribute("user", new User());
        return "register";
    }

    // ✅ 3. Handle Registration Submission
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, 
                               RedirectAttributes redirectAttributes) {
        try {
            // NOTE: The password in 'user' is the raw password from the form
            userService.registerNewUser(user);
            
            // Success message on successful registration
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please log in.");
            
            // Redirect to the login page
            return "redirect:/login";

        } catch (IllegalStateException e) {
            // Error handling for duplicate username/email
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            
            // Redirect back to registration page with the error
            return "redirect:/register";
        }
    }
}