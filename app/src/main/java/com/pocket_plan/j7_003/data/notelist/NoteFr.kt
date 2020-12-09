package com.pocket_plan.j7_003.data.notelist

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.SearchView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.pocket_plan.j7_003.MainActivity
import com.pocket_plan.j7_003.R
import com.pocket_plan.j7_003.data.fragmenttags.FT
import com.pocket_plan.j7_003.data.notelist.NoteEditorFr.Companion.noteColor
import com.pocket_plan.j7_003.data.settings.SettingId
import com.pocket_plan.j7_003.data.settings.SettingsManager
import kotlinx.android.synthetic.main.fragment_note.view.*
import kotlinx.android.synthetic.main.row_note.view.*
import java.util.*


/**
 * A simple [Fragment] subclass.
 */

class NoteFr : Fragment() {

    private lateinit var myMenu: Menu

    companion object {
        lateinit var myAdapter: NoteAdapter
        var noteLines = 0
        var noteListInstance: NoteList = NoteList()
        lateinit var myFragment: NoteFr

        var deletedNote: Note? = null

        var searching = false
        lateinit var searchView: SearchView

        lateinit var adjustedList: ArrayList<Note>
        lateinit var lastQuery: String
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_notes, menu)
        myMenu = menu
        adjustedList = arrayListOf()

        //color tint for undo icon
        myMenu.findItem(R.id.item_notes_undo)?.icon?.setTint(MainActivity.act.colorForAttr(R.attr.colorOnBackGround))

        searchView = menu.findItem(R.id.item_notes_search).actionView as SearchView
        val textListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                MainActivity.act.hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (searching) {
                    myFragment.search(newText.toString())
                }
                return true
            }
        }

        searchView.setOnQueryTextListener(textListener)

        val onCloseListener = SearchView.OnCloseListener {
            MainActivity.toolBar.title = getString(R.string.menuTitleNotes)
            searchView.onActionViewCollapsed()
            searching = false
            myAdapter.notifyDataSetChanged()
            true
        }

        searchView.setOnCloseListener(onCloseListener)

        searchView.setOnSearchClickListener {
            MainActivity.toolBar.title = ""
            searching = true
            adjustedList.clear()
            myAdapter.notifyDataSetChanged()
        }

        updateNoteSearchIcon()
        updateNoteUndoIcon()
        super.onCreateOptionsMenu(menu, inflater)

    }

    private fun updateNoteSearchIcon() {
        myMenu.findItem(R.id.item_notes_search).isVisible = noteListInstance.size > 0
    }

    private fun updateNoteUndoIcon() {
        myMenu.findItem(R.id.item_notes_undo).isVisible = deletedNote!=null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    fun search(query: String) {
        if (query == "") {
            adjustedList.clear()
        } else {
            lastQuery = query
            adjustedList.clear()
            noteListInstance.forEach { note ->
                if (note.content.toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT)) ||
                    note.title.toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT))
                ) {
                    adjustedList.add(note)
                }
            }
        }
        myAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_notes_search -> {
                /* no-op, listeners for this view are implemented in onCreateOptionsMenu */
            }

            R.id.item_notes_undo -> {
                //undo deletion of last deleted task
//                val newPos = noteListInstance.addFullNote(deletedNote!!)
//                myAdapter.notifyItemInserted(newPos)

                noteListInstance.addFullNote(deletedNote!!)
                myAdapter.notifyDataSetChanged()

                deletedNote = null
                myFragment.updateNoteUndoIcon()

            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        myFragment = this
        //inflating layout for NoteFragment
        val myView = inflater.inflate(R.layout.fragment_note, container, false)
        deletedNote = null
        initializeComponents(myView)
        return myView
    }

    private fun initializeComponents(myView: View) {
        val noteColumns = SettingsManager.getSetting(SettingId.NOTE_COLUMNS) as String

        val setting = SettingsManager.getSetting(SettingId.NOTE_LINES) as Double
        noteLines = setting.toInt()

        //initialize Recyclerview and Adapter
        val myRecycler = myView.recycler_view_note
        myAdapter = NoteAdapter()
        myRecycler.adapter = myAdapter
        val lm = StaggeredGridLayoutManager(noteColumns.toInt(), 1)
        myRecycler.layoutManager = lm
        myRecycler.setHasFixedSize(true)


        val swipeDirections =
            when (SettingsManager.getSetting(SettingId.NOTES_SWIPE_DELETE) as Boolean) {
                true -> ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                else -> 0
            }
        //itemTouchHelper to drag and reorder notes
        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.END or ItemTouchHelper.START,
                swipeDirections
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: ViewHolder, target: ViewHolder
                ): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition



                    //swap items in list
                    if(fromPos < toPos){
                        val movingNote = noteListInstance[fromPos]
                        for(i in fromPos+1..toPos){
                            noteListInstance[i-1] = noteListInstance[i]
                        }
                        noteListInstance[toPos] = movingNote
                    }
                    if(fromPos > toPos){
                        val movingNote = noteListInstance[fromPos]
                        for(i in fromPos downTo toPos+1){
                            noteListInstance[i] = noteListInstance[i-1]
                        }
                        noteListInstance[toPos] = movingNote
                    }

                    noteListInstance.save()

                    // move item in `fromPos` to `toPos` in adapter.
                    myAdapter.notifyItemMoved(fromPos, toPos)

                    return true // true if moved, false otherwise
                }

                override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
                    // remove from adapter
                    deletedNote = noteListInstance.getNote(viewHolder.adapterPosition)
                    noteListInstance.removeAt(viewHolder.adapterPosition)
                    noteListInstance.save()
                    myAdapter.notifyItemRemoved(viewHolder.adapterPosition)
                    myFragment.updateNoteSearchIcon()
                    myFragment.updateNoteUndoIcon()


                }
            })

        itemTouchHelper.attachToRecyclerView(myRecycler)
    }

}


