package com.mymindmirror.backend.resolver;

import com.mymindmirror.backend.annotation.CurrentUser;
import com.mymindmirror.backend.model.User;
import com.mymindmirror.backend.security.services.UserDetailsImpl;
import com.mymindmirror.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated principal found for @CurrentUser");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            log.warn("Principal is not of type UserDetailsImpl: {}", principal.getClass());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
        }

        // Fetch the full User entity from the database (ensures all fields are available)
        User user = userService.findById(userDetails.getId())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database: {}", userDetails.getId());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
                });

        log.debug("Resolved @CurrentUser for user: {}", user.getUsername());
        return user;
    }
}