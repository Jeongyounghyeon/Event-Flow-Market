package io.github.jeongyounghyeon.memberservice.repository

import io.github.jeongyounghyeon.memberservice.domain.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?
    fun existsByEmail(email: String): Boolean
}