package sery.vlasenko.netsegment.ui.base

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.*
import sery.vlasenko.netsegment.utils.ResourceProvider
import java.lang.Runnable
import kotlin.coroutines.CoroutineContext

abstract class BaseRXViewModel : ViewModel() {
    protected val disposable = CompositeDisposable()

    protected fun getString(@StringRes id: Int): String = ResourceProvider.getString(id)
    protected fun getString(@StringRes id: Int, vararg args: Any): String =
        ResourceProvider.getString(id, *args)

    protected val ioViewModelScope = CoroutineScope(viewModelScope.coroutineContext + Dispatchers.IO)

    override fun onCleared() {
        disposable.clear()
        super.onCleared()
    }
}
