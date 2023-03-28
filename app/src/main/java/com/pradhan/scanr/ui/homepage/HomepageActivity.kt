package com.pradhan.scanr.ui.homepage

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pradhan.scanr.DAO.LocalDAO
import com.pradhan.scanr.R
import com.pradhan.scanr.databinding.ActivityHomescreenBinding
import com.pradhan.scanr.databinding.ViewNoteAlertDialogBinding
import com.pradhan.scanr.model.LocalNote
import com.pradhan.scanr.ui.result.ResultActivity
import com.pradhan.scanr.utility.IntentExtraCodes
import com.pradhan.scanr.viewModel.HomepageViewModel
import com.squareup.picasso.Picasso
import jp.wasabeef.blurry.Blurry
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*


class HomepageActivity: AppCompatActivity() {
    private lateinit var binding: ActivityHomescreenBinding
    val APPLICATION_TAG : String = "SCANR-LOG"
    lateinit var searchResultsListView: RecyclerView
    lateinit var adapter: Adapter
    lateinit var noteList: ArrayList<LocalNote>
    lateinit var addFab: ExtendedFloatingActionButton
    lateinit var importFromGalleryFab:FloatingActionButton
    lateinit var importFromCameraFab:FloatingActionButton
    private lateinit var importFromGalleryText: TextView
    lateinit var importFromCameraText: TextView
    lateinit var categoryChipGroup: ChipGroup
    lateinit var viewModel: HomepageViewModel
    lateinit var searchView: SearchView
    var alertDialog: AlertDialog? = null

