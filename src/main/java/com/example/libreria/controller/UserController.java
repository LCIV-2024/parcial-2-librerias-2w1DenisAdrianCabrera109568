package com.example.libreria.controller;

import com.example.libreria.dto.UserRequestDTO;
import com.example.libreria.dto.UserResponseDTO;
import com.example.libreria.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO requestDTO) {
       // TODO: Implementar la creación de un usuario
        //Resolucion:
       UserResponseDTO createdUser = userService.createUser(requestDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        // TODO: Implementar la obtención de un usuario por su ID
        //Resolucion:
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        //TODO: Implementar la obtención de todos los usuarios
        //Resolucion:
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO requestDTO) {
        //TODO: Implementar la actualización de un usuario
        //Resolucion:
        UserResponseDTO updatedUser = userService.updateUser(id, requestDTO);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        //TODO: Implementar la eliminación de un usuario
        //Resolucion:
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

