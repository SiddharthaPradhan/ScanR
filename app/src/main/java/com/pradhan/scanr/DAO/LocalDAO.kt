package com.pradhan.scanr.DAO

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.pradhan.scanr.BuildConfig
import com.pradhan.scanr.model.LocalNote
import java.io.File
import java.util.Date
import kotlin.collections.ArrayList

/**
 * What is a Local Note
 * - Note Name
 * - Note time created
 * - Note category
 * - Note contents
 *
 */

class LocalDAO() {
    private val TEMP_THUMB_TAG: String = "TEMP_THUMBNAIL"
    private val TEMP_NOTE_TAG: String = "TEMP_NOTE"
    private var lastTimestamp: String? = null
    private val defaultCategories = arrayListOf("all", "personal", "work", "school")

    fun getLastNoteName(): String {
        return lastTimestamp.toString()
    }

    fun getThumbailDir(context:Context): File {
        // setup subdirectories
        val profileDirectory = File(context.applicationContext.filesDir,"scanr_thumbnails" )
        if (!profileDirectory.exists()) {
            profileDirectory.mkdir()
        }
        return profileDirectory
    }


    fun getNotesDir(context:Context): File {
        // setup subdirectories
        val profileDirectory = File(context.applicationContext.filesDir,"scanr_notes" )
        if (!profileDirectory.exists()) {
            profileDirectory.mkdir()
        }
        return profileDirectory
    }

    fun getCategoryDir(context:Context): File {
        // setup subdirectories
        val profileDirectory = File(context.applicationContext.filesDir,"scanr_categories" )
        if (!profileDirectory.exists()) {
            profileDirectory.mkdir()
            val categoryFileName = "categories.txt"
            val categoryFile = File(profileDirectory, categoryFileName)
            var categoryText = ""
            defaultCategories.forEach{ category ->
                categoryText += "$category\n"
            }
            categoryFile.writeText(categoryText)
        }
        return profileDirectory
    }

    fun getCategoryList(context:Context): ArrayList<String> {
        val categoryFileName = "categories.txt"
        val categoryFile = File(getCategoryDir(context), categoryFileName)
        val categories = ArrayList<String>()
            categoryFile.readLines().forEach {
                categoryString -> categories.add(categoryString)
        }
        return categories
    }

    fun addCategory(context:Context, newCategory: String){
        val categoryFileName = "categories.txt"
        val categoryFile = File(getCategoryDir(context), categoryFileName)
        categoryFile.appendText("$newCategory\n")
    }

    fun removeCategory(context:Context, categoryRemove: String){
        val categoryFileName = "categories.txt"
        val categoryFile = File(getCategoryDir(context), categoryFileName)
        val categories = categoryFile.readLines()
        categories.drop(categories.indexOf(categoryRemove))
        var categoryText = ""
        categories.forEach{ category ->
            categoryText += "$category\n"
        }
        categoryFile.writeText(categoryText)
    }

    // Creates empty note file and image file, returns an array of fileURI and IMAGEURI
    fun newLocalNote(context:Context): Uri {
        lastTimestamp = Date().toString()
        val noteName = TEMP_NOTE_TAG + "_${lastTimestamp}.txt"
        val noteThumbnail = TEMP_THUMB_TAG + "_${lastTimestamp}.jpg"
        var noteFile = File(getNotesDir(context), noteName)
        val thumbnailFile = File(getThumbailDir(context), noteThumbnail)
        noteFile.createNewFile()
        thumbnailFile.createNewFile()
        val uri: Uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", thumbnailFile)
        return uri
    }

    fun addImageToNote(context:Context, imgName: String, imageData: ByteArray){
        val noteThumbnail = TEMP_THUMB_TAG + "_${imgName}.jpg"
        val file = File(getThumbailDir(context),noteThumbnail)
        file.writeBytes(imageData)
    }

