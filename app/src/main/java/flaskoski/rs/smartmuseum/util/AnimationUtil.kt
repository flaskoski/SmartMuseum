
/*
   This is a modified version of the following code:
   Copyright 2013 Google Inc.
   Licensed under Apache 2.0: http://www.apache.org/licenses/LICENSE-2.0.html
   Source - https://gist.github.com/broady/6314689
   Video - https://www.youtube.com/watch?v=WKfZsCKSXVQ&feature=youtu.be
   //----------------------------------------

   Converted to Kotlin by Felipe F. Laskoski
   */
package flaskoski.rs.smartmuseum.util

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import kotlin.math.abs
import kotlin.math.sign

/**
 * Animation utilities for moving markers from one location to another with Maps API.
 *
 */
class AnimationUtil {

    /**
     * Animates a marker from it's current position to the provided finalPosition
     *
     * @param marker        marker to animate
     * @param finalPosition the final position of the marker after the animation
     */
    fun animateMarkerTo(marker : Marker, finalPosition: LatLng) {
        // Use the appropriate implementation per API Level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            animateMarkerToICS(marker, finalPosition)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            animateMarkerToHC(marker, finalPosition)
        } else {
            animateMarkerToGB(marker, finalPosition)
        }
    }

    private fun animateMarkerToGB(marker: Marker, finalPosition: LatLng) {
        val latLngInterpolator = LatLngInterpolator.Linear()
        val startPosition = marker.position
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val interpolator = AccelerateDecelerateInterpolator()
        val durationInMs = 3000f

        handler.post(object : Runnable {
            internal var elapsed: Long = 0

            internal var t: Float = 0.toFloat()

            internal var v: Float = 0.toFloat()

            override fun run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start
                t = elapsed / durationInMs
                v = interpolator.getInterpolation(t)

                marker.position = latLngInterpolator.interpolate(v, startPosition, finalPosition)

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private fun animateMarkerToHC(marker: Marker, finalPosition: LatLng) {
        val latLngInterpolator = LatLngInterpolator.Linear()
        val startPosition = marker.position

        val valueAnimator = ValueAnimator()
        valueAnimator.addUpdateListener { animation ->
            val v = animation.animatedFraction
            val newPosition = latLngInterpolator
                    .interpolate(v, startPosition, finalPosition)
            marker.position = newPosition
        }
        valueAnimator.setFloatValues(0f, 1f) // Ignored.
        valueAnimator.duration = 3000
        valueAnimator.start()
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private fun animateMarkerToICS(marker: Marker, finalPosition: LatLng) {
        val latLngInterpolator = LatLngInterpolator.Linear()
        val typeEvaluator = TypeEvaluator<LatLng> { fraction, startValue, endValue -> latLngInterpolator.interpolate(fraction, startValue, endValue) }
        val property = Property.of(Marker::class.java, LatLng::class.java, "position")
        val animator = ObjectAnimator
                .ofObject(marker, property, typeEvaluator, finalPosition)
        animator.duration = 3000
        animator.start()
    }


    /**
     * For other LatLngInterpolator interpolators, see https://gist.github.com/broady/6314689
     */
    internal interface LatLngInterpolator {

        fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng

        class Linear : LatLngInterpolator {

            override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
                val lat = (b.latitude - a.latitude) * fraction + a.latitude
                var lngDelta = b.longitude - a.longitude

                // Take the shortest path across the 180th meridian.
                if (abs(lngDelta) > 180) {
                    lngDelta -= sign(lngDelta) * 360
                }
                val lng = lngDelta * fraction + a.longitude
                return LatLng(lat, lng)
            }
        }
    }
}