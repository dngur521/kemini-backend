package com.opensource.kemini_backend.service;

import com.opensource.kemini_backend.dto.*;
import com.opensource.kemini_backend.model.User;
import com.opensource.kemini_backend.repository.UserRepository;
import com.opensource.kemini_backend.utility.CognitoSecretHashUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.*;

@Service
public class UserService {

    @Value("${aws.cognito.clientId}")
    private String clientId;
    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;
    @Value("${aws.cognito.clientSecret}") 
    private String clientSecret;

    private final CognitoIdentityProviderClient cognitoClient;
    private final UserRepository userRepository;

    public UserService(CognitoIdentityProviderClient cognitoClient, UserRepository userRepository) {
        this.cognitoClient = cognitoClient;
        this.userRepository = userRepository;
    }

    // C: 회원가입 (Cognito SignUp + DB Save)
    public void signUp(SignUpRequestDto signUpRequest) {
        // ... (Cognito User Attributes 정의)
        List<AttributeType> userAttributes = List.of(
            AttributeType.builder().name("email").value(signUpRequest.email()).build(),
            AttributeType.builder().name("name").value(signUpRequest.name()).build(),
            AttributeType.builder().name("phone_number").value(signUpRequest.phoneNumber()).build()
        );

        // SECRET_HASH 계산
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
            clientId, 
            clientSecret, 
            signUpRequest.email() // Username (여기서는 email)
        );

        // Cognito SignUp 요청 객체 생성 (SECRET_HASH 포함)
        SignUpRequest cognitoSignUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .secretHash(secretHash)
                .username(signUpRequest.email()) 
                .password(signUpRequest.password())
                .userAttributes(userAttributes)
                .build();

