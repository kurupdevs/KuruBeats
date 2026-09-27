/*
 * KuruBeats (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.kurubeats.app.ads.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import com.kurubeats.app.ads.domain.OpenSupportPageUseCase
import com.kurubeats.app.ads.domain.SupportPageOpenResult
import javax.inject.Inject

internal sealed interface SupportKuruBeatsScreenState {
    @Immutable
    data object Loading : SupportKuruBeatsScreenState

    @Immutable
    data object Success : SupportKuruBeatsScreenState

    @Immutable
    data object Empty : SupportKuruBeatsScreenState

    @Immutable
    data class Error(
        val reason: SupportKuruBeatsError,
    ) : SupportKuruBeatsScreenState
}

internal enum class SupportKuruBeatsError {
    PageUnavailable,
}

internal enum class SupportKuruBeatsUiEvent {
    OpenFailed,
}

@HiltViewModel
internal class SupportKuruBeatsViewModel
    @Inject
    constructor(
        private val openSupportPage: OpenSupportPageUseCase,
    ) : ViewModel() {
        private val _screenState =
            MutableStateFlow<SupportKuruBeatsScreenState>(SupportKuruBeatsScreenState.Success)
        val screenState: StateFlow<SupportKuruBeatsScreenState> = _screenState.asStateFlow()

        private val eventChannel = Channel<SupportKuruBeatsUiEvent>(Channel.BUFFERED)
        val events = eventChannel.receiveAsFlow()

        fun onSupportKuruBeatsClick() {
            if (_screenState.value is SupportKuruBeatsScreenState.Loading) return
            _screenState.value = SupportKuruBeatsScreenState.Loading
            when (openSupportPage()) {
                SupportPageOpenResult.Opened -> {
                    _screenState.value = SupportKuruBeatsScreenState.Success
                }

                SupportPageOpenResult.Unavailable -> {
                    _screenState.value =
                        SupportKuruBeatsScreenState.Error(SupportKuruBeatsError.PageUnavailable)
                    eventChannel.trySend(SupportKuruBeatsUiEvent.OpenFailed)
                }
            }
        }
    }
