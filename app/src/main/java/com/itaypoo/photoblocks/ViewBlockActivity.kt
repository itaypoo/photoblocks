package com.itaypoo.photoblocks

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.itaypoo.helpers.AppUtils
import com.itaypoo.helpers.ViewAnimator
import com.itaypoo.photoblocks.databinding.ActivityViewBlockBinding
import com.itaypoo.photoblockslib.Block
import java.time.Duration


class ViewBlockActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewBlockBinding

    private lateinit var currentBlock: Block

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get passed block
        if(AppUtils.passedBlock == null){
            Toast.makeText(this, "Error getting block data.", Toast.LENGTH_SHORT).show()
            finish()
        }
        else{
            // From now on we can safely use AppUtils.passedBlock!! ( or currentBlock )
            currentBlock = AppUtils.passedBlock!!
        }

        // Init block view
        initTopBarUi()
        topBarAnimator.openTopBar(binding, true)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.closeButton.setOnClickListener {
            topBarAnimator.closeTopBar(binding)
        }

        binding.openButton.setOnClickListener {
            topBarAnimator.openTopBar(binding, false)
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun initTopBarUi() {
        val bgColor = currentBlock.secondaryColor.toInt()
        val bgInvertedColor = AppUtils.invertColor(bgColor, 255)

        Glide.with(this).load(currentBlock.coverImageUrl).into(binding.blockImagePreview)

        binding.titleTextSmall.text = currentBlock.title
        binding.titleTextBig.text = currentBlock.title

        window.statusBarColor = bgColor
        binding.topBarCardView.setBackgroundColor( bgColor )
        binding.gradientImage.imageTintList = ColorStateList.valueOf( bgColor )

        binding.backButton.imageTintList = ColorStateList.valueOf( bgInvertedColor )
        binding.moreButton.imageTintList = ColorStateList.valueOf( bgInvertedColor )
        binding.titleTextBig.setTextColor( bgInvertedColor )
        binding.titleTextSmall.setTextColor( bgInvertedColor )



    }

    // Top bar animator object
    //////////////////////////////////////////////
    internal object topBarAnimator{
        var mainInterpolator: TimeInterpolator = DecelerateInterpolator()
        private var animDuration: Long = 300

        fun openTopBar(binding: ActivityViewBlockBinding, isInstant: Boolean){
            if(isInstant) animDuration = 1

            // Scale up card
            ViewAnimator.animateViewHeight(binding.topBarCardView, 500, animDuration, mainInterpolator)
            // Fade in big title, preview image
            ViewAnimator.fadeView(binding.titleTextBig, 0.0f, 1.0f, animDuration, mainInterpolator)
            ViewAnimator.fadeView(binding.blockImagePreview, 0.0f, 1.0f, animDuration, mainInterpolator)
            // Fade out small title
            ViewAnimator.fadeView(binding.titleTextSmall, 1.0f, 0.0f, animDuration, mainInterpolator)

            if(isInstant) animDuration = 300
        }

        fun closeTopBar(binding: ActivityViewBlockBinding){
            // Scale down card
            ViewAnimator.animateViewHeight(binding.topBarCardView, 180, animDuration, mainInterpolator)
            // Fade out big title, preview image
            ViewAnimator.fadeView(binding.titleTextBig, 1.0f, 0.0f, animDuration, mainInterpolator)
            ViewAnimator.fadeView(binding.blockImagePreview, 1.0f, 0.0f, animDuration, mainInterpolator)
            // Fade in small title
            ViewAnimator.fadeView(binding.titleTextSmall, 0.0f, 1.0f, animDuration, mainInterpolator)
        }
    }
    /////////////////////////////////////////////
}