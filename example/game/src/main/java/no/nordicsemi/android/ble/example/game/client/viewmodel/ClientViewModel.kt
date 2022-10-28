package no.nordicsemi.android.ble.example.game.client.viewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.example.game.client.repository.ClientConnection
import no.nordicsemi.android.ble.example.game.client.repository.ScannerRepository
import no.nordicsemi.android.ble.example.game.quiz.repository.Question
import no.nordicsemi.android.ble.example.game.server.data.ResultToClient
import no.nordicsemi.android.ble.example.game.timer.TimerViewModel
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import javax.inject.Inject

@HiltViewModel
class ClientViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scannerRepository: ScannerRepository,
) : TimerViewModel() {
    private var clientManager: ClientConnection? = null

    private val _question: MutableStateFlow<Question?> = MutableStateFlow(null)
    val question = _question.asStateFlow()

    private val _finalResult: MutableStateFlow<ResultToClient?> = MutableStateFlow(null)
    val finalResult = _finalResult.asStateFlow()

    private val _answer: MutableStateFlow<Int?> = MutableStateFlow(null)
    val answer = _answer.asStateFlow()

    private val _selectedAnswer: MutableState<Int?> = mutableStateOf(null)
    val selectedAnswer: State<Int?> = _selectedAnswer

    private val _state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Initializing)
    val state = _state.asStateFlow()

    init {
            viewModelScope.launch(Dispatchers.IO ) {
            val device = scannerRepository.searchForServer()

            ClientConnection(context, viewModelScope, device )
                .apply {
                    stateAsFlow()
                        .onEach {
                            _state.value = it
                        }
                        .launchIn(viewModelScope)
                    question
                        .onEach {
                            _answer.value = null
                            _selectedAnswer.value = null
                            _question.value = it
                            startCountDown()
                        }
                        .launchIn(viewModelScope)
                    answer
                        .onEach { _answer.value = it }
                        .launchIn(viewModelScope)

                    finalResult
                        .onEach { _finalResult.value = it }
                        .launchIn(viewModelScope)
                }
                .apply {
                    connect()
                    // Send players name
                    sendPlayersName(deviceName)
                }
                .apply { clientManager = this }
        }
    }


    override fun onCleared() {
        super.onCleared()

        clientManager?.release()
        clientManager = null
    }


    fun sendAnswer(answerId: Int) {
        _selectedAnswer.value = answerId

        viewModelScope.launch(Dispatchers.IO) {
            clientManager?.sendSelectedAnswer(answerId)
        }
    }
    fun sendName(playersName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            clientManager?.sendPlayersName(playersName)
        }
    }
}