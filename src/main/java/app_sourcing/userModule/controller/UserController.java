package app_sourcing.userModule.controller;

import app_sourcing.userModule.dtos.CreateUserRequest;
import app_sourcing.userModule.dtos.UserDto;
import app_sourcing.userModule.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/create-admin")
    public ResponseEntity<UserDto> createAdminUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto createdAdmin = userService.createAdminUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAdmin);
    }
}
