package net.kikorono.backend.controllers;

import jakarta.validation.Valid;
import net.kikorono.backend.db.entities.UserEntity;
import net.kikorono.backend.db.repositories.UserRepository;
import net.kikorono.backend.dto.LoginRequest;
import net.kikorono.backend.dto.MessageResponse;
import net.kikorono.backend.dto.SignupRequest;
import net.kikorono.backend.dto.UserInfoResponse;
import net.kikorono.backend.security.JwtUtils;
import net.kikorono.backend.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Handles routes for user sign-up, sign-in, and sign-out.<br>
 * <b>/api/auth</b>
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    /**
     * Handles a user sign-in request, accepting a username and password to generate a
     * JWT (JSON Web Token) if the login request is valid. If valid, the generated JWT
     * is returned to the user in the response as a cookie that will be saved in their
     * browser. Each subsequent request will/should contain this cookie, allowing for
     * the backend to fetch information about the user that is signed in.
     * @param loginRequest The sign-in request, containing the username and password
     *                     provided by the user.
     * @return A ResponseEntity containing the details of the user signing in and a
     * cookie with their JWT authentication token.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // Attempts to authenticate the username and password provided in the request body.
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // Sets the application security context to the current authentication attempt.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Attempts to find the details of the user that is being signed in based off
        // of the authentication attempt.
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generates a new JSON Web Token from the user's information, to be used for
        // authenticating the user in future requests to the backend.
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        // Responds to the client with the generated JWT cookie and the details of the
        // user that has been signed in.
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new UserInfoResponse(userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail()));
    }

    /**
     * Handles a user sign-up request, accepting a username, email address, and
     * password to create a new user entity.
     * @param signUpRequest The user's desired username, email address, and password.
     * @return If successful, a ResponseEntity containing a MessageResponse informing
     * the client that the user was successfully created. Responds with a 400 Bad
     * Request error if a malformed email address or an already existing username
     * were provided.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        // Validates that the username being registered does not already exist.
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        // Validates that the email being registered does not already exist.
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create the user's account...
        UserEntity user = new UserEntity(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        // ...and save it to the database.
        userRepository.save(user);

        // Responds to the client with a 200 OK status and a message notifying of
        // successful user registration.
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * Handles a user sign-out request, deleting their active JWT session cookie.
     * @return A ResponseEntity containing a MessageResponse notifying the client
     * that the user has been signed out.
     */
    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        // Creates an empty cookie to replace the client's existing JWT cookie.
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();

        // Responds to the client with a 200 OK status, an empty authentication cookie
        // (to clear it out), and a message notifying of successful user sign-out.
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!"));
    }
}
