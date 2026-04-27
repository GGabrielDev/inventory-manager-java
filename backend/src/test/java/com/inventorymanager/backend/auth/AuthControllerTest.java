package com.inventorymanager.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventorymanager.backend.domain.Role;
import com.inventorymanager.backend.domain.User;
import com.inventorymanager.backend.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

class AuthControllerTest {

    private MockMvc mockMvc;
    private UserRepository userRepository;
    private AppUserPrincipal mockPrincipal;

    @BeforeEach
    void setUp() {
        // We still mock the Repository as it's an interface and doesn't trigger the ByteBuddy issue like classes do
        userRepository = Mockito.mock(UserRepository.class);
        
        // Provide real instances for simple service classes to avoid Mockito + Java 25 issues
        var jwtService = new JwtService("dummy-secret-must-be-long-enough-32-chars", 60);
        
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        Role adminRole = new Role();
        adminRole.setName("admin");
        mockUser.setRoles(Set.of(adminRole));
        
        this.mockPrincipal = new AppUserPrincipal(mockUser);
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Note: AuthController doesn't strictly need a real AuthenticationManager for the /me endpoint
        AuthController authController = new AuthController(null, jwtService, userRepository);
        
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(AppUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    void meReturnsCorrectPayloadStructure() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").isString())
                .andExpect(jsonPath("$.roles[0]").value("admin"));
    }
}
