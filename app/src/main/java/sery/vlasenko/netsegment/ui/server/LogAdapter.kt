package sery.vlasenko.netsegment.ui.server

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import sery.vlasenko.netsegment.R
import sery.vlasenko.netsegment.model.LogItem

class LogAdapter : ListAdapter<LogItem, LogAdapter.LogVH>(DIFF_UTIL_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogVH(view)
    }

    override fun onBindViewHolder(holder: LogVH, position: Int) {
        holder.bind(currentList[position])
    }

    class LogVH(view: View) : ViewHolder(view) {
        private val tvTime: TextView = view.findViewById(R.id.tv_time)
        private val tvMessage: TextView = view.findViewById(R.id.tv_message)

        fun bind(log: LogItem) {
            tvTime.text = log.time
            tvMessage.text = log.message
        }
    }
}

object DIFF_UTIL_CALLBACK : DiffUtil.ItemCallback<LogItem>() {
    override fun areItemsTheSame(oldItem: LogItem, newItem: LogItem): Boolean {
        return oldItem.time == newItem.time
    }

    override fun areContentsTheSame(oldItem: LogItem, newItem: LogItem): Boolean {
        return oldItem == newItem
    }
}