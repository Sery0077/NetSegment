package sery.vlasenko.netsegment.ui.server.connections

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.model.LogItem
import sery.vlasenko.netsegment.model.connections.Protocol
import sery.vlasenko.netsegment.utils.TimeConst
import sery.vlasenko.netsegment.ui.server.log.LogAdapter

class ConnectionAdapter(
    private val clickListener: ClickListener,
) : ListAdapter<ConnectionItem, ConnectionAdapter.ConnectionVH>(DIFF_UTIL_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionVH {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_connection, parent, false)
        return ConnectionVH(view, LogAdapter())
    }

    override fun onBindViewHolder(holder: ConnectionVH, position: Int) {
        holder.setLogList(currentList[position].logs)
        holder.bind(currentList[position])
        with(holder) {
            btnStart.setOnClickListener { clickListener.onStartTestClick(position) }
            btnStop.setOnClickListener { clickListener.onStopTestClick(position) }
            btnResult.setOnClickListener { clickListener.onResultClick(position) }
        }
    }

    class ConnectionVH(view: View, val logAdapter: LogAdapter) : ViewHolder(view) {
        private val tvProtocol: TextView = view.findViewById(R.id.item_conn_tv_protocol)
        private val tvIp: TextView = view.findViewById(R.id.item_conn_tv_ip)
        private val tvPort: TextView = view.findViewById(R.id.item_conn_tv_port)
        private val tvPing: TextView = view.findViewById(R.id.item_conn_tv_ping)
        private val rvLog: RecyclerView = view.findViewById(R.id.item_conn_rv_logs)

        val btnStart: Button = view.findViewById(R.id.item_conn_btn_start_test)
        val btnStop: Button = view.findViewById(R.id.item_conn_btn_stop_test)
        val btnResult: Button = view.findViewById(R.id.item_conn_btn_show_result)

//        var isScrolling = false

//        private val touchListener = object : RecyclerView.OnItemTouchListener {
//            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
//                if (e.action == MotionEvent.ACTION_MOVE) {
//                    isScrolling = true
//                    rv.parent.requestDisallowInterceptTouchEvent(true)
//                } else if (e.action == MotionEvent.ACTION_CANCEL) {
//                    isScrolling = false
//                }
//                return false
//            }
//            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
//            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
//        }

//        init {
//            with(rvLog) {
//                addOnItemTouchListener(touchListener)
//            }
//        }

        fun setLogList(logList: List<LogItem>) {
            logAdapter.submitList(logList)
        }

        fun bind(conn: ConnectionItem) {
            rvLog.adapter = logAdapter
            rvLog.scrollToPosition(logAdapter.itemCount - 1)

            tvIp.text = conn.ip
            tvPort.text = conn.port

            val ping = conn.ping

            tvPing.text = if (ping > TimeConst.PING_TIMEOUT) {
                itemView.context.getString(R.string.ping_pattern, "-")
            } else {
                itemView.context.getString(R.string.ping_pattern, conn.ping.toString())
            }

            tvPing.text = itemView.context.getString(R.string.ping_pattern, conn.ping.toString())

            when (conn.state) {
                ConnectionItemState.IDLE -> {
                    btnStart.isEnabled = true
                    btnStop.isEnabled = false
                }
                ConnectionItemState.TESTING -> {
                    btnStart.isEnabled = false
                    btnStop.isEnabled = true
                }
            }

            btnResult.isEnabled = conn.isResultAvailable

            with(tvProtocol) {
                when (conn.protocol) {
                    Protocol.UDP -> setBackgroundColor(resources.getColor(R.color.teal_700))
                    Protocol.TCP -> setBackgroundColor(resources.getColor(R.color.purple_700))
                }
                text = conn.protocol.name
            }
        }
    }

    interface ClickListener {
        fun onStartTestClick(pos: Int)
        fun onStopTestClick(pos: Int)
        fun onResultClick(pos: Int)
    }
}

object DIFF_UTIL_CALLBACK : DiffUtil.ItemCallback<ConnectionItem>() {
    override fun areItemsTheSame(oldItem: ConnectionItem, newItem: ConnectionItem): Boolean {
        return oldItem.ip == newItem.ip
    }

    override fun areContentsTheSame(oldItem: ConnectionItem, newItem: ConnectionItem): Boolean {
        return oldItem == newItem
    }
}