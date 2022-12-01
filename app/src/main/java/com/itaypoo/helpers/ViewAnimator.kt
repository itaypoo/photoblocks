package com.itaypoo.helpers

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup

object ViewAnimator {

    /*
        VIEW ANIMATOR

        This is a utility class for animating views in runtime.

        Using premade animations in res/anim is also an option,
        so use this object only for when premade animations are not
        capable of getting the wanted result (for example - scaling a
        view to the screen size.)

     */

    fun fadeView(view: View, startAlpha: Float, endAlpha: Float, animDuration: Long, animInterpolator: TimeInterpolator){
        view.alpha = startAlpha
        ObjectAnimator.ofFloat(view, "alpha", endAlpha).apply {
            duration = animDuration
            interpolator = animInterpolator
            start()
        }
    }

    fun animateViewWidth(view: View, endWidth: Int, duration: Long, animInterpolator: TimeInterpolator){
        val anim = ValueAnimator.ofInt(view.measuredHeight, endWidth)
        anim.addUpdateListener { valueAnimator ->
            val width = valueAnimator.animatedValue as Int
            val newParams: ViewGroup.LayoutParams = view.layoutParams
            newParams.width = width
            view.layoutParams = newParams
        }
        anim.interpolator = animInterpolator
        anim.duration = duration
        anim.start()
    }

    fun animateViewHeight(view: View, endHeight: Int, duration: Long, animInterpolator: TimeInterpolator){
        val anim = ValueAnimator.ofInt(view.measuredHeight, endHeight)
        anim.addUpdateListener { valueAnimator ->
            val height = valueAnimator.animatedValue as Int
            val newParams: ViewGroup.LayoutParams = view.layoutParams
            newParams.height = height
            view.layoutParams = newParams
        }
        anim.interpolator = animInterpolator
        anim.duration = duration
        anim.start()
    }

}