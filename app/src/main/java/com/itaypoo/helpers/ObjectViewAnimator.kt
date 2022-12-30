package com.itaypoo.helpers

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup

object ObjectViewAnimator {
    public var timerDelay = 0f

    /*
        VIEW ANIMATOR

        This is a utility class for animating views in runtime.

        Using premade animations in res/anim is also an option,
        so use this object only for when premade animations are not
        capable of getting the wanted result (for example - scaling a
        view to the screen size.)

     */

    fun startTimer(animDuration: Long, delay: Long, onComplete: (() -> Unit)){
        var f = 0f
        ObjectAnimator.ofFloat(this, "timerDelay", 1f).apply {
            duration = animDuration
            startDelay = delay
            start()
        }.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
            override fun onAnimationEnd(animator: Animator) {
                // Animation end
                onComplete.invoke()
            }
        })
    }

    fun fadeView(view: View, startAlpha: Float, endAlpha: Float, animDuration: Long, animInterpolator: TimeInterpolator, delay: Long = 0L){
        view.alpha = startAlpha
        ObjectAnimator.ofFloat(view, "alpha", endAlpha).apply {
            duration = animDuration
            interpolator = animInterpolator
            setStartDelay(delay)
            start()
        }
    }

    fun animateViewWidth(view: View, endWidth: Int, duration: Long, animInterpolator: TimeInterpolator, delay: Long = 0L){
        val anim = ValueAnimator.ofInt(view.measuredHeight, endWidth)
        anim.addUpdateListener { valueAnimator ->
            val width = valueAnimator.animatedValue as Int
            val newParams: ViewGroup.LayoutParams = view.layoutParams
            newParams.width = width
            view.layoutParams = newParams
        }
        anim.startDelay = delay
        anim.interpolator = animInterpolator
        anim.duration = duration
        anim.start()
    }

    fun animateViewHeight(view: View, endHeight: Int, duration: Long, animInterpolator: TimeInterpolator, delay: Long = 0L){
        val anim = ValueAnimator.ofInt(view.measuredHeight, endHeight)
        anim.addUpdateListener { valueAnimator ->
            val height = valueAnimator.animatedValue as Int
            val newParams: ViewGroup.LayoutParams = view.layoutParams
            newParams.height = height
            view.layoutParams = newParams
        }
        anim.startDelay = delay
        anim.interpolator = animInterpolator
        anim.duration = duration
        anim.start()
    }

}