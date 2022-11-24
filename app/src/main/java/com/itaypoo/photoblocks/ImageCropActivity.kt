package com.itaypoo.photoblocks

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.Consts
import com.itaypoo.photoblocks.databinding.ActivityImageCropBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ImageCropActivity : AppCompatActivity() {
    /*

     --- Activity for selecting an image from gallery and cropping it ---

     INPUT EXTRAS: Crop ratio type (Consts.Extras.RATIO_XXX)

     RATIO_NORMAL = usees the aspect ratio of the original image
     RATIO_ONE_TO_ONE = crops at a 1:1 ratio - Square crop
     RATIO_BLOCK_COVER = crops at the ratio of a Block cover image

     OUTPUT EXTRAS: Cropped file name (String)

     Please use AppUtills.getBitmapFromPrivateInternal(croppedFileName) to retrieve cropped image

    */

    private lateinit var binding: ActivityImageCropBinding

    private var ratioType: Int = Consts.Extras.RATIO_NORMAL

    private lateinit var originalBitmap: Bitmap
    private lateinit var resultBitmap: Bitmap
    private var zoomPercent = 0.0

    private var xScrollPercent = 0.0
    private var yScrollPercent = 0.0

    private var resIntent = Intent()

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set device background colors to fit the UI
        window.statusBarColor = getColor(android.R.color.black)
        window.navigationBarColor = getColor(android.R.color.black)

        // Retrieve if one to one ratio is enabled
        ratioType = intent.getIntExtra(Consts.Extras.CROP_INPUT_RATIO, Consts.Extras.RATIO_NORMAL)

        // Pick image from gallery to crop
        var galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, Consts.RequestCode.GALLERY_PICKER_SINGLE)

        // Set Seekbar change listener
        binding.zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean)
            {
                // Update image on zoom in/out
                updateImage()
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun updateImage() {
        // Calculate zoom percentage
        zoomPercent = ((binding.zoomSeekBar.progress).toDouble() / 100)
        zoomPercent = abs(zoomPercent - 1.0)
        // Crop image to zoom percentage
        var imageBitmap = cropBitmap(originalBitmap, zoomPercent, xScrollPercent, yScrollPercent, zoomPercent)
        // Update image preview
        resultBitmap = imageBitmap
        binding.imagePreview.setImageBitmap(imageBitmap)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Get selected image from gallery
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null && requestCode == Consts.RequestCode.GALLERY_PICKER_SINGLE) {
            // Get and store the image Bitmap
            val imageUri = data.data
            var imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            originalBitmap = imageBitmap
            // Set image preview
            updateImage()
        }
        else{
            // Error loading image from gallery, finish crop activity
            setResult(RESULT_CANCELED, resIntent)
            finish()
        }
    }


    fun cropBitmap(bitmap: Bitmap, percent: Double, xScroll: Double, yScroll: Double, zoomPercent: Double): Bitmap {
        var x = bitmap.width; var y = bitmap.height
        var u: Int; var v: Int
        var uP: Int; var vP: Int

        // Calculate result height and width based on the zoom percentage
        u = (x * zoomPercent).toInt()
        v = (y * zoomPercent).toInt()

        if(ratioType == Consts.Extras.RATIO_ONE_TO_ONE){
            u = min(u,v); v= u
        }
        else if(ratioType == Consts.Extras.RATIO_BLOCK_COVER){
            v = (0.45 * u).toInt()
        }

        // Calculate result base point based on scroll percentage
        uP = ( (x - u) * xScroll ).toInt()
        vP = ( (y - v) * yScroll ).toInt()

        return Bitmap.createBitmap(bitmap, uP, vP, u, v)
    }


    fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        // Rotate a bitmap with a Matrix
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    fun rotateButton_click(view: View) {
        originalBitmap = rotateBitmap(originalBitmap, (90).toFloat())
        updateImage()
    }


    fun cancelButton_click(view: View) {
        // Canceled
        setResult(RESULT_CANCELED, resIntent)
        finish()
    }


    fun zoomOutButton_click(view: View) {
        if(binding.zoomSeekBar.progress > 10){
            binding.zoomSeekBar.progress -= 10
            updateImage()
        }
        else{
            binding.zoomSeekBar.progress = 0
            updateImage()
        }
    }

    fun zoomInButton_click(view: View) {
        if(binding.zoomSeekBar.progress < 90){
            binding.zoomSeekBar.progress += 10
            updateImage()
        }
        else{
            binding.zoomSeekBar.progress = 100
            updateImage()
        }
    }

    // Movement controls
    fun moveUp_click(view: View) { yScrollPercent = max(0.0, yScrollPercent - 0.1); updateImage() }
    fun moveLeft_click(view: View) { xScrollPercent = max(0.0, xScrollPercent - 0.1); updateImage() }
    fun moveRight_click(view: View) { xScrollPercent = min(1.0, xScrollPercent + 0.1); updateImage() }
    fun moveDown_click(view: View) { yScrollPercent = min(1.0, yScrollPercent + 0.1); updateImage() }


    fun finishButton_click(view: View) {
        // Save result into internal storage, return file name
        var fileName: String? = AppUtils.saveBitmapToPrivateInternal(resultBitmap, "tempCroppedImage", this)

        if(fileName == null){
            Toast.makeText(this, "Error saving image. Please try again", Toast.LENGTH_SHORT).show()
        }
        else{
            resIntent.putExtra(Consts.Extras.CROP_OUTPUT_CROPPEDFILENAME, fileName)
            setResult(RESULT_OK, resIntent)
            finish()
        }
    }
}