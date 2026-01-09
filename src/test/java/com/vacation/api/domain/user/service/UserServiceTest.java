package com.vacation.api.domain.user.service;

import com.vacation.api.domain.user.entity.User;
import com.vacation.api.domain.user.repository.UserRepository;
import com.vacation.api.domain.user.request.JoinRequest;
import com.vacation.api.domain.user.request.LoginRequest;
import com.vacation.api.enums.UserStatus;
import com.vacation.api.exception.ApiErrorCode;
import com.vacation.api.exception.ApiException;
import com.vacation.api.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserService 테스트
 *
 * @author vacation-api
 * @version 1.0
 * @since 2026-01-07
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private JoinRequest joinRequest;

    @BeforeEach
    void setUp() {
        joinRequest = new JoinRequest();
        joinRequest.setEmail("test@example.com");
        joinRequest.setName("홍길동");
        joinRequest.setPassword("password123");
        joinRequest.setDivision("서비스사업본부");
        joinRequest.setTeam("서비스2팀");
        joinRequest.setPosition("과장");
    }

    @Test
    @DisplayName("회원가입 성공 - 유효한 요청으로 사용자가 생성되어야 한다")
    void testJoin_Success() {
        // given
        String encodedPassword = "encoded_password_123";
        User savedUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("홍길동")
                .password(encodedPassword)
                .division("서비스사업본부")
                .team("서비스2팀")
                .position("과장")
                .status(UserStatus.PENDING)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        User result = userService.join(joinRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(result.getPassword()).isEqualTo(encodedPassword);

        // verify
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 같은 이메일로 두 번 회원가입 시도 시 ApiException이 발생해야 한다")
    void testJoin_DuplicateEmail() {
        // given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.join(joinRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage(ApiErrorCode.DUPLICATE_EMAIL.getDescription())
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.DUPLICATE_EMAIL);
                });

        // verify
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 - 유효한 이메일과 비밀번호로 로그인 시 JWT 토큰 배열을 반환해야 한다")
    void testLogin_Success() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        User user = User.builder()
                .userId(1L)
                .email("test@example.com")
                .name("홍길동")
                .password("encoded_password_123")
                .division("서비스사업본부")
                .team("서비스2팀")
                .position("과장")
                .status(UserStatus.APPROVED)
                .firstLogin(true)
                .loginFailureCount(0)
                .build();

        String accessToken = "test.access.token";
        String refreshToken = "test.refresh.token";

        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password_123")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateAccessToken(1L, "test@example.com")).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(1L, "test@example.com")).thenReturn(refreshToken);

        // when
        String[] result = userService.login(loginRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo(accessToken);
        assertThat(result[1]).isEqualTo(refreshToken);

        // verify
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("password123", "encoded_password_123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtUtil, times(1)).generateAccessToken(1L, "test@example.com");
        verify(jwtUtil, times(1)).generateRefreshToken(1L, "test@example.com");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일로 로그인 시 ApiException이 발생해야 한다")
    void testLogin_UserNotFound() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("notfound@example.com");
        loginRequest.setPassword("password123");

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(java.util.Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage(ApiErrorCode.INVALID_LOGIN.getDescription())
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.INVALID_LOGIN);
                });

        // verify
        verify(userRepository, times(1)).findByEmail("notfound@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호로 로그인 시 ApiException이 발생해야 한다")
    void testLogin_InvalidPassword() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrong_password");

        User user = User.builder()
                .userId(1L)
                .email("test@example.com")
                .password("encoded_password_123")
                .status(UserStatus.APPROVED)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "encoded_password_123")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage(ApiErrorCode.INVALID_LOGIN.getDescription())
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.INVALID_LOGIN);
                });

        // verify
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("wrong_password", "encoded_password_123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 실패 - 승인되지 않은 사용자는 ApiException이 발생해야 한다")
    void testLogin_UserNotApproved() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        User user = User.builder()
                .userId(1L)
                .email("test@example.com")
                .password("encoded_password_123")
                .status(UserStatus.PENDING)  // 승인 전
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password_123")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(ApiException.class)
                .hasMessage(ApiErrorCode.USER_NOT_APPROVED.getDescription())
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.getApiErrorCode()).isEqualTo(ApiErrorCode.USER_NOT_APPROVED);
                });

        // verify
        verify(userRepository, times(1)).findByEmail("test@example.com");
        verify(passwordEncoder, times(1)).matches("password123", "encoded_password_123");
        verify(userRepository, never()).save(any(User.class));
    }
}

