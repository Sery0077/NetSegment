package sery.vlasenko.netsegment.utils.extensions

import androidx.appcompat.widget.AppCompatTextView
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.model.connections.ConnectionState

fun AppCompatTextView.setConnState(state: ConnectionState) =
    when (state) {
        ConnectionState.IDLE -> {
            setText(R.string.conn_state_idle)
            setBackgroundColor(resources.getColor(R.color.teal_700, null))
        }
        ConnectionState.MEASURE -> {
            setText(R.string.conn_state_measure)
            setBackgroundColor(resources.getColor(R.color.purple_700, null))
        }
    }