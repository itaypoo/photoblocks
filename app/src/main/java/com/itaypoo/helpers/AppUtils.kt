package com.itaypoo.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.toColor
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itaypoo.photoblockslib.Block
import com.itaypoo.photoblockslib.User
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.*

object AppUtils {
    var storageRef: StorageReference
    var currentUser: User? = null

    // These variables are used to pass data between activities, that we cannot pass with intet.putExtra(). (Like objects)
    var passedBlock: Block? = null

    init {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        storageRef = FirebaseStorage.getInstance().reference
    }

    fun saveBitmapToPrivateInternal(bitmap: Bitmap, fileName: String, context: Context): String? {
        var fileName: String? = fileName // No .png or .jpg needed
        try {
            // Compress bitmap into a ByteArray
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            // Save the ByteArray to private storage with a FileOutputStream
            val outputStream: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            outputStream.write(bytes.toByteArray())
            // Remember close file output
            outputStream.close()
        } catch (exep: Exception) {
            // Catch exception, return null
            exep.printStackTrace()
            fileName = null
        }
        return fileName
    }

    fun getBitmapFromPrivateInternal(fileName: String, context: Context): Bitmap{
        // Get bitmap that was saved in internal storage
        val inputStream = context.openFileInput(fileName)
        val res = BitmapFactory.decodeStream(inputStream)
        // Remember to close input stream
        inputStream.close()

        return res
    }

    fun getGlobalTime(): List<Int>{
        // Get current time in GMT
        // Returns a list of (year, day, minute)
        val dateFormat: DateFormat = DateFormat.getTimeInstance()
        dateFormat.setTimeZone(TimeZone.getTimeZone("gmt"))
        val cal: Calendar = dateFormat.getCalendar()

        val year: Int = cal.get(Calendar.YEAR)
        val day: Int = cal.get(Calendar.DAY_OF_YEAR)
        val minute: Int = cal.get(Calendar.MINUTE)

        return listOf<Int>(year, day, minute)
    }

    fun invertColor(color: Int, alpha: Int): Int{
        val red: Int = ( 255 - color.toColor().red() ).toInt()
        val green: Int = ( 255 - color.toColor().green() ).toInt()
        val blue: Int = ( 255 - color.toColor().blue() ).toInt()

        return Color.argb(alpha, red, green, blue)
    }

}