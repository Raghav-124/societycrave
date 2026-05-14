package com.raghav.societycrave.controller;

import com.raghav.societycrave.entity.User;
import com.raghav.societycrave.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/society/{societyName}")
    public List<User> getUsersBySociety(@PathVariable final String societyName) {
        return userService.getUsersBySociety(societyName);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable final Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/email")
    public User getUserByEmail(@RequestParam final String email) {
        return userService.getUserByEmail(email);
    }

    @GetMapping("/search")
    public Page<User> searchUsers(@RequestParam final String keyword,
                                  @RequestParam(defaultValue = "0") final int page,
                                  @RequestParam(defaultValue = "10") final int size) {
        return userService.searchUsers(keyword, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody final User user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable final Long id,
                           @Valid @RequestBody final User userDetails) {
        return userService.updateUser(id, userDetails);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable final Long id) {
        userService.deleteUser(id);
    }
}
