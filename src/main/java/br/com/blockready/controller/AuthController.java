package br.com.blockready.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.blockready.dto.Requests.LoginRequest;
import br.com.blockready.dto.Requests.RegistroRequest;
import br.com.blockready.dto.Responses.MeResponse;
import br.com.blockready.dto.Responses.UsuarioResponse;
import br.com.blockready.security.UsuarioPrincipal;
import br.com.blockready.service.AuthService;
import br.com.blockready.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Cadastro, login e usuário atual. O logout é tratado pelo Spring Security (POST /api/v1/auth/logout). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(AuthService authService, UsuarioService usuarioService,
            AuthenticationManager authenticationManager, SecurityContextRepository contextRepository) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse register(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.senha()));

        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId(); // evita fixação de sessão
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);

        return usuarioService.me((UsuarioPrincipal) auth.getPrincipal());
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UsuarioPrincipal ator) {
        return usuarioService.me(ator);
    }
}
