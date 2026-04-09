package io.github.jeongyounghyeon.gateway.filter

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.util.Collections

class MutableHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private val customHeaders = mutableMapOf<String, String>()

    fun addHeader(name: String, value: String) {
        customHeaders[name] = value
    }

    override fun getHeader(name: String): String? {
        return customHeaders[name] ?: super.getHeader(name)
    }

    override fun getHeaderNames(): java.util.Enumeration<String> {
        val names = customHeaders.keys.toMutableList()
        val superNames = super.getHeaderNames()
        while (superNames.hasMoreElements()) {
            names.add(superNames.nextElement())
        }
        return Collections.enumeration(names)
    }
}
