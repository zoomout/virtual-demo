package com.virtual.bz.demo.service.domain

sealed interface Result {
    data class Success(val id: String) : Result
    object Failure : Result
}