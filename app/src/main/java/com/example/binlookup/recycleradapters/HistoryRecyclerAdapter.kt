package com.example.binlookup.recycleradapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.binlookup.databinding.RecyclerviewHistoryItemBinding

class HistoryRecyclerAdapter internal constructor(
    private val items: List<String>,
    private val onClickListener: OnItemClickListener,
) : RecyclerView.Adapter<HistoryRecyclerAdapter.MyViewHolder>(){

    interface OnItemClickListener {
        fun onItemClick(item: String)
    }

    inner class MyViewHolder(private val binding: RecyclerviewHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.tvItem.text = item
        }

        init {
            itemView.setOnClickListener { onClickListener.onItemClick(items[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val listItemBinding = RecyclerviewHistoryItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(listItemBinding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}