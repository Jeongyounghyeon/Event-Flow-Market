package io.github.jeongyounghyeon.memberservice.security

import io.github.jeongyounghyeon.memberservice.repository.MemberRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberRepository: MemberRepository,
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val member = memberRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("회원을 찾을 수 없습니다: $email")
        return User.builder()
            .username(member.id.toString())
            .password(member.password ?: "")
            .authorities(SimpleGrantedAuthority("ROLE_${member.role.name}"))
            .build()
    }
}