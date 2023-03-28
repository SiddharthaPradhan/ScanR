package com.pradhan.scanr.ui.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.api.client.extensions.android.json.AndroidJsonFactory
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.AnnotateImageRequest
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest
import com.google.api.services.vision.v1.model.Feature
import com.google.api.services.vision.v1.model.Image
import com.pradhan.scanr.R
import com.pradhan.scanr.databinding.ActivityResultBinding
import com.pradhan.scanr.utility.IntentExtraCodes
import com.pradhan.scanr.viewModel.ResultViewModel
import java.io.IOException
import java.util.*

const val KEY_FOR_MESSAGE: String = "KEY_FOR_MESSAGE"
class ResultActivity: AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var result_edittext: EditText
    private lateinit var name_edittext: EditText
    private lateinit var enter_title_textview: TextView
    private lateinit var saveButton: Button
    private lateinit var copyToClipButton: Button
    private lateinit var textField: TextInputLayout
    private lateinit var viewModel: ResultViewModel
    private lateinit var progressOverlay: FrameLayout
    lateinit var handler: Handler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        topAppBar = binding.topAppBar
        enter_title_textview = binding.addTitleTextview
        topAppBar.setNavigationOnClickListener {
            // close activity (go back)
            finish()
        }
        setContentView(binding.root)


        val bundleExtra: Bundle? = intent.extras
        val noteName = bundleExtra?.get(IntentExtraCodes.EXTRA_FILE_NAME) as String
        viewModel = ViewModelProvider(this)[ResultViewModel::class.java]
        progressOverlay = findViewById(R.id.progress_overlay)

        result_edittext = binding.resultEdittext
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val convertedText: String = msg.data.getString(KEY_FOR_MESSAGE, "Server Error Occurred")
                result_edittext.setText(convertedText)
                progressOverlay.visibility = View.INVISIBLE
            }
        }



        if (viewModel.categoryList.size == 0) {
            viewModel.categoryList = viewModel.localDAO.getCategoryList(this)
        }


        val adapter =
            ArrayAdapter(this@ResultActivity, R.layout.category_list_item, viewModel.categoryList)
        textField = binding.categorySelect
        (textField.editText as? AutoCompleteTextView)?.setAdapter(adapter)
        (textField.editText as? AutoCompleteTextView)?.setText("all", false)

        name_edittext = binding.nameEdittext
        saveButton = binding.saveTextButton


        saveButton.setOnClickListener {
            val newName = name_edittext.text.toString().ifEmpty { "Untitled_note_${Date()}" }
            val returnIntent = Intent().apply {
                putExtra(
                    IntentExtraCodes.EXTRA_RETURN_FILE_CONTENT,
                    result_edittext.text.toString()
                )
                putExtra(IntentExtraCodes.EXTRA_RETURN_FILE_NAME, newName)
                putExtra(
                    IntentExtraCodes.EXTRA_RETURN_FILE_CATEGORY,
                    textField.editText?.text.toString()
                )
            }
            setResult(RESULT_OK, returnIntent)
            finish()
        }

        copyToClipButton = binding.copyClipboardButton
        // copy to sys clipboard
        copyToClipButton.setOnClickListener {
            Toast.makeText(
                this@ResultActivity,
                "Copied converted text to system clipboard",
                Toast.LENGTH_LONG
            ).show()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ScanR Note Content", result_edittext.text)
            clipboard.setPrimaryClip(clip)
        }

        if (bundleExtra.containsKey(IntentExtraCodes.EXTRA_IS_FOR_EDIT)) {
            prepareActivityForEdit(bundleExtra)
        } else {
            progressOverlay.visibility = View.VISIBLE
            val t = Thread(MyRunnable(noteName, this, viewModel,handler))
            t.start()
        }

    }


    private fun prepareActivityForEdit(bundleExtra: Bundle?) {
        topAppBar.title = "Edit Note"
        enter_title_textview.text = "Edit title"
        name_edittext.setText(bundleExtra?.get(IntentExtraCodes.EXTRA_FILE_NAME) as String)
        result_edittext.setText(bundleExtra.get(IntentExtraCodes.EXTRA_FILE_CONTENT) as String)
        (textField.editText as? AutoCompleteTextView)?.setText(
            bundleExtra.get(IntentExtraCodes.EXTRA_FILE_CATEGORY) as String,
            false
        )
        saveButton.text = "Confirm"
        saveButton.setOnClickListener {
            val newName = name_edittext.text.toString().ifEmpty { "Untitled_note_${Date()}" }
            val returnIntent = Intent().apply {
                putExtra(
                    IntentExtraCodes.EXTRA_RETURN_FILE_CONTENT,
                    result_edittext.text.toString()
                )
                putExtra(IntentExtraCodes.EXTRA_RETURN_FILE_NAME, newName)
                putExtra(
                    IntentExtraCodes.EXTRA_RETURN_FILE_CATEGORY,
                    textField.editText?.text.toString()
                )
                putExtra(
                    IntentExtraCodes.EXTRA_PREV_FILE_NAME,
                    bundleExtra.get(IntentExtraCodes.EXTRA_FILE_NAME) as String
                )


                //putExtra(IntentExtraCodes.EXTRA_RETURN_IS_FOR_EDIT, true)
            }
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    //
//    private fun scanImage(noteName: String): String{
//        // AIzaSyDJOweDhgYbC-9JNXItK6gXQVfVurSAUUc TODO key lmao
//        val visionBuilder = Vision.Builder(
//            NetHttpTransport(),
//            AndroidJsonFactory(),
//            null
//        )
//
//        visionBuilder.setVisionRequestInitializer(
//            VisionRequestInitializer("AIzaSyDJOweDhgYbC-9JNXItK6gXQVfVurSAUUc")
//        )
//        val vision = visionBuilder.build()
//        val uri = viewModel.localDAO.getTempNoteImage(this, noteName)
//        println(uri)
//        val inputData: ByteArray? = readBytes(this, uri)
//        val inputImage = Image()
//        inputImage.encodeContent(inputData)
//        val desiredFeature = Feature()
//        desiredFeature.type = "TEXT_DETECTION"
//        val request = AnnotateImageRequest()
//        request.image = inputImage
//        request.features = listOf(desiredFeature)
//        val batchRequest = BatchAnnotateImagesRequest()
//        batchRequest.requests = listOf(request)
//        val batchResponse = vision.images().annotate(batchRequest).execute()
//        val text = batchResponse.responses[0].fullTextAnnotation
//        return text.text
//    }
    class MyRunnable(
        val noteName: String,
        val context: Context,
        val viewModel: ResultViewModel,
        val handler: Handler
    ) : Runnable {
        override fun run() {
            val visionBuilder = Vision.Builder(
                NetHttpTransport(),
                AndroidJsonFactory(),
                null
            )

            visionBuilder.setVisionRequestInitializer(
                VisionRequestInitializer("AIzaSyDJOweDhgYbC-9JNXItK6gXQVfVurSAUUc")
            )
            val vision = visionBuilder.build()
            val uri = viewModel.localDAO.getTempNoteImage(context, noteName)
            println(uri)
            val inputData: ByteArray? = readBytes(context, uri)
            val inputImage = Image()
            inputImage.encodeContent(inputData)
            val desiredFeature = Feature()
            desiredFeature.type = "TEXT_DETECTION"
            val request = AnnotateImageRequest()
            request.image = inputImage
            request.features = listOf(desiredFeature)
            val batchRequest = BatchAnnotateImagesRequest()
            batchRequest.requests = listOf(request)
            val batchResponse = vision.images().annotate(batchRequest).execute()
            val text = batchResponse.responses[0].fullTextAnnotation
            val bundle = Bundle()
            if (text == null){
                bundle.putString(KEY_FOR_MESSAGE, "No text detected")
            } else{
                bundle.putString(KEY_FOR_MESSAGE, text.text)
            }
            val message: Message = handler.obtainMessage()
            message.data = bundle
            handler.sendMessage(message)
        }

            @Throws(IOException::class)
            fun readBytes(context: Context, uri: Uri): ByteArray? =
                context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }


    }

}




