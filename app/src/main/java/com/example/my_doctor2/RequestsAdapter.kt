package com.example.my_doctor2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RequestsAdapter(private val listener: RequestItemClickListener) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {
    private val requests = mutableListOf<MyRequest>()

    interface RequestItemClickListener {
        fun onRequestItemClick(request: MyRequest)
    }

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val doctorNameTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val request = requests[position]
                    listener.onRequestItemClick(request)
                }
            }
        }

        fun bind(request: MyRequest) {
            doctorNameTextView.text = request.doctorName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.request_item, parent, false)
        return RequestViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val currentRequest = requests[position]
        holder.bind(currentRequest)
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    fun submitRequests(requestsList: List<MyRequest>) {
        requests.clear()
        requests.addAll(requestsList)
        notifyDataSetChanged()
    }
}
