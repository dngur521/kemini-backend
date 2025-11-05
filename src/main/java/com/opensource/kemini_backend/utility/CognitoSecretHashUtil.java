package com.opensource.kemini_backend.utility;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cognito SECRET_HASH 계산을 위한 유틸리티 클래스
 * SECRET_HASH = Base64(HMAC-SHA256(Client Secret, Username + Client ID))
 */
public class CognitoSecretHashUtil {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public static String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
        
        // 메시지: Username + Client ID
        String data = userName + userPoolClientId;
        
        try {
            // HMAC-SHA256 인스턴스 초기화
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                userPoolClientSecret.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA256_ALGORITHM
            );
            sha256_HMAC.init(secretKeySpec);
            
            // 해시 계산 및 Base64 인코딩
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            throw new RuntimeException("SECRET_HASH 계산 오류", e);
        }
    }
}