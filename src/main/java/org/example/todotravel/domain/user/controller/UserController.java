package org.example.todotravel.domain.user.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.todotravel.domain.user.dto.request.*;
import org.example.todotravel.domain.user.dto.response.LoginResponseDto;
import org.example.todotravel.domain.user.dto.response.OAuth2SignUpResponseDto;
import org.example.todotravel.domain.user.entity.User;
import org.example.todotravel.domain.user.service.impl.RefreshTokenServiceImpl;
import org.example.todotravel.domain.user.service.impl.UserServiceImpl;
import org.example.todotravel.global.controller.ApiResponse;
import org.example.todotravel.global.jwt.util.JwtTokenizer;
import org.example.todotravel.global.oauth2.CustomOAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {
    private final UserServiceImpl userService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final JwtTokenizer jwtTokenizer;

    // 회원가입
    @PostMapping("/signup")
    public ApiResponse<?> registerUser(@Valid @RequestBody UserRegisterRequestDto dto) {
        User newUser = userService.registerNewUser(dto, passwordEncoder);
        return new ApiResponse<>(true, "회원가입 성공", newUser);
    }

    // OAuth2 첫 가입시 추가 정보 입력 후 로그인 처리
    @PostMapping("/oauth2/additional-info")
    public ApiResponse<?> updateOAuth2UserAdditionalInfo(@RequestBody OAuth2AdditionalInfoRequestDto dto,
                                                         HttpServletResponse response) {
        try {
            User updateUser = userService.updateOAuth2UserAdditionalInfo(dto);

            // accessToken, refreshToken 생성
            String accessToken = jwtTokenizer.issueTokenAndSetCookies(response, updateUser);

            LoginResponseDto loginResponseDto = LoginResponseDto.builder()
                .userId(updateUser.getUserId())
                .nickname(updateUser.getNickname())
                .role(updateUser.getRole().name())
                .accessToken(accessToken)
                .build();

            return new ApiResponse<>(true, "추가 정보 업데이트 성공", loginResponseDto);
        } catch (Exception e) {
            return new ApiResponse<>(false, "추가 정보 업데이트 실패");
        }
    }

    // OAuth2 첫 가입시
    @GetMapping("/oauth2/signup")
    public ApiResponse<?> getOAuth2UserInfo(@RequestParam("token") String userInfoJwt) {
        try {
            Claims claims = jwtTokenizer.parseAccessToken(userInfoJwt);
            String email = claims.getSubject();

            OAuth2SignUpResponseDto oAuth2SignUpResponseDto = userService.getUserIdByEmail(email);

            return new ApiResponse<>(true, "OAuth2 가입 성공", oAuth2SignUpResponseDto);
        } catch (Exception e) {
            return new ApiResponse<>(false, "OAuth2 가입 성공");
        }
    }

    // OAuth2 기존 가입 유저 로그인
    @GetMapping("/oauth2/login")
    public ApiResponse<?> oauth2UserLogin(@RequestParam("token") String userInfoJwt, HttpServletResponse response) {
        try {
            Claims claims = jwtTokenizer.parseAccessToken(userInfoJwt);
            String email = claims.getSubject();

            User user = userService.getUserByEmail(email);

            // accessToken, refreshToken 생성
            String accessToken = jwtTokenizer.issueTokenAndSetCookies(response, user);

            LoginResponseDto loginResponseDto = LoginResponseDto.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .build();

            return new ApiResponse<>(true, "OAuth2 로그인 성공", loginResponseDto);
        } catch (Exception e) {
            return new ApiResponse<>(false, "OAuth2 로그인 실패: " + e.getMessage(), null);
        }
    }

    // 사용자 아이디 중복 검사
    @PostMapping("/check-username")
    public ApiResponse<?> checkUsername(@RequestParam String username) {
        userService.checkDuplicateUsername(username);
        return new ApiResponse<>(true, "아이디 사용 가능", username);
    }

    // 사용자 이메일 중복 검사
    @PostMapping("/check-email")
    public ApiResponse<?> checkEmail(@RequestParam String email) {
        userService.checkDuplicateEmail(email);
        return new ApiResponse<>(true, "이메일 사용 가능", email);
    }

    // 사용자 닉네임 중복 검사
    @PostMapping("/check-nickname")
    public ApiResponse<?> checkNickname(@RequestParam String nickname) {
        userService.checkDuplicateUsername(nickname);
        return new ApiResponse<>(true, "닉네임 사용 가능", nickname);
    }

    // 로그인
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequestDto dto, HttpServletResponse response) {
        User loginUser = userService.checkLoginAvailable(dto.getUsername(), dto.getPassword(), passwordEncoder);

        // accessToken, refreshToken 생성
        String accessToken = jwtTokenizer.issueTokenAndSetCookies(response, loginUser);

        LoginResponseDto loginResponseDto = LoginResponseDto.builder()
            .userId(loginUser.getUserId())
            .nickname(loginUser.getNickname())
            .role(loginUser.getRole().name())
            .accessToken(accessToken)
            .build();

        return new ApiResponse<>(true, "로그인 성공", loginResponseDto);
    }

    // 아이디 찾기
    @PostMapping("/find-username")
    public ApiResponse<?> findUsername(@Valid @RequestBody UsernameRequestDto dto) {
        String username = userService.getUsername(dto);
        return new ApiResponse<>(true, "아이디 찾기 성공", username);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // RefreshToken 쿠키 삭제
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }

        // DB에서 RefreshToken 삭제
        String accessToken = request.getHeader("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
            try {
                Claims claims = jwtTokenizer.parseAccessToken(accessToken);
                Long userId = Long.valueOf((Integer) claims.get("userId"));
                refreshTokenService.deleteRefreshToken(userId);
            } catch (Exception e) {
                log.error("Failed to delete refresh token", e);
            }
        }

        // 클라이언트에게 AccessToken 삭제 지시 (프론트엔드에서 처리)
        return new ApiResponse<>(true, "로그아웃 성공", null);
    }
}
