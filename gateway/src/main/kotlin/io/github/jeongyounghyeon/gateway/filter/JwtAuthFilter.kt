package io.github.jeongyounghyeon.gateway.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtDecoder: JwtDecoder,
) : OncePerRequestFilter() {

    private val whitelist = listOf(
        "/api/members/signup",
        "/api/members/login",
        "/api/members/token/refresh",
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (whitelist.any { request.requestURI.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val token = extractBearerToken(request)
        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증 토큰이 필요합니다")
            return
        }

        try {
            val jwt = jwtDecoder.decode(token)
            val memberId = jwt.subject
            val role = jwt.getClaim<String>("role")

            val mutated = MutableHttpServletRequest(request)
            mutated.addHeader("Member-Id", memberId)
            if (role != null) mutated.addHeader("Member-Role", role)
            filterChain.doFilter(mutated, response)
        } catch (e: JwtException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.message)
        }
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader("Authorization") ?: return null
        if (!authorization.startsWith("Bearer ")) return null
        return authorization.removePrefix("Bearer ")
    }
}