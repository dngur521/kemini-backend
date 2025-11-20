package com.opensource.kemini_backend.service;

import com.opensource.kemini_backend.dto.*;
import com.opensource.kemini_backend.model.SecurityQuestion;
import com.opensource.kemini_backend.model.User;
import com.opensource.kemini_backend.repository.SecurityQuestionRepository;
import com.opensource.kemini_backend.repository.UserRepository;
import com.opensource.kemini_backend.utility.CognitoSecretHashUtil;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    @Value("${aws.cognito.clientId}")
    private String clientId;
    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;
    @Value("${aws.cognito.clientSecret}") 
    private String clientSecret;

    private final CognitoIdentityProviderClient cognitoClient;
    private final UserRepository userRepository;
    private final SecurityQuestionRepository questionRepository;

    // 생성자
    public UserService(
        CognitoIdentityProviderClient cognitoClient,
        UserRepository userRepository,
        SecurityQuestionRepository questionRepository) {
        this.cognitoClient = cognitoClient;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }

    // 전화번호를 +82 국제 표준 형식으로 변환하는 헬퍼 메서드
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            // (필수 값이므로) 혹은 null을 반환하는 대신 예외를 던질 수도 있습니다.
            return phoneNumber; 
        }

        // 혹시 모를 하이픈(-) 제거
        String digits = phoneNumber.replaceAll("-", "");

        // 이미 +82로 시작하는 올바른 형식인가?
        if (digits.startsWith("+82")) {
            return digits;
        }

        // 010, 011 등 '0'으로 시작하는 한국 형식인가?
        if (digits.startsWith("01")) {
            // 맨 앞의 '0'을 제거하고 '+82'를 붙입니다.
            return "+82" + digits.substring(1);
        }

        // 그 외의 형식은 Cognito가 어차피 거부할 것이므로 그대로 반환
        return digits;
    }

    // 회원가입 메서드
    public void signUp(SignUpRequestDto signUpRequest) {
        
        // 전화번호 변환
        String normalizedPhone = normalizePhoneNumber(signUpRequest.phoneNumber());

        // 변환된 번호로 Cognito 속성 생성
        List<AttributeType> userAttributes = List.of(
            AttributeType.builder().name("email").value(signUpRequest.email()).build(),
            AttributeType.builder().name("name").value(signUpRequest.name()).build(),
            AttributeType.builder().name("phone_number").value(normalizedPhone).build()
        );

        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
            clientId, 
            clientSecret, 
            signUpRequest.email()
        );

        SignUpRequest cognitoSignUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .secretHash(secretHash)
                .username(signUpRequest.email()) 
                .password(signUpRequest.password())
                .userAttributes(userAttributes)
                .build();

        try {
            cognitoClient.signUp(cognitoSignUpRequest);
            
            AdminConfirmSignUpRequest adminConfirmRequest = AdminConfirmSignUpRequest.builder()
                .userPoolId(userPoolId)
                .username(signUpRequest.email())
                .build();
            cognitoClient.adminConfirmSignUp(adminConfirmRequest);
            
            //DB에도 변환된 번호로 저장 (데이터 일관성)
            User newUser = User.builder()
                .email(signUpRequest.email())
                .name(signUpRequest.name())
                .phoneNumber(normalizedPhone)
                .status("CONFIRMED") 
                .askId(signUpRequest.askId())
                .askAnswer(signUpRequest.askAnswer())
                .build();
            userRepository.save(newUser);

        } catch (Exception e) {
            throw new RuntimeException("회원가입 오류: " + e.getMessage());
        }
    }
    
    // 아이디(이메일) 찾기 (보안 질문 기반)
    public String findEmailByQuestion(FindEmailRequestDto request) {
        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        // DB에서 3가지 정보가 일치하는 사용자를 찾음
        User user = userRepository.findByPhoneNumberAndAskIdAndAskAnswer(
                normalizedPhone,
                request.askId(),
                request.askAnswer()).orElseThrow(() -> new RuntimeException("일치하는 사용자 정보가 없습니다.")); // 없으면 예외

        // 있으면 이메일 반환
        return user.getEmail();
    }


    // 비밀번호 찾기 1단계: 이메일로 askId 조회
    @Transactional(readOnly = true)
    public Long findAskIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("가입되지 않은 아이디입니다."));
        return user.getAskId();
    }

    // 비밀번호 찾기 2단계: 질문 답변 검증
    @Transactional(readOnly = true)
    public void verifySecurityQuestion(String email, Long askId, String askAnswer) {
        // 사용자가 존재하는지 확인 (없으면 예외 발생)
        userRepository.findByEmailAndAskIdAndAskAnswer(email, askId, askAnswer)
                .orElseThrow(() -> new RuntimeException("답변이 일치하지 않습니다."));
    }

    // 비밀번호 재설정 (3단계): 전화번호 검증 로직을 제거하고 이메일+질문 만으로 검증
    public void resetPasswordByQuestion(ResetPasswordByQuestionRequestDto request) {
        // 1. 전화번호 없이 이메일+질문+답변으로만 사용자 조회
        User user = userRepository.findByEmailAndAskIdAndAskAnswer(
                request.email(),
                request.askId(),
                request.askAnswer()
        ).orElseThrow(() -> new RuntimeException("입력한 정보가 일치하지 않습니다."));

        // 2. Cognito 비밀번호 강제 재설정 (기존 동일)
        AdminSetUserPasswordRequest adminSetPasswordRequest = AdminSetUserPasswordRequest.builder()
            .userPoolId(userPoolId)
            .username(request.email())
            .password(request.newPassword())
            .permanent(true)
            .build();

        try {
            cognitoClient.adminSetUserPassword(adminSetPasswordRequest);
        } catch (Exception e) {
            throw new RuntimeException("Cognito 비밀번호 재설정 실패: " + e.getMessage());
        }
    }

    // 사용자 정보 조회 (인증된 사용자용)
    public UserResponseDto getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다.")); // 예외처리
        
        return new UserResponseDto(user.getEmail(), user.getName(), user.getPhoneNumber(), user.getStatus());
    }
    
    // 사용자 정보 수정 (UpdateUserRequestDto 필요)
    public void updateUser(String email, UpdateUserRequestDto updateRequest) {

        // DB 정보 수정 및 저장
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // Entity 내부 메서드로 필드 업데이트
        user.updateDetails(updateRequest.name(), updateRequest.phoneNumber());
        userRepository.save(user);

        // Cognito 속성 동기화 (Admin API 사용)
        List<AttributeType> attributesToUpdate = new ArrayList<>();

        if (updateRequest.name() != null) {
            attributesToUpdate.add(AttributeType.builder().name("name").value(updateRequest.name()).build());
        }
        if (updateRequest.phoneNumber() != null) {
            attributesToUpdate
                    .add(AttributeType.builder().name("phone_number").value(updateRequest.phoneNumber()).build());
        }

        if (!attributesToUpdate.isEmpty()) {
            AdminUpdateUserAttributesRequest cognitoUpdateReq = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(userPoolId) // @Value로 주입받은 userPoolId
                    .username(email)
                    .userAttributes(attributesToUpdate)
                    .build();

            try {
                cognitoClient.adminUpdateUserAttributes(cognitoUpdateReq);
            } catch (Exception e) {
                // Cognito 업데이트 실패 시 DB 롤백 또는 로깅 처리 필요 (운영 시점)
                throw new RuntimeException("Cognito 속성 업데이트 실패: " + e.getMessage());
            }
        }
    }

    // 사용자 삭제 (Cognito AdminDeleteUser + DB Delete 필요)
    public void deleteUser(String email) {

        // Cognito 사용자 계정 삭제 (Admin API 사용)
        // 서버에서 관리자 권한으로 삭제하므로 사용자 토큰이 필요 없습니다.
        AdminDeleteUserRequest cognitoDeleteReq = AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId) // @Value로 주입된 User Pool ID
                .username(email)
                .build();

        try {
            cognitoClient.adminDeleteUser(cognitoDeleteReq);
        } catch (Exception e) {
            // Cognito에서 사용자를 찾지 못했더라도, DB에서는 삭제를 시도합니다.
            System.err.println("Cognito 사용자 삭제 실패: " + e.getMessage());
        }

        // DB 사용자 레코드 삭제
        userRepository.deleteByEmail(email);
    }

    // 로그인
    public InitiateAuthResponse login(LoginRequestDto loginRequest) {
        String username = loginRequest.email();

        // SECRET_HASH 계산
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
                clientId,
                clientSecret,
                username);

        // AuthParameters 구성 (USERNAME, PASSWORD, SECRET_HASH 포함)
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", loginRequest.password());
        authParameters.put("SECRET_HASH", secretHash); // 🚨 SECRET_HASH 추가

        // InitiateAuthRequest 객체 생성
        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH) // 일반 인증 흐름 사용
                .authParameters(authParameters) // 🚨 SECRET_HASH 포함된 파라미터 사용
                .build();

        try {
            // Cognito API 호출 및 응답 반환
            return cognitoClient.initiateAuth(authRequest);
        } catch (Exception e) {
            // 잘못된 ID/PW, 계정 미확인 등 인증 실패 처리
            throw new RuntimeException("로그인 실패: " + e.getMessage());
        }
    }

    // 로그아웃: 사용자 세션을 무효화하고 토큰을 취소합니다.
    public GlobalSignOutResponse globalSignOut(String accessToken) {

        GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                .accessToken(accessToken) // 무효화할 Access Token
                .build();

        try {
            // Cognito API 호출: 세션 무효화
            return cognitoClient.globalSignOut(signOutRequest);

        } catch (NotAuthorizedException e) {
            // "Invalid Access Token" 등 토큰이 유효하지 않을 때 발생하는 예외
            // 이 예외는 이미 로그아웃되었거나, 토큰이 만료/위조된 경우 발생합니다.
            // 회원 탈퇴 API의 오류 메시지와 동일한 메시지를 던지도록 수정합니다.
            System.err.println("Cognito Global Sign Out (NotAuthorizedException): " + e.getMessage());
            
            // GlobalExceptionHandler가 이 메시지를 잡아 JSON으로 변환합니다.
            throw new RuntimeException("인증이 필요합니다. 유효한 토큰을 포함하여 요청하십시오.");

        } catch (Exception e) {
            // 그 외의 예상치 못한 오류 (예: Cognito 서비스 다운)
            System.err.println("Cognito Global Sign Out (General Error): " + e.getMessage());
            
            // GlobalExceptionHandler가 이 메시지를 잡아 JSON으로 변환합니다.
            throw new RuntimeException("로그아웃 처리 중 서버 오류가 발생했습니다.", e);
        }
    }

    // 토큰 갱신
    public InitiateAuthResponse refreshToken(RefreshTokenRequestDto refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();
        String email = refreshRequest.email();

        // SECRET_HASH 계산 (기존 유틸리티 재사용)
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
                clientId,
                clientSecret,
                email
        );

        // AuthParameters 구성 (REFRESH_TOKEN, SECRET_HASH 포함)
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("REFRESH_TOKEN", refreshToken);
        authParameters.put("SECRET_HASH", secretHash);

        // InitiateAuthRequest 객체 생성
        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .authParameters(authParameters)
                .build();

        try {
            // Congnito API 호출
            return cognitoClient.initiateAuth(authRequest);
        } catch (Exception e) {
            // 오류는 GlobalExceptionHandler가 처리
            throw new RuntimeException("토큰 갱신 실패: " + e.getMessage());
        }
    }

    // 로그인된 사용자의 비밀번호 변경
    public void changePassword(String accessToken, ChangePasswordRequestDto request) {
        
        // Cognito API 요청 객체 생성
        ChangePasswordRequest cognitoRequest = ChangePasswordRequest.builder()
            .accessToken(accessToken) // 필터가 아닌 Controller에서 받은 Access Token
            .previousPassword(request.currentPassword()) // 현재 비밀번호
            .proposedPassword(request.newPassword())      // 새 비밀번호
            .build();

        try {
            // Cognito API 호출
            cognitoClient.changePassword(cognitoRequest);
        } catch (Exception e) {
            // (예: 현재 비밀번호가 틀렸을 때 NotAuthorizedException 발생)
            throw new RuntimeException("비밀번호 변경 실패: " + e.getMessage());
        }
    }
    
    // 보안 질문 목록 전체 조회
    public List<SecurityQuestionResponseDto> getSecurityQuestions() {
        // 1. DB에서 모든 질문을 찾음
        List<SecurityQuestion> questions = questionRepository.findAllByOrderByIdAsc();
        
        // 2. DTO 리스트로 변환하여 반환
        return questions.stream()
            .map(question -> new SecurityQuestionResponseDto(
                question.getId(), 
                question.getQuestionText()
            ))
            .collect(Collectors.toList());
    }

    // 아이디(이메일) 중복 확인
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public String checkEmailAvailability(String email) {
        // DB에서 이메일 조회
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // 이미 존재하면, 예외를 발생시킴
            // (GlobalExceptionHandler가 400 Bad Request로 처리)
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        } else {
            // 존재하지 않으면, 성공 메시지 반환
            return "사용 가능한 아이디입니다.";
        }
    }

    // 전화번호로 회원을 찾고, 보안 질문 ID(askId)를 반환
    @Transactional(readOnly = true)
    public Long findAskIdByPhoneNumber(String phoneNumber) {
        // 1. 전화번호 정규화 (010 -> +8210)
        String normalizedPhone = normalizePhoneNumber(phoneNumber); //

        // 2. DB 조회
        User user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new RuntimeException("가입되지 않은 전화번호입니다."));

        // 3. askId 반환
        return user.getAskId();
    }
    
}