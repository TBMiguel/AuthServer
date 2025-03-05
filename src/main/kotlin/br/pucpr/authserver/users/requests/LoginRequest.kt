package br.pucpr.authserver.users.requests

data class LoginRequest(
    val email: String?,
    val password: String?
)
