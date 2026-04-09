package io.github.jeongyounghyeon.memberservice.service

import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.exception.ErrorCode
import io.github.jeongyounghyeon.memberservice.domain.Member
import io.github.jeongyounghyeon.memberservice.dto.LoginRequest
import io.github.jeongyounghyeon.memberservice.dto.MemberResponse
import io.github.jeongyounghyeon.memberservice.dto.MemberUpdateRequest
import io.github.jeongyounghyeon.memberservice.dto.SignupRequest
import io.github.jeongyounghyeon.memberservice.dto.TokenResponse
import io.github.jeongyounghyeon.memberservice.repository.MemberRepository
import io.github.jeongyounghyeon.memberservice.security.JwtProvider
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional
class MemberServiceImpl(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val redisTemplate: StringRedisTemplate,
) : MemberService {

    override fun signup(request: SignupRequest): MemberResponse {
        if (memberRepository.existsByEmail(request.email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        val member = Member(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            nickname = request.nickname,
        )
        return memberRepository.save(member).toResponse()
    }

    override fun login(request: LoginRequest): TokenResponse {
        val member = memberRepository.findByEmail(request.email)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        if (!passwordEncoder.matches(request.password, member.password)) {
            throw BusinessException(ErrorCode.INVALID_PASSWORD)
        }
        val accessToken = jwtProvider.generateAccessToken(member.id, member.role)
        val refreshToken = jwtProvider.generateRefreshToken(member.id)
        redisTemplate.opsForValue().set(
            refreshTokenKey(member.id),
            refreshToken,
            Duration.ofDays(14),
        )
        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    override fun refresh(refreshToken: String): TokenResponse {
        val memberId = jwtProvider.validateAndGetMemberId(refreshToken)
        val stored = redisTemplate.opsForValue().get(refreshTokenKey(memberId))
            ?: throw BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)
        if (stored != refreshToken) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

        val newAccessToken = jwtProvider.generateAccessToken(member.id, member.role)
        val newRefreshToken = jwtProvider.generateRefreshToken(member.id)
        redisTemplate.opsForValue().set(
            refreshTokenKey(memberId),
            newRefreshToken,
            Duration.ofDays(14),
        )
        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): MemberResponse {
        val member = memberRepository.findByIdOrNull(id)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        return member.toResponse()
    }

    override fun update(id: Long, request: MemberUpdateRequest): MemberResponse {
        val member = memberRepository.findByIdOrNull(id)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        member.updateNickname(request.nickname)
        return member.toResponse()
    }

    private fun refreshTokenKey(memberId: Long) = "refresh_token:$memberId"

    private fun Member.toResponse() = MemberResponse(
        id = id,
        email = email,
        nickname = nickname,
        role = role.name,
    )
}
