package com.neilb.mindwarchess.unit

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.neilb.mindwarchess.model.Piece
import com.neilb.mindwarchess.model.PieceInPosition

fun Context.showToast(msg: String?) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Boolean.toInt() = if (this) 1 else 0

fun Activity.tryCatchAndLog(function: () -> Unit) {
    try {
        function()
    } catch (e: java.lang.Exception) {
        //Log.e(this.localClassName, e.message.toString())
        println("-------------------------------------------")
        e.printStackTrace()
        println("-------------------------------------------")
    }
}

fun Int.toBoolean() = this % 2 != 0

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
