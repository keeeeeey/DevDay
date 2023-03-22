package com.example.userservice.service;

import com.example.userservice.dto.request.*;
import com.example.userservice.dto.response.TokenResponseDto;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.entity.EmailAuth;
import com.example.userservice.entity.User;
import com.example.userservice.exception.ApiException;
import com.example.userservice.exception.ExceptionEnum;
import com.example.userservice.repository.EmailAuthRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    private final EmailAuthRepository emailAuthRepository;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    private final JWTUtil jwtUtil;

    private final RedisService redisService;

    @Override
    @Transactional
    public void join(Long emailAuthId, SignUpRequestDto requestDto) {

        EmailAuth emailAuth = emailAuthRepository.findById(emailAuthId)
                .orElseThrow(() -> new ApiException(ExceptionEnum.EMAIL_NOT_ACCEPT_EXCEPTION));

        if (!emailAuth.getIsChecked()) throw new ApiException(ExceptionEnum.EMAIL_NOT_ACCEPT_EXCEPTION);

        User user = User.from(requestDto);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto login(LoginRequestDto requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new ApiException(ExceptionEnum.MEMBER_NOT_EXIST_EXCEPTION));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new ApiException(ExceptionEnum.PASSWORD_NOT_MATCHED_EXCEPTION);
        }

        String accessToken = jwtUtil.createToken(user.getId());
        String refreshToken = jwtUtil.createRefreshToken(user.getId());

        redisService.setValues(refreshToken, user.getId());

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String findId(FindIdRequestDto requestDto) {
        User user = userRepository.findByNameAndNickname(requestDto.getName(), requestDto.getNickname())
                .orElseThrow(() -> new ApiException(ExceptionEnum.MEMBER_NOT_EXIST_EXCEPTION));
        return user.getEmail();
    }

    @Override
    @Transactional
    public void findPw(FindPwRequestDto requestDto) {
        User user = userRepository.findByNameAndNicknameAndEmail(
                        requestDto.getName(),
                        requestDto.getNickname(),
                        requestDto.getEmail()
                )
                .orElseThrow(() -> new ApiException(ExceptionEnum.MEMBER_INFO_NOT_MATCHED_EXCEPTION));

        String tempPw = UUID.randomUUID().toString();

        user.updatePassword(passwordEncoder.encode(tempPw));
        emailService.send(user.getEmail(), "임시 비밀번호 발송", tempPw);
    }

    @Override
    @Transactional(readOnly = true)
    public Long emailCheck(String email) {

        // 이메일 중복 체크
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) throw new ApiException(ExceptionEnum.MEMBER_EXIST_EXCEPTION);

        EmailAuth emailAuth = EmailAuth.builder()
                .email(email)
                .authToken(UUID.randomUUID().toString())
                .build();

        EmailAuth saveEmailAuth = emailAuthRepository.save(emailAuth);

        // 인증 이메일 보내기
        emailService.send(email, "이메일 인증 코드 발송", saveEmailAuth.getAuthToken());

        return saveEmailAuth.getId();
    }

    @Override
    @Transactional
    public void confirmEmail(EmailAuthRequestDto requestDto) {
        EmailAuth emailAuth = emailAuthRepository.findById(requestDto.getId())
                .orElseThrow(() -> new ApiException(ExceptionEnum.EMAIL_AUTH_NOT_FOUNT_EXCEPTION));

        if (emailAuth.getExpireDate().isBefore(LocalDateTime.now())) {
            throw new ApiException(ExceptionEnum.EMAIL_ACCEPT_TIMEOUT_EXCEPTION);
        }

        String authToken = requestDto.getAuthToken();

        if (!authToken.equals(emailAuth.getAuthToken())) {
            throw new ApiException(ExceptionEnum.CODE_NOT_MATCHED_EXCEPTION);
        }

        emailAuth.check();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ExceptionEnum.MEMBER_NOT_EXIST_EXCEPTION));
        return UserResponseDto.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponseDto refresh(HttpServletRequest request) {
        String accessToken = jwtUtil.resolveToken(request, "Authorization");
        String refreshToken = jwtUtil.resolveToken(request, "RefreshToken");

        Long userId = tokenValidation(accessToken, refreshToken);

        String newAccessToken = jwtUtil.createToken(userId);
        String newRefreshToken = jwtUtil.createRefreshToken(userId);

        redisService.setValues(newRefreshToken, userId);

        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private Long tokenValidation(String accessToken, String refreshToken) {

        // 리프레쉬 토큰과 액세스 토큰 null 체크
        if (accessToken == null || refreshToken == null) {
            log.error("accessToken or refreshToken null");
            throw new ApiException(ExceptionEnum.MEMBER_ACCESS_EXCEPTION);
        }

        Long accessTokenPk = Long.parseLong(jwtUtil.getUserPk(accessToken));
        Long refreshTokenPk = Long.parseLong(jwtUtil.getUserPk(refreshToken));

        // 리프레쉬 토큰 pk값이랑 액세스 토큰 pk값이 같은지 비교
        if (!accessTokenPk.equals(refreshTokenPk)) {
            log.error("accessTokenPk and refreshTokenPk not equals");
            throw new ApiException(ExceptionEnum.MEMBER_ACCESS_EXCEPTION);
        }

        // 리프레쉬 토큰 유효성 검사 - 만료시 에러
        if (!jwtUtil.validateToken(refreshToken)) {
            log.error("refreshToken not valid");
            throw new ApiException(ExceptionEnum.MEMBER_ACCESS_EXCEPTION);
        }

        String refreshTokenRedis = redisService.getValues(refreshTokenPk);

        // 헤더 리프레쉬 토큰과 레디스 리프레쉬 토큰 동등성 비교
        if (!refreshToken.equals(refreshTokenRedis)) {
            log.error("accessToken and refreshToken not equals");
            throw new ApiException(ExceptionEnum.MEMBER_ACCESS_EXCEPTION);
        }

        // 액세스 토큰 유효성 검사 - 통과했을 때 해킹으로 간주
        if (jwtUtil.validateToken(accessToken)) {
            log.error("accessToken is valid");
            throw new ApiException(ExceptionEnum.MEMBER_ACCESS_EXCEPTION);
        }

        return accessTokenPk;
    }

}
