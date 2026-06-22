package com.investment.portal.domain.repository.user;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.investment.portal.domain.entity.user.User;

import java.util.Optional;

@Mapper
public interface UserMapper {
    
    /**
     * 사용자 ID로 조회
     */
    User findByUserId(@Param("userId") String userId);
    
    /**
     * 이메일로 조회
     */
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 이메일로 조회 (비밀번호 포함 - 일반 로그인 인증용)
     */
    Optional<User> findByEmailWithPassword(@Param("email") String email);
    
    /**
     * 닉네임으로 조회
     */
    Optional<User> findByNickname(@Param("nickname") String nickname);
    
    /**
     * 사용자 등록
     */
    int insert(User user);
    
    /**
     * 사용자 정보 수정
     */
    int update(User user);
    
    /**
     * 마지막 로그인 일시 업데이트
     */
    int updateLastLoginDt(@Param("userId") String userId);
    
    /**
     * 사용자 삭제 (USE_YN = 'N')
     */
    int delete(@Param("userId") String userId);
}
