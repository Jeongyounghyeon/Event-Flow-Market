package io.github.jeongyounghyeon.memberservice.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import io.github.jeongyounghyeon.common.exception.BusinessException
import io.github.jeongyounghyeon.common.exception.ErrorCode
import io.github.jeongyounghyeon.memberservice.domain.MemberRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.stereotype.Component
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiry}") private val accessTokenExpiry: Long,
    @Value("\${jwt.refresh-token-expiry}") private val refreshTokenExpiry: Long,
) {
    private val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
    private val encoder = NimbusJwtEncoder(ImmutableSecret(secretKey))
    private val decoder = NimbusJwtDecoder.withSecretKey(secretKey).build()

    fun generateAccessToken(memberId: Long, role: MemberRole): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(memberId.toString())
            .claim("role", role.name)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(accessTokenExpiry))
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    fun generateRefreshToken(memberId: Long): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(memberId.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(refreshTokenExpiry))
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    fun validateAndGetMemberId(token: String): Long {
        return try {
            decoder.decode(token).subject.toLong()
        } catch (e: Exception) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
    }

    fun validateAndGetRole(token: String): String {
        return try {
            decoder.decode(token).getClaim("role")
        } catch (e: Exception) {
            throw BusinessException(ErrorCode.INVALID_TOKEN)
        }
    }
}