    /**
     * Done 1: Alert Dialog Viewing the image and content (DO THIS TONIGHT)
     * Done 2: Add View Model to all activities (DO THIS TONIGHT)
     * Done 3: Create alt layouts for horizontal
     * Done 4: Implement the OCR
     * TODO IDK: add placeholder when app is empty
     * Done : Add ability to sign out
     * NOPE 5: Implement Google Drive Integration (Maybe not)
     * Done 6: Fix FAB https://stackoverflow.com/questions/60122776/floating-action-button-background-blur (DO THIS TONIGHT)
     * TODO 7: Fix the overall looks and add finishing touches
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomescreenBinding.inflate(layoutInflater)
        // view model instance
        viewModel = ViewModelProvider(this)[HomepageViewModel::class.java]
        // TODO connect with ViewModel
        noteList = viewModel.localDAO.getAllNotes(this)
        searchResultsListView = binding.searchResultList
        searchResultsListView.layoutManager = LinearLayoutManager(this)
        if (noteList.size == 0){
            // show placeholder
        }
        adapter = Adapter(noteList,
            Adapter.OnClickListener { localNote ->
                // TODO: make intent or create alert dialog or something
                alertInputDialog(localNote)
                Toast.makeText(this@HomepageActivity, localNote.noteName, Toast.LENGTH_SHORT).show()
        },
            Adapter.OnLongClickListener { localNote, view ->
                val p = PopupMenu(this@HomepageActivity, view)
                val inflater = p.menuInflater
                inflater.inflate(R.menu.popup_menu, p.menu)
                p.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.deleteMenuButton -> {
                            // delete item here
                            viewModel.localDAO.deleteNote(this,localNote) // delete from storage
                            adapter.removeItem(localNote) // remove from RV
                            true
                        }
                        R.id.editMenuButton -> {
                            // start edit here
                            launchEditActivity(localNote.noteName, localNote.category, localNote.content)
                            true
                        }
                        else -> {
                            true
                        }
                    }

                }
                p.show()
                true
            },viewModel.localDAO, this)
        searchResultsListView.adapter = adapter

        addFab = binding.addFab
        importFromCameraFab = binding.addCameraFab
        importFromCameraText = binding.addCameraActionText
        importFromGalleryFab = binding.addGalleryFab
        importFromGalleryText = binding.addGalleryActionText
        categoryChipGroup = binding.chipGroup
        categoryChipGroup.setOnCheckedStateChangeListener(categoryChipListener)
        addFab.shrink() // initially set FAB to be small

        addFab.setOnClickListener{

            handleFabClick()

        }

        importFromCameraFab.setOnClickListener{
            handleCameraButtonClick()
        }
        importFromGalleryFab.setOnClickListener {
            handleGalleryButtonClick()
        }
        setSupportActionBar(binding.topAppBar)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        alertDialog?.dismiss()
        super.onDestroy()
    }

    private fun handleCameraButtonClick(){
        viewModel.latestNoteThumbnailImage = viewModel.localDAO.newLocalNote(this)
        cameraLauncher.launch(viewModel.latestNoteThumbnailImage)
        Toast.makeText(applicationContext,"Clicked Camera", Toast.LENGTH_SHORT).show()
    }

    private fun handleGalleryButtonClick(){
        viewModel.latestNoteThumbnailImage = viewModel.localDAO.newLocalNote(this)
        galleryLauncher.launch("image/*")
        Toast.makeText(applicationContext,"Clicked Gallery", Toast.LENGTH_SHORT).show()
    }

    private fun handleFabClick(){
        if (!viewModel.isAllFabVisible) {
            Blurry.with(this).radius(25).animate().sampling(2).capture(binding.containerForBlur).into(binding.mainBlurFL)
            importFromCameraFab.show();
            importFromGalleryFab.show();
            importFromCameraText.visibility = View.VISIBLE;
            importFromGalleryText.visibility = View.VISIBLE;
            addFab.extend();
            viewModel.isAllFabVisible = true;
        } else {
            binding.mainBlurFL.setImageDrawable(null)
            importFromCameraFab.hide();
            importFromGalleryFab.hide();
            importFromCameraText.visibility = View.GONE;
            importFromGalleryText.visibility = View.GONE;
            addFab.shrink();
            viewModel.isAllFabVisible = false;
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar,menu)
        val searchViewMenuItem = menu?.findItem(R.id.search)
        searchView = MenuItemCompat.getActionView(searchViewMenuItem) as SearchView
        val searchEditText =
            searchView.findViewById<View>(androidx.appcompat.R.id.search_src_text) as EditText
        searchEditText.setTextColor(resources.getColor(R.color.white))
        searchEditText.setHintTextColor(resources.getColor(R.color.white))
        searchView.queryHint = "Search for notes here"
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter.filter(query)
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
    }


    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        if (it == null) {
            // Delete last created temp file
            viewModel.localDAO.deleteLastNote(this)
            Toast.makeText(this@HomepageActivity, "No image selected from gallery", Toast.LENGTH_SHORT).show()
        } else {
            // image was selected, start the new activity with the URI
            val inputStream: InputStream? = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 0, bos) // YOU can also save it in JPEG
            val bitmapData: ByteArray = bos.toByteArray()
            viewModel.localDAO.addImageToNote(this, viewModel.localDAO.getLastNoteName(), bitmapData)
            launchResultActivity(viewModel.localDAO.getLastNoteName())
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { takenPicture: Boolean ->
    if (takenPicture){ // also saved to uri
        Toast.makeText(this@HomepageActivity, "Just took a photo", Toast.LENGTH_SHORT).show()
        launchResultActivity(viewModel.localDAO.getLastNoteName())
        }
    }

    private fun launchResultActivity(noteName: String){
        val resultIntent = Intent(this, ResultActivity::class.java)
        resultIntent.putExtra(IntentExtraCodes.EXTRA_FILE_NAME, noteName)
        resultLauncher.launch(resultIntent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleFabClick() // fake click to revert to normal
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = result.data?.extras
            val noteName: String = extras?.get(IntentExtraCodes.EXTRA_RETURN_FILE_NAME) as String
            val category: String = extras.get(IntentExtraCodes.EXTRA_RETURN_FILE_CATEGORY) as String
            val timeOfCreation: String = Date().toString()
            val content: String = extras.get(IntentExtraCodes.EXTRA_RETURN_FILE_CONTENT) as String
            val note = LocalNote(noteName,timeOfCreation,category,content)
            viewModel.localDAO.saveLastNote(this,note)
            adapter.addItem(note) // refresh RV adapter
//            Blurry.with(this).capture(binding.containerForBlur).into(binding.mainBlurFL)

        } else {
            viewModel.localDAO.deleteLastNote(this)
        }
    }

    private fun launchEditActivity(noteName: String, category: String, content: String){
        val editIntent = Intent(this, ResultActivity::class.java)
        editIntent.putExtra(IntentExtraCodes.EXTRA_FILE_NAME, noteName)
        editIntent.putExtra(IntentExtraCodes.EXTRA_FILE_CONTENT, content)
        editIntent.putExtra(IntentExtraCodes.EXTRA_FILE_CATEGORY, category)
        editIntent.putExtra(IntentExtraCodes.EXTRA_IS_FOR_EDIT, true)
        editLauncher.launch(editIntent)
    }

    private var editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = result.data?.extras
            val prevNoteName: String = extras?.get(IntentExtraCodes.EXTRA_PREV_FILE_NAME) as String
            val noteName: String = extras.get(IntentExtraCodes.EXTRA_RETURN_FILE_NAME) as String
            val category: String = extras.get(IntentExtraCodes.EXTRA_RETURN_FILE_CATEGORY) as String
            val content: String = extras.get(IntentExtraCodes.EXTRA_RETURN_FILE_CONTENT) as String
            //val note = LocalNote(noteName,timeOfCreation,category,content)
            viewModel.localDAO.overwriteNote(this, prevNoteName, noteName, category, content)
            adapter.updateItem(prevNoteName, noteName, category, content)// refresh RV adapter
//          Blurry.with(this).capture(binding.containerForBlur).into(binding.mainBlurFL)

        } else {
            viewModel.localDAO.deleteLastNote(this)
        }
    }

    private fun alertInputDialog(note: LocalNote){
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.view_note_alert_dialog,null)
        builder.setTitle(note.noteName)
        val image: ImageView = view.findViewById(R.id.noteImageAlertDialog)
        val content: TextView = view.findViewById(R.id.noteContentAlertDialog)
        val category: TextView = view.findViewById(R.id.noteCategoryAlertDialog)
        val date: TextView = view.findViewById(R.id.noteDateAlertDialog)
        val copyToClipButton: TextView = view.findViewById(R.id.cpClipButtonAlertDialog)
        copyToClipButton.setOnClickListener {
            Toast.makeText(this@HomepageActivity,"Copied text to clipboard", Toast.LENGTH_SHORT).show()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ScanR Note Content", note.content)
            clipboard.setPrimaryClip(clip)
        }

        val thumbnailUri = viewModel.localDAO.getImageFromNoteName(this, note.noteName)
        Picasso.get().load(thumbnailUri).placeholder(R.mipmap.ic_launcher).error(R.mipmap.ic_launcher).resize(200, 200).centerCrop().rotate(90F).into(image)
        content.text = note.content
        category.text = "Category: ${note.category}"
        content.movementMethod = ScrollingMovementMethod()
        date.text = "Time of creation: ${Date(note.timeOfCreation).toLocaleString()}"
        builder.setView(view)
        builder.setNeutralButton("Ok", null)
        alertDialog = builder.create()
        alertDialog?.show()
    }

    // chip filtering logic
    private var categoryChipListener =
        ChipGroup.OnCheckedStateChangeListener { group, _ ->
            val selectedChip : Chip = findViewById(group.checkedChipId);
            adapter.getCategoryFilter(searchView.query).filter(selectedChip.text.toString().lowercase())
        }

}



