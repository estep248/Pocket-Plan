package com.pocket_plan.j7_003.system_interaction.handler.share

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.pocket_plan.j7_003.MainActivity
import java.io.File

class ImportHandler {
    fun import() {
        val permission = ActivityCompat.checkSelfPermission(MainActivity.act,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            MainActivity.act.requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
        }

        val importFile = browse()
    }

    private fun browse(): File {
        val chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        chooseFileIntent.type = "application/zip"
        chooseFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        MainActivity.act.startActivityForResult(Intent.createChooser(chooseFileIntent, "Choose file"), 2000)
        return File("")
    }
}