    fun getImageFromNoteName(context:Context, noteName: String): Uri {
        val noteThumbnail = "${noteName}.jpg"
        val thumbnailFile = File(getThumbailDir(context), noteThumbnail)
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            thumbnailFile
        )
    }


    fun getTempNoteImage(context:Context, noteName: String): Uri {
        val noteThumbnail = TEMP_THUMB_TAG + "_${noteName}.jpg"
        val thumbnailFile = File(getThumbailDir(context), noteThumbnail)
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            thumbnailFile
        )
    }






    // renames temp file
    fun renameLastNote(context:Context, noteName:String): Boolean{
        val noteFile = File(getNotesDir(context), TEMP_NOTE_TAG + "_${lastTimestamp}.txt")
        val thumbnailFile = File(getThumbailDir(context), TEMP_THUMB_TAG + "_${lastTimestamp}.jpg")
        val noteFileNew = File(getNotesDir(context), "$noteName.txt")
        val thumbnailFileNew = File(getThumbailDir(context), "$noteName.jpg")
        return (noteFile.renameTo(noteFileNew) && thumbnailFile.renameTo(thumbnailFileNew))
    }

    fun deleteLastNote(context:Context): Boolean{
        val noteName = TEMP_NOTE_TAG + "_${lastTimestamp}.txt"
        val noteThumbnail = TEMP_THUMB_TAG + "_${lastTimestamp}.jpg"
        val noteFile = File(getNotesDir(context), noteName)
        val thumbnailFile = File(getThumbailDir(context), noteThumbnail)
        return (noteFile.delete() && thumbnailFile.delete())
    }

    fun deleteNote(context:Context, note: LocalNote): Boolean{
        val noteName = "${note.noteName}.txt"
        val noteThumbnail = "${note.noteName}.jpg"
        var noteFile = File(getNotesDir(context), noteName)
        val thumbnailFile = File(getThumbailDir(context), noteThumbnail)
        return (noteFile.delete() && thumbnailFile.delete())
    }



    // Saving text into existing note
    fun overwriteNote(context:Context, previousNoteName: String, noteName: String, category: String, content: String){
        val noteFile = File(getNotesDir(context),"$previousNoteName.txt")
        val thumbnailFile = File(getThumbailDir(context), "$previousNoteName.jpg")
        val note = LocalNote.fromJsonStringToLocalNote(noteFile.readText(), noteName)
        note.category = category
        note.content = content
        noteFile.writeText(note.toJson().toString())
        val noteFileNew = File(getNotesDir(context), "$noteName.txt")
        val thumbnailFileNew = File(getThumbailDir(context), "$noteName.jpg")
        noteFile.renameTo(noteFileNew)
        thumbnailFile.renameTo(thumbnailFileNew)
    }

    fun getAllNotes(context:Context): ArrayList<LocalNote>{
        val localNotes: ArrayList<LocalNote> = ArrayList()
        val profileDirectory = File(context.applicationContext.filesDir,"scanr_notes" )
        profileDirectory.list()?.forEach { noteFileName ->
            val noteFile = File(getNotesDir(context), noteFileName)
            val rawNoteText = noteFile.readText()
            // dropLast to remove the ".txt" from noteFileName
            try {
                localNotes.add(LocalNote.fromJsonStringToLocalNote(rawNoteText,noteFileName.dropLast(4)))
            } catch (e: Exception){

            }
        }
        return localNotes
    }

    fun saveLastNote(context:Context, note: LocalNote){
        // find latest Tempt note
        val noteFile = File(getNotesDir(context), TEMP_NOTE_TAG + "_${lastTimestamp}.txt")
        if (noteFile.exists()){
            noteFile.writeText(note.toJson().toString())
            // update name
            renameLastNote(context, note.noteName)
        }
    }

    /**
     * Local Temp Note
     *  Set up has image and blank text
     * Then after image processing
     *   -> And if user clicks save
     *          -> Create new localNote object, pass to dao and overwrite existing temp file
     */
}