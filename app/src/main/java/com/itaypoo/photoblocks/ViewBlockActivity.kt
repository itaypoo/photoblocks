package com.itaypoo.photoblocks

import android.animation.ObjectAnimator
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
        topBarAnimator.openTopBar(binding)
        topBarAnimator.mainInterpolator = DecelerateInterpolator()

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.closeButton.setOnClickListener {
            topBarAnimator.closeTopBar(binding)
        }

        binding.openButton.setOnClickListener {
            topBarAnimator.openTopBar(binding)
        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private fun initTopBarUi() {
        val bgColor = currentBlock.secondaryColor.toInt()
        val bgInvertedColor = AppUtils.invertColor(bgColor, 255)

        binding.titleTextSmall.text = currentBlock.title
        binding.titleTextBig.text = currentBlock.title

        window.statusBarColor = bgColor
        binding.topBarCardView.setBackgroundColor( bgColor )
        binding.gradientImage.imageTintList = ColorStateList.valueOf( bgColor )

        binding.backButton.imageTintList = ColorStateList.valueOf( bgInvertedColor )
        binding.moreButton.imageTintList = ColorStateList.valueOf( bgInvertedColor )
        binding.titleTextBig.setTextColor( bgInvertedColor )
        binding.titleTextSmall.setTextColor( bgInvertedColor )

        Glide.with(this).load(currentBlock.coverImageUrl).into(binding.blockImagePreview)
    }

    // Top bar animator object
    //////////////////////////////////////////////
    internal object topBarAnimator{
        var mainInterpolator: Interpolator? = null
        private var animDuration: Long = 300

        fun fadeView(view: View, startAlpha: Float, endAlpha: Float, animDuration: Long){
            view.alpha = startAlpha
            ObjectAnimator.ofFloat(view, "alpha", endAlpha).apply {
                duration = animDuration
                interpolator = mainInterpolator
                start()
            }
        }

        fun animateViewHeight(view: View, endHeight: Int, duration: Long){
            val anim = ValueAnimator.ofInt(view.measuredHeight, endHeight)
            anim.addUpdateListener { valueAnimator ->
                val height = valueAnimator.animatedValue as Int
                val newParams: ViewGroup.LayoutParams = view.layoutParams
                newParams.height = height
                view.layoutParams = newParams
            }
            anim.interpolator = mainInterpolator
            anim.duration = duration
            anim.start()
        }

        fun openTopBar(binding: ActivityViewBlockBinding){
            // Scale up card
            animateViewHeight(binding.topBarCardView, 500, animDuration)
            // Fade in big title, preview image
            fadeView(binding.titleTextBig, 0.0f, 1.0f, animDuration)
            fadeView(binding.blockImagePreview, 0.0f, 1.0f, animDuration)
            // Fade out small title
            fadeView(binding.titleTextSmall, 1.0f, 0.0f, animDuration)
        }

        fun closeTopBar(binding: ActivityViewBlockBinding){
            // Scale down card
            animateViewHeight(binding.topBarCardView, 180, animDuration)
            // Fade out big title, preview image
            fadeView(binding.titleTextBig, 1.0f, 0.0f, animDuration)
            fadeView(binding.blockImagePreview, 1.0f, 0.0f, animDuration)
            // Fade in small title
            fadeView(binding.titleTextSmall, 0.0f, 1.0f, animDuration)
        }
    }
    /////////////////////////////////////////////
}