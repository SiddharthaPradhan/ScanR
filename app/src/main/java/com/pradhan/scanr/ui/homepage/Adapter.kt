package com.pradhan.scanr.ui.homepage
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pradhan.scanr.DAO.LocalDAO
import com.pradhan.scanr.model.LocalNote
import com.pradhan.scanr.R
import com.squareup.picasso.Picasso


// Adapter class for RecyclerView
class Adapter(private val mList: ArrayList<LocalNote>, private val onClickListener: OnClickListener, private val onLongClickListener: OnLongClickListener, private val localDAO: LocalDAO, private val context:Context) : RecyclerView.Adapter<Adapter.ViewHolder>(), Filterable {
    var notesListFiltered: ArrayList<LocalNote> = mList


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemModel = notesListFiltered[position]

        val thumbnailUri = localDAO.getImageFromNoteName(context, itemModel.noteName)
        // use picasso over here
        Picasso.get().load(thumbnailUri).placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).resize(100, 100)
            .centerCrop().rotate(90.0F).into(holder.thumbnail)
        holder.noteName.text = itemModel.noteName
        holder.itemView.setOnClickListener{
            onClickListener.onClick(itemModel)
        }
        holder.itemView.setOnLongClickListener {
            onLongClickListener.onClick(itemModel, it)
        }

    }

    override fun getItemCount(): Int {
        return notesListFiltered.size
    }

    fun addItem(note: LocalNote){
        mList.add(note)
        notifyDataSetChanged()
    }

    fun removeItem(note: LocalNote){
        mList.remove(note)
        notifyDataSetChanged()
    }
    fun updateItem(previousNoteName: String, noteName: String, category: String, content: String){
        for (item in mList){
            if(item.noteName == previousNoteName){
                item.noteName = noteName
                item.category = category
                item.content = content
                break
            }
        }
        notifyDataSetChanged()
    }


    // ViewHolder that for individual zodiac symbols
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var  thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        var  noteName: TextView = itemView.findViewById(R.id.note_name)
    }

    class OnClickListener(val clickListener: (note: LocalNote) -> Unit) {
        fun onClick(note: LocalNote) = clickListener(note)
    }

    class OnLongClickListener(val clickListener: (note: LocalNote, view: View) -> Boolean) {
        fun onClick(note: LocalNote, view: View) = clickListener(note, view)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.lowercase() ?: ""
                notesListFiltered = if (charString.isEmpty()) mList else {
                    val filteredList = ArrayList<LocalNote>()
                    mList
                        .filter {
                            // TODO: create seperate filter for category
                            (it.noteName.lowercase().contains(charString)) or
                                    (it.category.lowercase().contains(charString))
                        }
                        .forEach { filteredList.add(it) }
                    filteredList

                }
                return FilterResults().apply { values = notesListFiltered }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {

                notesListFiltered = if (results?.values == null) ArrayList()
                else {
                    notifyDataSetChanged()
                    results.values as ArrayList<LocalNote>
                }
            }
        }
    }

    fun getCategoryFilter(searchQuery: CharSequence?): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchQueryString = searchQuery?.toString()?.lowercase() ?: ""
                val charString = constraint?.toString()?.lowercase() ?: ""
                notesListFiltered = if (charString.isEmpty() || charString == "all") mList else {
                    val filteredList = ArrayList<LocalNote>()
                    mList
                        .filter {
                            (it.noteName.lowercase().contains(searchQueryString)) and
                                    (it.category.lowercase().contains(charString))
                        }
                        .forEach { filteredList.add(it) }
                    filteredList

                }
                return FilterResults().apply { values = notesListFiltered }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {

                notesListFiltered = if (results?.values == null) ArrayList()
                else {
                    notifyDataSetChanged()
                    results.values as ArrayList<LocalNote>
                }
            }
        }
    }

}
