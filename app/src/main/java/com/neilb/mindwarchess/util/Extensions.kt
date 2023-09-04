package com.neilb.mindwarchess.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import com.neilb.mindwarchess.model.PieceInPosition

fun Boolean.toInt() = if (this) 1 else 0

fun tryCatchAndLog(function: () -> Unit) {
    try {
        function()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

fun Context.showAlertDialog(
    title: String,
    body: String,
    positiveButtonText: String = "Yes",
    negativeButtonText: String = "No",
    positiveCallback: () -> Unit,
) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setMessage(body)

    builder.setPositiveButton(positiveButtonText) { _, _ ->
        positiveCallback()
    }

    builder.setNegativeButton(negativeButtonText) { dialog, _ ->
        dialog.dismiss()
    }

    builder.show()
}

fun compareArrayLists(arrayList1: ArrayList<PieceInPosition>, arrayList2: ArrayList<PieceInPosition>): Boolean {
    if (arrayList1.size != arrayList2.size) {
        return false
    }

    for (i in 0 until arrayList1.size) {
        val obj1 = arrayList1[i]
        val obj2 = arrayList2[i]
        if (obj1 != obj2) {
            return false
        }
    }

    return true
}

fun LiveData<Boolean>.asBoolean(): Boolean = this.value == true