class NoteAdapter :
    RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {
    private val round = SettingsManager.getSetting(SettingId.SHAPES_ROUND) as Boolean
    private val cr = MainActivity.act.resources.getDimension(R.dimen.cornerRadius)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_note, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {


        val currentNote = when (NoteFr.searching) {
            /**
             * NoteFr is currently in search mode, current note gets grabbed from
             * NoteFr. adjusted list
             */
            true -> NoteFr.adjustedList[position]
            /**
             * NoteFr is currently in normal mode, current note gets grabbed from noteList
             */
            false -> NoteFr.noteListInstance.getNote(position)
        }

        holder.itemView.setOnLongClickListener {
            val animationShake =
                AnimationUtils.loadAnimation(MainActivity.act, R.anim.shake_small)
            holder.itemView.startAnimation(animationShake)
            true
        }

        //EDITING TASK VIA ONCLICK LISTENER ON RECYCLER ITEMS
        holder.itemView.setOnClickListener {
            NoteFr.searching = false
            MainActivity.editNoteHolder = currentNote
            noteColor = NoteFr.noteListInstance.getNote(holder.adapterPosition).color
            MainActivity.act.changeToFragment(FT.NOTE_EDITOR)
            MainActivity.act.hideKeyboard()
        }

        //when title is empty, hide it else show it and set the proper text
        if (currentNote.title == "") {
            holder.tvNoteTitle.visibility = View.GONE
        } else {
            holder.tvNoteTitle.visibility = View.VISIBLE
            holder.tvNoteTitle.text = currentNote.title
        }

        holder.tvNoteContent.text = currentNote.content

        //decide how many lines per note are shown, depending on teh setting noteLines
        if (NoteFr.noteLines == -1) {
            holder.tvNoteContent.maxLines = Int.MAX_VALUE
        } else {
            holder.tvNoteContent.maxLines = NoteFr.noteLines
            holder.tvNoteContent.ellipsize = TextUtils.TruncateAt.END
        }

        holder.cvNoteCard.radius = when (round) {
            true -> cr
            else -> 0f
        }

        holder.itemView.cvNoteCard.setCardBackgroundColor(MainActivity.act.colorForAttr(currentNote.color.colorCode))
    }

    override fun getItemCount(): Int {
        return when (NoteFr.searching) {
            true -> NoteFr.adjustedList.size
            false -> NoteFr.noteListInstance.size
        }
    }

    //one instance of this class will contain one instance of row_task and meta data like position
    //also holds references to views inside the layout
    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNoteTitle: TextView = itemView.tvNoteTitle
        val tvNoteContent: TextView = itemView.tvNoteContent
        var cvNoteCard: CardView = itemView.cvNoteCard
    }


}

