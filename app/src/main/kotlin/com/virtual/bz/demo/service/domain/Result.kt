package com.virtual.bz.demo.service.domain

sealed interface Result {
    data class Success(val id: String) : Result
    data class Failure(val cause: String) : Result
}