        try {
            // Cognito에 사용자 등록 (UNCONFIRMED 상태로 생성됨)
            cognitoClient.signUp(cognitoSignUpRequest);

            // ----------------------------------------------------------------
            // 🚨 (이 부분이 핵심) 관리자 권한으로 사용자 즉시 확인
            AdminConfirmSignUpRequest adminConfirmRequest = AdminConfirmSignUpRequest.builder()
                .userPoolId(userPoolId) // @Value로 주입된 User Pool ID
                .username(signUpRequest.email())
                .build();
            
            cognitoClient.adminConfirmSignUp(adminConfirmRequest);

            // ----------------------------------------------------------------
            
            // DB에 부가 정보 저장
            User newUser = User.builder()
                .email(signUpRequest.email())
                .name(signUpRequest.name())
                .phoneNumber(signUpRequest.phoneNumber())
                .status("CONFIRMED") // "UNCONFIRMED" -> "CONFIRMED"로 변경
                .build();
            userRepository.save(newUser);

        } catch (Exception e) {
            // 예외 처리 (이미 존재하는 사용자 등)
            throw new RuntimeException("회원가입 오류: " + e.getMessage());
        }
    }
    
    // C: 계정 확인 (Cognito Confirm) (11/10~ 현재는 사용 안함)
    public void confirmSignUp(ConfirmRequestDto confirmRequest) {

        String username = confirmRequest.email();

        // 🚨 1. SECRET_HASH 계산
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(clientId, clientSecret, username);

        ConfirmSignUpRequest cognitoConfirmRequest = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .secretHash(secretHash) // 🚨 SECRET_HASH 추가
                .username(username)
                .confirmationCode(confirmRequest.confirmationCode())
                .build();

        try {
            cognitoClient.confirmSignUp(cognitoConfirmRequest);
            System.out.println("Cognito Confirm Sign Up 요청: " + cognitoConfirmRequest.toString());

            // DB 상태 업데이트
            User user = userRepository.findByEmail(confirmRequest.email())
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            user.setStatus("CONFIRMED");
            userRepository.save(user);
            
        } catch (Exception e) {
            throw new RuntimeException("계정 확인 오류: " + e.getMessage());
        }
    }

    // R: 사용자 정보 조회 (인증된 사용자용)
    public UserResponseDto getUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
        
        return new UserResponseDto(user.getEmail(), user.getName(), user.getPhoneNumber(), user.getStatus());
    }
    
    // U: 사용자 정보 수정 (UpdateUserRequestDto 필요)
    public void updateUser(String email, UpdateUserRequestDto updateRequest) {

        // 1. RDS DB 정보 수정 및 저장
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // Entity 내부 메서드로 필드 업데이트
        user.updateDetails(updateRequest.name(), updateRequest.phoneNumber());
        userRepository.save(user);

        // 2. 🚨 Cognito 속성 동기화 (Admin API 사용)
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

    // D: 사용자 삭제 (Cognito AdminDeleteUser + DB Delete 필요)
    public void deleteUser(String email) {

        // 1. Cognito 사용자 계정 삭제 (Admin API 사용)
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

        // 2. 🚨 RDS DB 사용자 레코드 삭제
        userRepository.deleteByEmail(email);
    }

    // 로그인
    public InitiateAuthResponse login(LoginRequestDto loginRequest) {
        String username = loginRequest.email();

        // 1. 🚨 SECRET_HASH 계산
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
                clientId,
                clientSecret,
                username);

        // 2. AuthParameters 구성 (USERNAME, PASSWORD, SECRET_HASH 포함)
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", loginRequest.password());
        authParameters.put("SECRET_HASH", secretHash); // 🚨 SECRET_HASH 추가

        // 3. InitiateAuthRequest 객체 생성
        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH) // 일반 인증 흐름 사용
                .authParameters(authParameters) // 🚨 SECRET_HASH 포함된 파라미터 사용
                .build();

        try {
            // 4. Cognito API 호출 및 응답 반환
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
            // 🚨 2. (수정) "Invalid Access Token" 등 토큰이 유효하지 않을 때 발생하는 예외
            // 이 예외는 이미 로그아웃되었거나, 토큰이 만료/위조된 경우 발생합니다.
            // 회원 탈퇴 API의 오류 메시지와 동일한 메시지를 던지도록 수정합니다.
            System.err.println("Cognito Global Sign Out (NotAuthorizedException): " + e.getMessage());
            
            // GlobalExceptionHandler가 이 메시지를 잡아 JSON으로 변환합니다.
            throw new RuntimeException("인증이 필요합니다. 유효한 토큰을 포함하여 요청하십시오.");

        } catch (Exception e) {
            // 🚨 3. (수정) 그 외의 예상치 못한 오류 (예: Cognito 서비스 다운)
            System.err.println("Cognito Global Sign Out (General Error): " + e.getMessage());
            
            // GlobalExceptionHandler가 이 메시지를 잡아 JSON으로 변환합니다.
            throw new RuntimeException("로그아웃 처리 중 서버 오류가 발생했습니다.", e);
        }
    }

    // 토큰 갱신
    public InitiateAuthResponse refreshToken(RefreshTokenRequestDto refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();
        String email = refreshRequest.email();

        // 🚨 SECRET_HASH 계산 (기존 유틸리티 재사용)
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
                clientId,
                clientSecret,
                email // Username (email)
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

    // 비밀번호 재설정 코드 요청
    public void forgotPassword(ForgotPasswordRequestDto request) {
        String email = request.email();

        // SECRET_HASH 계산
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
                clientId,
                clientSecret,
                email
        );

        // Cognito ForgotPassword API 요청 객체 생성
        ForgotPasswordRequest cognitoRequest = ForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(email)
                .secretHash(secretHash)
                .build();

        try {
            cognitoClient.forgotPassword(cognitoRequest);
        } catch (Exception e) {
            // 존재하지 않는 사용자, 미확인 사용자 등
            throw new RuntimeException("비밀번호 재설정 코드 요청 실패: " + e.getMessage());
        }
    }

    // 새 비밀번호로 재설정
    public void confirmForgotPassword(ConfirmForgotPasswordRequestDto request) {
        String email = request.email();

        // SECRET_HASH 계산
        String secretHash = CognitoSecretHashUtil.calculateSecretHash(
                clientId,
                clientSecret,
                email);

        // Cognito ConfirmForgotPassword API 요청 객체 생성
        ConfirmForgotPasswordRequest cognitoRequest = ConfirmForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(email)
                .confirmationCode(request.confirmationCode())
                .password(request.newPassword())
                .secretHash(secretHash)
                .build();

        try {
            cognitoClient.confirmForgotPassword(cognitoRequest);
        } catch (Exception e) {
            // 코드 만료, 잘못된 코드 등
            throw new RuntimeException("비밀번호 재설정 실패: " + e.getMessage());
        }
    }
    
}

