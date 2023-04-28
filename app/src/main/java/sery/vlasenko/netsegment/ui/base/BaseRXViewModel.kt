package sery.vlasenko.netsegment.ui.base

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import sery.vlasenko.netsegment.utils.ResourceProvider

abstract class BaseRXViewModel: ViewModel() {
    protected val disposable = CompositeDisposable()

    protected fun getString(@StringRes id: Int): String = ResourceProvider.getString(id)
    protected fun getString(@StringRes id: Int, vararg args: Any): String = ResourceProvider.getString(id, *args)

    override fun onCleared() {
        disposable.clear()
        super.onCleared()
    }
}
