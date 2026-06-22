package com.investment.portal.domain.repository.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.investment.portal.domain.entity.user.UserSocial;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserSocialMapper {
    
    /**
     * 소셜 ID로 조회
     */
    Optional<UserSocial> findBySocialId(@Param("socialId") Long socialId);
    
    /**
     * 사용자 ID로 소셜 계정 목록 조회
     */
    List<UserSocial> findByUserId(@Param("userId") String userId);
    
    /**
     * 사용자 ID와 제공자로 조회
     */
    Optional<UserSocial> findByUserIdAndProvider(
            @Param("userId") String userId, 
            @Param("provider") String provider
    );
    
    /**
     * 제공자와 제공자 사용자 ID로 조회
     */
    Optional<UserSocial> findByProviderAndProviderUserId(
            @Param("provider") String provider, 
            @Param("providerUserId") String providerUserId
    );
    
    /**
     * 소셜 계정 등록
     */
    int insert(UserSocial userSocial);
    
    /**
     * 소셜 계정 정보 수정
     */
    int update(UserSocial userSocial);
    
    /**
     * 토큰 정보 업데이트
     */
    int updateTokenInfo(UserSocial userSocial);
    
    /**
     * 마지막 로그인 일시 업데이트
     */
    int updateLastLoginDt(@Param("socialId") Long socialId);
    
    /**
     * 소셜 계정 삭제 (USE_YN = 'N')
     */
    int delete(@Param("socialId") Long socialId);
    
    /**
     * 소셜 계정 물리 삭제
     */
    int deletePhysical(@Param("socialId") Long socialId);
}
