package com.pradhan.scanr.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.pradhan.scanr.DAO.LocalDAO

class HomepageViewModel: ViewModel() {
    var isAllFabVisible: Boolean = false
    var latestNoteThumbnailImage: Uri? = null
    val localDAO: LocalDAO = LocalDAO()

}