package it.trentosmartmountain.app.data.nfc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NfcTagBus {
    // replay = 1: il tag emesso prima che NfcScanViewModel si iscriva (es. cold start
    // dell'app via intent NFC) non viene perso. Il VM riceve l'ultimo tag al subscribe.
    private val _tagId = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val tagId = _tagId.asSharedFlow()

    fun emit(id: String) { _tagId.tryEmit(id) }

    /** Da chiamare dopo aver consumato il tag, per evitare che venga riconsegnato
     *  a future iscrizioni (es. apertura di un'altra schermata che osserva il bus). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun consume() { _tagId.resetReplayCache() }
}
