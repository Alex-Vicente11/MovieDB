package com.example.apptest.data.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apptest.data.model.Movie
import com.example.apptest.data.network.NetworkResult
import com.example.apptest.data.repository.MovieRepository
import kotlinx.coroutines.launch

class MovieViewModel: ViewModel() {
    private val repository = MovieRepository()

    private val _movies = MutableLiveData<NetworkResult<List<Movie>>>()
    val movies: LiveData<NetworkResult<List<Movie>>> = _movies

    private val _popularMovies = MutableLiveData<NetworkResult<List<Movie>>>()
    val popularMovies: LiveData<NetworkResult<List<Movie>>> = _popularMovies

    fun searchMovies(query: String) {
        viewModelScope.launch {
            _movies.value = NetworkResult.Loading()

            val result = repository.searchMovies(query)

            _movies.value = result
        }
    }

    fun getPopularMovies() {
        viewModelScope.launch {
            _popularMovies.value = NetworkResult.Loading()

            val result = repository.getPopularMovies()

            _popularMovies.value = result
        }
    }
}