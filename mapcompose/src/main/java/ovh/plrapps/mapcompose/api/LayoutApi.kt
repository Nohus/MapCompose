@file:Suppress("unused")

package ovh.plrapps.mapcompose.api

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import ovh.plrapps.mapcompose.ui.layout.Fill
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.layout.MinimumScaleMode
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.AngleDegree

/**
 * The scale of the map. By convention, the scale at full dimension is 1f.
 */
var MapState.scale: Float
    get() = zoomPanRotateState.scale
    set(value) {
        zoomPanRotateState.setScale(value)
    }

/**
 * The [rotation] property is the angle (in decimal degrees) of rotation,
 * using the center of the view as the pivot point.
 */
var MapState.rotation: AngleDegree
    get() = zoomPanRotateState.rotation
    set(value) {
        zoomPanRotateState.setRotation(value)
    }

/**
 * The [scroll] property defines the position of the top-left corner of the visible viewport.
 * This is a low-level concept (the value is in scaled pixels). To scroll to a known position,
 * prefer the [scrollToAndCenter] API.
 */
var MapState.scroll: Offset
    get() = Offset(zoomPanRotateState.scrollX, zoomPanRotateState.scrollY)
    set(value) {
        zoomPanRotateState.setScroll(value.x, value.y)
    }

/**
 * Get notified whenever the state ([scale] and/or [scroll] and/or [rotation]) changes.
 *
 * @param cb An extension function with [MapState] as receiver type
 */
fun MapState.setStateChangeListener(cb: MapState.() -> Unit) {
    stateChangeListener = cb
}

/**
 * Removes the state change listener.
 */
fun MapState.removeStateChangeListener() {
    stateChangeListener = null
}

/**
 * On double-tap, and if the scale is already at its maximum value, circle-back to the minimum scale.
 */
var MapState.shouldLoopScale
    get() = zoomPanRotateState.shouldLoopScale
    set(value) {
        zoomPanRotateState.shouldLoopScale = value
    }

/**
 * Enable the rotation by user gestures.
 */
fun MapState.enableRotation() {
    zoomPanRotateState.isRotationEnabled = true
}

/**
 * Discard rotation gestures. The map can still be programmatically rotated using APIs such as
 * [rotateTo] or [rotation].
 */
fun MapState.disableRotation() {
    zoomPanRotateState.isRotationEnabled = false
}

/**
 * Set the minimum scale mode. See [MinimumScaleMode].
 * The minimum scale can be manually defined using [Forced], or can be inferred using [Fill], or
 * [Fit] (the default).
 * Note: When enabling map rotation, it's advised to use the [Fill] mode.
 */
var MapState.minimumScaleMode: MinimumScaleMode
    get() = zoomPanRotateState.minimumScaleMode
    set(value) {
        zoomPanRotateState.minimumScaleMode = value
    }

/**
 * The default maximum scale is 2f.
 * When changed, and if the current scale is greater than the new [maxScale], the current scale is
 * changed to be equal to [maxScale].
 */
var MapState.maxScale: Float
    get() = zoomPanRotateState.maxScale
    set(value) {
        zoomPanRotateState.maxScale = value
    }

/**
 * The scroll offset ratio allows to scroll past the default scroll limits. They are expressed in
 * percentage of the layout dimensions.
 * Values must be in [0f..1f] range, or an [IllegalArgumentException] is thrown.
 * Setting a scroll offset ratio is useful when rotation is enabled, so that edges of the map are
 * reachable.
 *
 * @param xRatio The horizontal scroll offset ratio. The scroll offset will be equal to this ratio
 * multiplied by the layout width.
 * @param yRatio The vertical scroll offset ratio. The scroll offset will be equal to this ratio
 * multiplied by the layout height.
 */
fun MapState.setScrollOffsetRatio(xRatio: Float, yRatio: Float) {
    zoomPanRotateState.scrollOffsetRatio = Offset(xRatio, yRatio)
}

/**
 * Rotates to the specified [angle] in decimal degrees, animating the rotation.
 */
suspend fun MapState.rotateTo(
    angle: AngleDegree,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    zoomPanRotateState.smoothRotateTo(angle, animationSpec)
}

/**
 * Scrolls and center on a position, animating the scroll position and the scale.
 *
 * @param x The normalized X position on the map, in range [0..1]
 * @param y The normalized Y position on the map, in range [0..1]
 * @param destScale The destination scale. The default value is the current scale.
 * @param animationSpec The [AnimationSpec]. Default is [SpringSpec] with low stiffness.
 */
suspend fun MapState.scrollToAndCenter(
    x: Double,
    y: Double,
    destScale: Float = scale,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    with(zoomPanRotateState) {
        awaitLayout()
        val destScrollX = (x * fullWidth * destScale - layoutSize.width / 2).toFloat()
        val destScrollY = (y * fullHeight * destScale - layoutSize.height / 2).toFloat()

        smoothScrollAndScale(
            destScrollX,
            destScrollY,
            destScale,
            animationSpec
        )
    }
}

/**
 * The [centroidX] is the x coordinate of the center of rotation transformation. It changes with the
 * scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a relative coordinate (in [0.0 .. 1.0] range).
 */
val MapState.centroidX: Double
    get() = zoomPanRotateState.centroidX

/**
 * The [centroidY] is the y coordinate of the center of rotation transformation. It changes with the
 * scroll and the scale.
 * This is a low-level concept, and is only useful when defining custom views.
 * The value is a relative coordinate (in [0.0 .. 1.0] range).
 */
val MapState.centroidY: Double
    get() = zoomPanRotateState.centroidY

/**
 * A convenience property. It corresponds to the size used when creating the [MapState].
 */
val MapState.fullSize: IntSize
    get() = IntSize(zoomPanRotateState.fullWidth, zoomPanRotateState.fullHeight)

/**
 * Registers a tap callback for tap gestures. The callback is invoked with the relative coordinates
 * of the tapped point on the map.
 * Note: the tap gesture is detected only after the [ViewConfiguration.doubleTapMinTimeMillis] has
 * passed, because the layout's gesture detector also detects double-tap gestures.
 */
fun MapState.onTap(tapCb: (x: Double, y: Double) -> Unit) {
    zoomPanRotateState.tapCb = tapCb
}

