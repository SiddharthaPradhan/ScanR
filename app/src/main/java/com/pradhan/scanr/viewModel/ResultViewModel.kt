package com.pradhan.scanr.viewModel

import androidx.lifecycle.ViewModel
import com.pradhan.scanr.DAO.LocalDAO

class ResultViewModel: ViewModel() {
    var localDAO: LocalDAO = LocalDAO()
    var categoryList: ArrayList<String> = ArrayList()
}