package sery.vlasenko.netsegment.ui.server.clients

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.model.connections.Connection
import sery.vlasenko.netsegment.model.connections.Protocol

class ConnectionAdapter(
    private val onClick: ClickListener,
): ListAdapter<Connection<*>, ConnectionAdapter.ConnectionVH>(DIFF_UTIL_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_connection, parent, false)
        return ConnectionVH(view)
    }

    override fun onBindViewHolder(holder: ConnectionVH, position: Int) {
        holder.bind(currentList[position])
        holder.btnStartText.setOnClickListener {
            onClick.onStartTestClick(position)
        }
    }

    class ConnectionVH(view: View): ViewHolder(view) {
        private val tvProtocol: TextView = view.findViewById(R.id.item_conn_tv_protocol)
        private val tvIp: TextView = view.findViewById(R.id.item_conn_tv_ip)
        private val tvPort: TextView = view.findViewById(R.id.item_conn_tv_port)
        val btnStartText: Button = view.findViewById(R.id.item_conn_btn_start_test)

        fun bind(conn: Connection<*>) {
            tvIp.text = conn.ip.toString()
            tvPort.text = conn.port.toString()

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
    }
}

object DIFF_UTIL_CALLBACK: DiffUtil.ItemCallback<Connection<*>>() {
    override fun areItemsTheSame(oldItem: Connection<*>, newItem: Connection<*>): Boolean {
        return oldItem.ip == newItem.ip
    }

    override fun areContentsTheSame(oldItem: Connection<*>, newItem: Connection<*>): Boolean {
        return oldItem == newItem
    }
}