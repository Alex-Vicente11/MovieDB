package com.example.apptest.util

sealed class ViewState<out T> {
    object Idle : ViewState<Nothing>()
    object Loading : ViewState<Nothing>()
    data class Success<T>(val data: T) : ViewState<T>()
    data class Error(val message: String) : ViewState<Nothing>()
}

