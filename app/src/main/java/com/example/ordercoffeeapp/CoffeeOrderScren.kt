package com.example.ordercoffeeapp

/*
 * Copyright 2026 Georgiopoulos Kyriakos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


// ==========================================
// SECTION 1: CORE DOMAIN, STATE & MOCK DATA
// ==========================================

/** Represents the physical size of the coffee cup and its base impact on price. */
enum class CoffeeSize(val basePrice: Double) {
    SMALL(3.50), MEDIUM(4.50), LARGE(5.50)
}

/** Classifies the type of ingredient for routing to the correct dock and popover logic. */
enum class IngredientCategory { SUGAR, MILK, DRINK }

/** The three foundational states of the application's transaction engine. */
enum class AppState { BREWING, PROCESSING, RECEIPT }

/** Core domain model representing an add-on item in the coffee application. */
data class Ingredient(
    val category: IngredientCategory,
    val displayName: String,
    val price: Double,
    val icon: String,
    val bgColor: Color,
    val contentColor: Color = Color(0xFF333333)
)

/** Represents the user's active configuration of their drink. */
data class OrderState(
    val size: CoffeeSize = CoffeeSize.MEDIUM,
    val ingredients: List<Ingredient> = emptyList()
)

/**
 * Defines the physics-based drag states for ingredient interaction.
 * Provides granular tracking of an ingredient's origin and current floating point.
 */
private sealed class DragState {
    data object None : DragState()
    data class Dragging(
        val ingredient: Ingredient,
        val initialPosition: Offset,
        val currentPosition: Offset
    ) : DragState()

    data class Returning(
        val ingredient: Ingredient,
        val initialPosition: Offset
    ) : DragState()
}

// Data Providers (Mock Database)
val SugarOptions = listOf(
    Ingredient(IngredientCategory.SUGAR, "Plain", 0.0, "🥄", Color(0xFFFFFFFF)),
    Ingredient(IngredientCategory.SUGAR, "Brown", 0.0, "🤎", Color(0xFFD7CCC8)),
    Ingredient(IngredientCategory.SUGAR, "Stevia", 0.0, "🌿", Color(0xFFE8F5E9), Color(0xFF2E7D32))
)

val MilkOptions = listOf(
    Ingredient(IngredientCategory.MILK, "Whole", 0.0, "🥛", Color(0xFFF5F5F7), Color(0xFF333333)),
    Ingredient(IngredientCategory.MILK, "Oat", 0.50, "🌾", Color(0xFFFFF8E1), Color(0xFFF57F17)),
    Ingredient(IngredientCategory.MILK, "Almond", 0.50, "🥜", Color(0xFFEFEBE9), Color(0xFF5D4037)),
    Ingredient(IngredientCategory.MILK, "Coconut", 0.50, "🥥", Color(0xFFFAFAFA), Color(0xFF4E342E))
)

val DrinkOptions = listOf(
    Ingredient(IngredientCategory.DRINK, "Latte", 0.0, "☕", Color(0xFFF5F5F7), Color(0xFF333333)),
    Ingredient(IngredientCategory.DRINK, "Cappuccino", 0.50, "☕", Color(0xFFFFF8E1), Color(0xFFF57F17)),
    Ingredient(IngredientCategory.DRINK, "Espresso", 0.50, "☕", Color(0xFFEFEBE9), Color(0xFF5D4037)),
    Ingredient(IngredientCategory.DRINK, "Americano", 0.50, "☕", Color(0xFFFAFAFA), Color(0xFF4E342E)),
    Ingredient(IngredientCategory.DRINK, "Mocha", 0.50, "☕", Color(0xFFFAFAFA), Color(0xFF4E342E)),
    Ingredient(IngredientCategory.DRINK, "Matcha", 0.50, "🍵", Color(0xFFFAFAFA), Color(0xFF4E342E))
)

val EspressoOptions = listOf(
    Ingredient(
        IngredientCategory.DRINK,
        "Espresso",
        1.20,
        "☕",
        Color(0xFF3E2723),
        Color(0xFFD7CCC8)
    )
)

val BaseIngredients = listOf(SugarOptions.first(), MilkOptions.first(), DrinkOptions.first())


// ==========================================
// SECTION 2: REUSABLE UI COMPONENTS
// ==========================================

/**
 * An interactive, draggable ingredient element.
 * Computes its true spatial center for pixel-perfect physics snap-backs.
 */
@Composable
fun DraggableIngredient(
    ingredient: Ingredient,
    customLabel: String? = null,
    sizeDp: Dp = 72.dp,
    isLightText: Boolean = false,
    onTap: () -> Unit = {},
    onDragStart: (Ingredient, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    isEnabled: Boolean
) {
    var boxCenter by remember { mutableStateOf(Offset.Zero) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { _ ->
                        onDragStart(ingredient, boxCenter)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp)
                .onGloballyPositioned { coords ->
                    val windowOffset = coords.positionInWindow()
                    val size = coords.size
                    boxCenter = Offset(
                        x = windowOffset.x + size.width / 2f,
                        y = windowOffset.y + size.height / 2f
                    )
                }
                .shadow(
                    if (sizeDp == 72.dp) 12.dp else 4.dp,
                    CircleShape,
                    ambientColor = ingredient.bgColor.copy(alpha = 0.5f),
                    spotColor = ingredient.bgColor
                )
                .background(ingredient.bgColor, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(ingredient.icon, fontSize = if (sizeDp == 72.dp) 28.sp else 22.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = customLabel ?: ingredient.displayName,
            fontSize = if (sizeDp == 72.dp) 12.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isLightText) Color.White.copy(alpha = 0.9f) else Color(0xFF555555)
        )
    }
}

/**
 * A dynamic popover that reveals sub-variants of an ingredient.
 * Coordinates alpha handoffs flawlessly with the DragOverlay engine.
 */
@Composable
fun SpatialPopover(
    isVisible: Boolean,
    variants: List<Ingredient>,
    isCheckout: Boolean,
    draggedIngredient: Ingredient?,
    onTap: (Ingredient) -> Unit,
    onDragStart: (Ingredient, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.0f,
            transformOrigin = TransformOrigin(0.5f, 1f),
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(tween(150)),
        exit = scaleOut(
            targetScale = 0.0f,
            transformOrigin = TransformOrigin(0.5f, 1f),
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200))
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    24.dp,
                    RoundedCornerShape(40.dp),
                    spotColor = Color(0xFF3E2723).copy(alpha = 0.25f)
                )
                .background(Color(0xFFFAF9F6).copy(alpha = 0.98f), RoundedCornerShape(40.dp))
                .border(1.dp, Color(0xFFD7CCC8).copy(alpha = 0.8f), RoundedCornerShape(40.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            variants.forEachIndexed { index, variant ->
                var itemVisible by remember { mutableStateOf(false) }

                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        delay(150L + (index * 120L))
                        itemVisible = true
                    } else {
                        itemVisible = false
                    }
                }

                val isBeingDragged = variant == draggedIngredient

                val itemScale by animateFloatAsState(
                    targetValue = if (itemVisible) 1f else 0.4f,
                    animationSpec = if (itemVisible) spring(
                        0.55f,
                        Spring.StiffnessMediumLow
                    ) else tween(150),
                    label = "scale"
                )
                val itemAlpha by animateFloatAsState(
                    targetValue = if (!itemVisible) 0f else if (isBeingDragged) 0f else 1f,
                    animationSpec = if (isBeingDragged || !itemVisible) tween(150) else tween(0),
                    label = "alpha"
                )
                val itemOffsetY by animateDpAsState(
                    targetValue = if (itemVisible) 0.dp else 32.dp,
                    animationSpec = if (itemVisible) spring(
                        0.5f,
                        Spring.StiffnessMediumLow
                    ) else tween(150),
                    label = "offset"
                )

                Box(modifier = Modifier.graphicsLayer {
                    scaleX = itemScale; scaleY = itemScale; alpha = itemAlpha; translationY =
                    itemOffsetY.toPx()
                }) {
                    DraggableIngredient(
                        ingredient = variant,
                        sizeDp = 56.dp,
                        onTap = { onTap(variant) },
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        isEnabled = !isCheckout
                    )
                }
            }
        }
    }
}

/**
 * Top animated segment selector determining the physical dimensions of the cup.
 */
@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun AnimatedSizeSelector(
    selectedSize: CoffeeSize,
    onSizeSelected: (CoffeeSize) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizes = CoffeeSize.entries
    val selectedIndex = sizes.indexOf(selectedSize)

    val indicatorOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "indicatorOffset"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .height(64.dp)
            .fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            val segmentWidth = maxWidth / sizes.size
            val leftEdge = indicatorOffset.coerceAtLeast(0f)
            val rightEdge = (indicatorOffset + 1f).coerceAtMost(sizes.size.toFloat())
            val pillWidth = ((rightEdge - leftEdge).coerceAtLeast(0f)) * segmentWidth
            val pillOffsetX = leftEdge * segmentWidth

            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .fillMaxHeight()
                    .offset(x = pillOffsetX)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2C1E16),
                                Color(0xFF140A05)
                            )
                        ), RoundedCornerShape(50)
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                sizes.forEach { size ->
                    val isSelected = selectedSize == size
                    val textScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "scale"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFFFD54F) else Color(0xFFA1887F),
                        animationSpec = tween(300),
                        label = "color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSizeSelected(size) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size.name,
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 13.sp,
                            modifier = Modifier.scale(textScale)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A beautiful active pill indicating an applied ingredient.
 * Designed with extensive internal padding boundaries to prevent layout clipping during bounce dynamics.
 */
@Composable
fun ActiveIngredientChip(
    ingredient: Ingredient,
    count: Int,
    onRemove: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val chipScale = remember { Animatable(0f) }
    val chipAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { chipAlpha.animateTo(1f, tween(150)) }
        launch {
            chipScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentScale = chipScale.value * if (isPressed) 0.90f else 1f

    val perfectPillShape = RoundedCornerShape(percent = 50)

    val fancyTextBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF3E2723),
                Color(0xFFD4AF37)
            ),
            start = Offset(0f, 0f),
            end = Offset(50f, 50f)
        )
    }

    Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp)) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    alpha = chipAlpha.value
                    rotationZ = (1f - chipScale.value) * 12f
                }
                .shadow(
                    elevation = 14.dp,
                    shape = perfectPillShape,
                    ambientColor = ingredient.bgColor.copy(alpha = 0.3f),
                    spotColor = ingredient.bgColor.copy(alpha = 0.5f)
                )
                .background(ingredient.bgColor, perfectPillShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.15f)
                        )
                    ),
                    shape = perfectPillShape
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f)
                        )
                    ),
                    shape = perfectPillShape
                )
                .clip(perfectPillShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (count <= 1) {
                            coroutineScope.launch {
                                launch { chipAlpha.animateTo(0f, tween(150)) }
                                chipScale.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                onRemove()
                            }
                        } else {
                            onRemove()
                        }
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .animateContentSize(
                        spring(
                            dampingRatio = 0.6f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = ingredient.icon, fontSize = 24.sp)

                AnimatedVisibility(
                    visible = count > 1,
                    enter = scaleIn(spring(dampingRatio = 0.6f)) + expandHorizontally(),
                    exit = scaleOut(spring(dampingRatio = 0.6f)) + shrinkHorizontally()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = " x",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(brush = fancyTextBrush),
                            modifier = Modifier.padding(start = 6.dp)
                        )

                        AnimatedContent(
                            targetState = count,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> height } + fadeOut())
                                }
                            },
                            label = "countRoll"
                        ) { targetCount ->
                            Text(
                                text = "$targetCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                style = androidx.compose.ui.text.TextStyle(brush = fancyTextBrush)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// SECTION 3: COMPLEX VISUALS & MORPHING CANVAS
// ==========================================

/**
 * A highly performant spatial background layer.
 * Utilizes `drawWithCache` and native `drawPoints` batching to ensure a zero-allocation,
 * 60+ FPS fluid backdrop that reacts automatically to AppState transitions.
 */
@Composable
fun GeometricCafeBackground(appState: AppState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "geometry")

    val fluidPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart),
        label = "fluid"
    )

    val rotation1 by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(90000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot1"
    )
    val rotation2 by transition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(120000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "rot2"
    )

    val isDark = appState != AppState.BREWING

    val bgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF140A05) else Color(0xFFFAF9F6),
        animationSpec = tween(1200), label = "bgColor"
    )
    val gridColor by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.03f) else Color(0xFFD7CCC8).copy(alpha = 0.3f),
        animationSpec = tween(1200), label = "gridColor"
    )
    val ghostPillColor by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.02f) else Color(0xFFEFEBE6),
        animationSpec = tween(1200), label = "ghostPillColor"
    )
    val bottomCircleColor by animateColorAsState(
        targetValue = if (isDark) Color.White.copy(alpha = 0.015f) else Color(0xFF3E2723).copy(alpha = 0.04f),
        animationSpec = tween(1200), label = "bottomCircleColor"
    )

    val uniqueSwirlAlpha by animateFloatAsState(
        targetValue = if (appState == AppState.PROCESSING) 1f else 0f,
        animationSpec = tween(1000), label = "swirlAlpha"
    )

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val dotSpacing = 80f

                // Highly optimized batch computation. This executes only once per layout change.
                val cachedGridPoints = mutableListOf<Offset>()
                for (x in 0..w.toInt() step dotSpacing.toInt()) {
                    for (y in 0..h.toInt() step dotSpacing.toInt()) {
                        cachedGridPoints.add(Offset(x.toFloat(), y.toFloat()))
                    }
                }

                onDrawBehind {
                    drawRect(color = bgColor)

                    if (uniqueSwirlAlpha > 0f) {
                        val cx = w / 2f + sin(fluidPhase) * (w * 0.25f)
                        val cy = h / 2f + cos(fluidPhase * 0.8f) * (h * 0.2f)

                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF4A3025).copy(alpha = uniqueSwirlAlpha),
                                    Color(0xFF2C1E16).copy(alpha = uniqueSwirlAlpha),
                                    Color.Transparent
                                ),
                                center = Offset(cx.toFloat(), cy.toFloat()),
                                radius = w * 1.3f
                            )
                        )
                    }

                    // Submits thousands of coordinates via a single low-level rendering call.
                    drawPoints(
                        points = cachedGridPoints,
                        pointMode = PointMode.Points,
                        color = gridColor,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )

                    val floatingOffset = sin(fluidPhase) * 40f

                    rotate(degrees = rotation1, pivot = Offset(w * 0.2f, h * 0.3f)) {
                        drawRoundRect(
                            color = ghostPillColor,
                            topLeft = Offset(-w * 0.2f, h * 0.1f),
                            size = Size(w * 1.2f, w * 0.6f),
                            cornerRadius = CornerRadius(w * 0.3f, w * 0.3f)
                        )
                    }

                    translate(top = floatingOffset) {
                        rotate(degrees = rotation2, pivot = Offset(w * 0.8f, h * 0.6f)) {
                            drawCircle(
                                color = Color(0xFFD4AF37).copy(alpha = if (isDark) 0.1f else 0.4f),
                                radius = w * 0.55f,
                                center = Offset(w * 0.8f, h * 0.6f),
                                style = Stroke(width = 3f)
                            )
                        }
                    }

                    val espressoX = (w * 0.15f) + (sin(fluidPhase * 2) * 20f)
                    val espressoY = (h * 0.75f) + (sin(fluidPhase) * -30f)
                    drawCircle(
                        color = bottomCircleColor,
                        radius = w * 0.4f,
                        center = Offset(espressoX, espressoY)
                    )
                }
            }
    )
}

/**
 * Custom geometry dynamically linking standard rectangular outlines with complex jagged paths.
 * Optimized to bypass expensive Path operations entirely when jagged progress is 0.
 */
class MorphingReceiptShape(private val cornerRadiusPx: Float, private val jaggedProgress: Float) :
    Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (jaggedProgress == 0f) {
            return Outline.Rounded(
                RoundRect(
                    Rect(Offset.Zero, size),
                    CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )
            )
        }

        val path = Path().apply {
            moveTo(0f, cornerRadiusPx)
            quadraticBezierTo(0f, 0f, cornerRadiusPx, 0f)
            lineTo(size.width - cornerRadiusPx, 0f)
            quadraticBezierTo(size.width, 0f, size.width, cornerRadiusPx)
            lineTo(size.width, size.height)

            val toothWidth = with(density) { 6.dp.toPx() }
            val toothHeight = with(density) { 4.dp.toPx() } * jaggedProgress
            val numTeeth = (size.width / toothWidth).toInt()
            for (i in 0 until numTeeth) {
                val x = size.width - (i * toothWidth)
                lineTo(x - (toothWidth / 2), size.height - toothHeight)
                lineTo(x - toothWidth, size.height)
            }
            lineTo(0f, cornerRadiusPx)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Animated loader interface. */
@Composable
fun SpatialDotsLoader() {
    val transition = rememberInfiniteTransition(label = "dots_loader")

    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val containerRotation by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "containerRotation"
    )

    val dotColors = listOf(
        Color(0xFFD4AF37),
        Color(0xFFFAF9F6),
        Color(0xFFA1887F)
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = containerRotation },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotColors.forEachIndexed { index, color ->
            val phaseOffset = index * (Math.PI / 2.5)
            val currentSin = sin(wave + phaseOffset).toFloat()

            val dotScale = 1f + (currentSin * 0.45f)
            val yTranslation = currentSin * -12f
            val dotAlpha = 0.6f + (currentSin * 0.4f)
            val glowElevation = 10f * ((currentSin + 1f) / 2f)

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(12.dp)
                    .graphicsLayer {
                        translationY = yTranslation
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotAlpha
                    }
                    .shadow(
                        elevation = glowElevation.dp,
                        shape = CircleShape,
                        spotColor = color,
                        ambientColor = color
                    )
                    .background(color, CircleShape)
            )
        }
    }
}

/**
 * Complex central staging hub drawing the 3D rendered cup, spatial loaders, and receipt matrix.
 * Implements severe rendering optimizations via hoisted Path resets and cached Brush instances.
 */
@Composable
fun MorphingCoffeeCup(
    coffeeSize: CoffeeSize,
    ingredientCount: Int,
    appState: AppState,
    orderState: OrderState,
    modifier: Modifier = Modifier
) {
    val targetWidth = when (appState) {
        AppState.BREWING -> when (coffeeSize) {
            CoffeeSize.SMALL -> 160.dp; CoffeeSize.MEDIUM -> 180.dp; CoffeeSize.LARGE -> 200.dp
        }

        AppState.PROCESSING -> 140.dp
        AppState.RECEIPT -> 320.dp
    }

    val targetHeight = when (appState) {
        AppState.BREWING -> when (coffeeSize) {
            CoffeeSize.SMALL -> 240.dp; CoffeeSize.MEDIUM -> 300.dp; CoffeeSize.LARGE -> 360.dp
        }

        AppState.PROCESSING -> 100.dp
        AppState.RECEIPT -> 480.dp
    }

    val animatedWidth by animateDpAsState(
        targetWidth,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "width"
    )
    val animatedHeight by animateDpAsState(
        targetHeight,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "height"
    )

    val paperAlpha by animateFloatAsState(
        targetValue = if (appState == AppState.RECEIPT) 1f else 0f,
        animationSpec = if (appState == AppState.RECEIPT) tween(500) else tween(0),
        label = "paperAlpha"
    )

    val cornerRadius by animateFloatAsState(
        targetValue = when (appState) {
            AppState.BREWING -> 0f; AppState.PROCESSING -> 200f; AppState.RECEIPT -> 24f
        },
        animationSpec = tween(400), label = "corner"
    )

    val shadowElevationDp by animateDpAsState(
        targetValue = if (appState == AppState.RECEIPT) 24.dp else 0.dp,
        animationSpec = if (appState == AppState.RECEIPT) tween(400) else tween(0),
        label = "shadow"
    )

    val jaggedProgress by animateFloatAsState(
        if (appState == AppState.RECEIPT) 1f else 0f,
        tween(400),
        label = "jaggedEdge"
    )

    val contentAlpha by animateFloatAsState(
        if (appState == AppState.BREWING) 1f else 0f,
        tween(300),
        label = "contentAlpha"
    )

    val scale = remember { Animatable(1f) }
    val splashAlpha = remember { Animatable(0f) }

    LaunchedEffect(ingredientCount) {
        if (ingredientCount > 0 && appState == AppState.BREWING) {
            launch {
                scale.animateTo(1.04f, spring(stiffness = Spring.StiffnessMedium))
                scale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch { splashAlpha.snapTo(0.5f); splashAlpha.animateTo(0f, tween(500)) }
        }
    }

    val cupBodyPath = remember { Path() }
    val sleevePath = remember { Path() }
    val domePath = remember { Path() }
    val domeBottomPath = remember { Path() }

    val cupBodyBrush = remember {
        Brush.horizontalGradient(
            0.0f to Color(0xFFE0E0E0),
            0.15f to Color(0xFFFFFFFF),
            0.45f to Color(0xFFD6D6D6),
            0.80f to Color(0xFF9E9E9E),
            1.0f to Color(0xFF757575)
        )
    }
    val sleeveBrush = remember {
        Brush.horizontalGradient(
            0.0f to Color(0xFF2C2F33),
            0.15f to Color(0xFF454A4E),
            0.45f to Color(0xFF1E2124),
            0.80f to Color(0xFF0D0F10),
            1.0f to Color(0xFF050607)
        )
    }
    val bandingBrush = remember {
        Brush.horizontalGradient(
            0.0f to Color(0xFF8D6E63),
            0.15f to Color(0xFFFFF59D),
            0.45f to Color(0xFFA1887F),
            0.80f to Color(0xFF4E342E),
            1.0f to Color(0xFF21110C)
        )
    }
    val lidRimBrush = remember {
        Brush.horizontalGradient(
            0.0f to Color(0xFFE0E0E0),
            0.15f to Color(0xFFFFFFFF),
            0.45f to Color(0xFFD6D6D6),
            0.80f to Color(0xFF9E9E9E),
            1.0f to Color(0xFF757575)
        )
    }
    val domeGradientBrush = remember {
        Brush.horizontalGradient(
            0.0f to Color(0xFFF5F5F5),
            0.15f to Color(0xFFFFFFFF),
            0.45f to Color(0xFFE0E0E0),
            0.80f to Color(0xFFAFAFAF),
            1.0f to Color(0xFF8A8A8A)
        )
    }

    Box(
        modifier = modifier
            .size(width = animatedWidth, height = animatedHeight)
            .scale(scale.value)
            .graphicsLayer {
                shadowElevation = shadowElevationDp.toPx()
                shape = MorphingReceiptShape(cornerRadius, jaggedProgress)
                clip = appState == AppState.RECEIPT
                ambientShadowColor = Color.Black.copy(alpha = 0.05f * paperAlpha)
                spotShadowColor = Color.Black.copy(alpha = 0.15f * paperAlpha)
            }
            .background(
                color = Color(0xFFFAF9F6).copy(alpha = paperAlpha),
                shape = MorphingReceiptShape(cornerRadius, jaggedProgress)
            ),
        contentAlignment = Alignment.Center
    ) {
        // --- LAYER 1: 3D CANVAS CUP ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }) {
            if (appState != AppState.BREWING) return@Canvas
            val canvasWidth = size.width
            val canvasHeight = size.height
            val lidHeight = canvasHeight * 0.12f
            val cupBodyTop = lidHeight * 0.85f
            val cupBodyHeight = canvasHeight - cupBodyTop
            val bottomTaperRatio = 0.72f
            val bottomWidth = canvasWidth * bottomTaperRatio
            val leftXBottom = (canvasWidth - bottomWidth) / 2f
            val rightXBottom = leftXBottom + bottomWidth

            drawOval(
                Brush.radialGradient(
                    listOf(
                        Color(0x80000000),
                        Color(0x20000000),
                        Color.Transparent
                    ), center = Offset((canvasWidth / 2f) + 15f, canvasHeight)
                ),
                topLeft = Offset(leftXBottom - 30f, canvasHeight - 15f),
                size = Size(bottomWidth + 80f, 30f)
            )

            // OPTIMIZATION: Recycle the path instances
            cupBodyPath.apply {
                reset()
                moveTo(0f, cupBodyTop)
                lineTo(canvasWidth, cupBodyTop)
                lineTo(rightXBottom, canvasHeight)
                lineTo(leftXBottom, canvasHeight)
                close()
            }

            drawPath(cupBodyPath, cupBodyBrush)

            val baseLiquidFill = when (coffeeSize) {
                CoffeeSize.SMALL -> 0.45f; CoffeeSize.MEDIUM -> 0.60f; CoffeeSize.LARGE -> 0.75f
            }
            val liquidHeight =
                cupBodyHeight * (baseLiquidFill + (ingredientCount * 0.05f)).coerceAtMost(0.9f)
            val liquidTopY = canvasHeight - liquidHeight

            clipPath(cupBodyPath) {
                drawRect(
                    Brush.horizontalGradient(
                        0.0f to Color(0xFF3E2723),
                        0.15f to Color(0xFF5D4037),
                        0.45f to Color(0xFF26140E),
                        0.80f to Color(0xFF140905),
                        1.0f to Color(0xFF0A0402)
                    ), topLeft = Offset(0f, liquidTopY), size = Size(canvasWidth, liquidHeight)
                )
                // Note: explicit bounds vertical gradients must be declared during the draw cycle
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x22000000),
                            Color(0x99120602)
                        ), startY = liquidTopY, endY = canvasHeight
                    ), topLeft = Offset(0f, liquidTopY), size = Size(canvasWidth, liquidHeight)
                )
                drawOval(
                    color = Color(0xFF050201).copy(alpha = 0.5f),
                    topLeft = Offset(0f, liquidTopY - 6f),
                    size = Size(canvasWidth, 12f)
                )
            }

            val sleeveHeight = cupBodyHeight * 0.40f
            val sleeveTopY = cupBodyTop + (cupBodyHeight - sleeveHeight) / 2.5f
            val sleeveBottomY = sleeveTopY + sleeveHeight

            fun getXAtY(y: Float): Pair<Float, Float> {
                val progress = (y - cupBodyTop) / cupBodyHeight
                return (leftXBottom * progress) to (canvasWidth + (rightXBottom - canvasWidth) * progress)
            }
            val (sLeftT, sRightT) = getXAtY(sleeveTopY)
            val (sLeftB, sRightB) = getXAtY(sleeveBottomY)

            sleevePath.apply {
                reset()
                moveTo(sLeftT, sleeveTopY)
                lineTo(sRightT, sleeveTopY)
                lineTo(sRightB, sleeveBottomY)
                lineTo(sLeftB, sleeveBottomY)
                close()
            }

            drawPath(sleevePath, sleeveBrush)

            val brandingLineY = sleeveBottomY - (sleeveHeight * 0.1f)
            val (bLeft, bRight) = getXAtY(brandingLineY)
            drawLine(
                brush = bandingBrush,
                start = Offset(bLeft, brandingLineY),
                end = Offset(bRight, brandingLineY),
                strokeWidth = 3f
            )

            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                ), topLeft = Offset(0f, cupBodyTop), size = Size(canvasWidth, 16f)
            )

            val lidRimHeight = lidHeight * 0.35f
            val lidRimY = lidHeight - lidRimHeight
            drawRoundRect(
                Color.Black.copy(alpha = 0.15f),
                topLeft = Offset(-3f, lidRimY + 4f),
                size = Size(canvasWidth + 6f, lidRimHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                brush = lidRimBrush,
                topLeft = Offset(-4f, lidRimY),
                size = Size(canvasWidth + 8f, lidRimHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            domePath.apply {
                reset()
                moveTo(canvasWidth * 0.08f, lidRimY)
                lineTo(canvasWidth * 0.18f, 0f)
                lineTo(canvasWidth * 0.82f, 0f)
                lineTo(canvasWidth * 0.92f, lidRimY)
                close()
            }
            drawPath(domePath, domeGradientBrush)

            domeBottomPath.apply {
                reset()
                moveTo(canvasWidth * 0.08f, lidRimY)
                lineTo(canvasWidth * 0.92f, lidRimY)
                lineTo(canvasWidth * 0.88f, lidRimY - 6f)
                lineTo(canvasWidth * 0.12f, lidRimY - 6f)
                close()
            }
            drawPath(
                domeBottomPath,
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                    startY = lidRimY - 6f,
                    endY = lidRimY
                )
            )
            drawOval(
                Brush.radialGradient(
                    listOf(Color(0xFF050505), Color(0xFF222222)),
                    center = Offset(canvasWidth * 0.5f, lidHeight * 0.10f)
                ),
                topLeft = Offset(canvasWidth * 0.42f, lidHeight * 0.05f),
                size = Size(canvasWidth * 0.16f, lidHeight * 0.12f)
            )
        }

        // --- LAYER 2: SPATIAL LOADER ---
        AnimatedVisibility(
            visible = appState == AppState.PROCESSING,
            enter = fadeIn(tween(delayMillis = 200, durationMillis = 300)),
            exit = fadeOut(tween(200))
        ) {
            SpatialDotsLoader()
        }

        // --- LAYER 3: RECEIPT CONTENT ---
        AnimatedVisibility(
            visible = appState == AppState.RECEIPT,
            enter = fadeIn(tween(delayMillis = 300, durationMillis = 400)),
            exit = fadeOut(tween(100))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val receiptFont = androidx.compose.ui.text.font.FontFamily.Monospace
                val inkColor = Color(0xFF2B2B2B)
                val lightInk = Color(0xFF666666)

                Text(
                    "C A F E  C O M P O S E",
                    fontFamily = receiptFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = inkColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "123 Jetpack Lane",
                    fontFamily = receiptFont,
                    fontSize = 10.sp,
                    color = lightInk
                )
                Text(
                    "Terminal #04 • Cashier: Jet",
                    fontFamily = receiptFont,
                    fontSize = 10.sp,
                    color = lightInk
                )
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "DATE: 10/24/2026",
                        fontFamily = receiptFont,
                        fontSize = 10.sp,
                        color = lightInk
                    )
                    Text(
                        "TIME: 08:42 AM",
                        fontFamily = receiptFont,
                        fontSize = 10.sp,
                        color = lightInk
                    )
                }
                Spacer(Modifier.height(12.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                ) {
                    drawLine(
                        lightInk,
                        Offset(0f, 0f),
                        Offset(size.width, 0f),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f),
                            0f
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "QTY ITEM",
                        fontFamily = receiptFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                    Text(
                        "PRICE",
                        fontFamily = receiptFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "1   ${orderState.size.name} COFFEE",
                        fontFamily = receiptFont,
                        fontSize = 14.sp,
                        color = inkColor
                    )
                    Text(
                        "$${String.format("%.2f", orderState.size.basePrice)}",
                        fontFamily = receiptFont,
                        fontSize = 14.sp,
                        color = inkColor
                    )
                }
                Spacer(Modifier.height(4.dp))

                if (orderState.ingredients.isNotEmpty()) {
                    val groupedIngredients = orderState.ingredients.groupingBy { it }.eachCount()
                    groupedIngredients.forEach { (ingredient, count) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "      + ${count}x ${ingredient.displayName.uppercase()}",
                                fontFamily = receiptFont,
                                fontSize = 12.sp,
                                color = lightInk
                            )
                            Text(
                                "$${String.format("%.2f", ingredient.price * count)}",
                                fontFamily = receiptFont,
                                fontSize = 12.sp,
                                color = lightInk
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                ) {
                    drawLine(
                        lightInk,
                        Offset(0f, 0f),
                        Offset(size.width, 0f),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(
                                10f,
                                10f
                            ), 0f
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))

                // OPTIMIZATION: Math derived during drawing Phase.
                // Uses values that are static given the orderState dependency above.
                val subtotal = orderState.size.basePrice + orderState.ingredients.sumOf { it.price }
                val tax = subtotal * 0.08
                val total = subtotal + tax

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "SUBTOTAL",
                        fontFamily = receiptFont,
                        fontSize = 12.sp,
                        color = lightInk
                    )
                    Text(
                        "$${String.format("%.2f", subtotal)}",
                        fontFamily = receiptFont,
                        fontSize = 12.sp,
                        color = lightInk
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TAX (8%)",
                        fontFamily = receiptFont,
                        fontSize = 12.sp,
                        color = lightInk
                    )
                    Text(
                        "$${String.format("%.2f", tax)}",
                        fontFamily = receiptFont,
                        fontSize = 12.sp,
                        color = lightInk
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "TOTAL",
                        fontFamily = receiptFont,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                    Text(
                        "$${String.format("%.2f", total)}",
                        fontFamily = receiptFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkColor
                    )
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "CARD - APPROVED",
                    fontFamily = receiptFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = inkColor
                )
                Spacer(Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(44.dp)
                    ) {
                        val bitmask =
                            "10110011010110111001011010011101011011100101101011100101101110010110"
                        val moduleWidth = size.width / bitmask.length
                        bitmask.forEachIndexed { index, bit ->
                            if (bit == '1') drawRect(
                                color = inkColor,
                                topLeft = Offset(index * moduleWidth, 0f),
                                size = Size(moduleWidth + 0.5f, size.height)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.75f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0", fontFamily = receiptFont, fontSize = 10.sp, color = inkColor)
                        Text("12345", fontFamily = receiptFont, fontSize = 10.sp, color = inkColor)
                        Text("67890", fontFamily = receiptFont, fontSize = 10.sp, color = inkColor)
                        Text("5", fontFamily = receiptFont, fontSize = 10.sp, color = inkColor)
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 4: MAIN SCREEN ORCHESTRATION
// ==========================================

/**
 * Top-level view orchestrating the entire lifecycle of a coffee order.
 * Connects the data models to the spatial drag-and-drop mechanics.
 */
@Composable
fun CoffeeOrderScreen() {
    var appState by remember { mutableStateOf(AppState.BREWING) }
    val isBrewing = appState == AppState.BREWING

    var orderState by remember { mutableStateOf(OrderState()) }
    var dragState by remember { mutableStateOf<DragState>(DragState.None) }
    var cupDropZoneBounds by remember { mutableStateOf(Rect.Zero) }
    var popoverState by remember { mutableStateOf<IngredientCategory?>(null) }

    val activeDraggedIngredient = when (val state = dragState) {
        is DragState.Dragging -> state.ingredient
        is DragState.Returning -> state.ingredient
        else -> null
    }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. AMBIENT BACKGROUND ---
        GeometricCafeBackground(appState = appState, modifier = Modifier.fillMaxSize())

        // --- 2. TOP SIZE SELECTOR ---
        AnimatedVisibility(
            visible = isBrewing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp),
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { -50 }),
            exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.6f) + slideOutVertically(
                targetOffsetY = { 200 })
        ) {
            AnimatedSizeSelector(
                selectedSize = orderState.size,
                onSizeSelected = { orderState = orderState.copy(size = it) }
            )
        }

        // --- 3. ACTIVE INGREDIENTS BADGES ---
        AnimatedVisibility(
            visible = isBrewing && orderState.ingredients.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 140.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            enter = fadeIn(tween(300)) + scaleIn(spring(dampingRatio = 0.55f)) + slideInVertically(
                initialOffsetY = { -30 }),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f)
        ) {
            val groupedIngredients = orderState.ingredients.groupingBy { it }.eachCount()
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.animateContentSize(
                    spring(
                        dampingRatio = 0.6f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                groupedIngredients.forEach { (ingredient, count) ->
                    androidx.compose.runtime.key(ingredient.displayName) {
                        ActiveIngredientChip(ingredient = ingredient, count = count, onRemove = {
                            val updatedList = orderState.ingredients.toMutableList()
                            updatedList.remove(ingredient)
                            orderState = orderState.copy(ingredients = updatedList)
                        })
                    }
                }
            }
        }

        // --- 4. STAGING AREA (CUP / LOADER / RECEIPT) ---
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .onGloballyPositioned { coordinates ->
                    cupDropZoneBounds = coordinates.boundsInRoot()
                }
        ) {
            MorphingCoffeeCup(
                coffeeSize = orderState.size,
                ingredientCount = orderState.ingredients.size,
                appState = appState,
                orderState = orderState
            )
        }

        // --- 5. SCRIM ---
        AnimatedVisibility(
            visible = popoverState != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.04f))
                    .pointerInput(Unit) { detectTapGestures(onTap = { popoverState = null }) })
        }

        // --- 6. FLOATING SPATIAL POPOVERS ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 248.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            BaseIngredients.forEach { baseIngredient ->
                val variants = when (baseIngredient.category) {
                    IngredientCategory.SUGAR -> SugarOptions; IngredientCategory.MILK -> MilkOptions; IngredientCategory.DRINK -> DrinkOptions; else -> emptyList()
                }

                SpatialPopover(
                    isVisible = popoverState == baseIngredient.category,
                    variants = variants,
                    isCheckout = !isBrewing,
                    draggedIngredient = activeDraggedIngredient,
                    onTap = { variant ->
                        orderState = orderState.copy(ingredients = orderState.ingredients + variant)
                        popoverState = null
                    },
                    onDragStart = { ing, startOffset ->
                        dragState = DragState.Dragging(ing, startOffset, startOffset)
                    },
                    onDrag = { dragAmount ->
                        if (dragState is DragState.Dragging) {
                            val current = dragState as DragState.Dragging
                            dragState =
                                current.copy(currentPosition = current.currentPosition + dragAmount)
                        }
                    },
                    onDragEnd = {
                        if (dragState is DragState.Dragging) {
                            val current = dragState as DragState.Dragging
                            if (cupDropZoneBounds.contains(current.currentPosition)) {
                                orderState =
                                    orderState.copy(ingredients = orderState.ingredients + current.ingredient)
                                dragState = DragState.None
                                popoverState = null
                            } else {
                                dragState =
                                    DragState.Returning(current.ingredient, current.initialPosition)
                            }
                        }
                    }
                )
            }
        }

        // --- 7. MAIN INGREDIENT DOCK ---
        AnimatedVisibility(
            visible = isBrewing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 144.dp),
            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 50 }),
            exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.6f) + slideOutVertically(
                targetOffsetY = { -200 })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                BaseIngredients.forEach { baseIngredient ->
                    val isPopoverOpen = popoverState == baseIngredient.category
                    val buttonScale by animateFloatAsState(
                        if (isPopoverOpen) 0.85f else 1f,
                        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                        label = "buttonScale"
                    )
                    val dockCategoryName = when (baseIngredient.category) {
                        IngredientCategory.SUGAR -> "Sugar"; IngredientCategory.MILK -> "Milk"; IngredientCategory.DRINK -> "Drinks"
                    }

                    val isBeingDragged = baseIngredient == activeDraggedIngredient
                    val buttonAlpha by animateFloatAsState(
                        targetValue = if (isBeingDragged) 0f else 1f,
                        animationSpec = if (isBeingDragged) tween(150) else tween(0),
                        label = "buttonAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .scale(buttonScale)
                            .graphicsLayer { alpha = buttonAlpha }
                    ) {
                        DraggableIngredient(
                            ingredient = baseIngredient,
                            customLabel = dockCategoryName,
                            sizeDp = 72.dp,
                            isEnabled = isBrewing,
                            isLightText = false,
                            onTap = {
//                                if (baseIngredient.category == IngredientCategory.DRINK) {
//                                    orderState =
//                                        orderState.copy(ingredients = orderState.ingredients + baseIngredient)
//                                } else {
                                    popoverState =
                                        if (popoverState == baseIngredient.category) null else baseIngredient.category
                                //}
                            },
                            onDragStart = { ing, startOffset ->
                                popoverState = null
                                dragState = DragState.Dragging(
                                    ingredient = ing,
                                    initialPosition = startOffset,
                                    currentPosition = startOffset
                                )
                            },
                            onDrag = { dragAmount ->
                                if (dragState is DragState.Dragging) {
                                    val current = dragState as DragState.Dragging
                                    dragState =
                                        current.copy(currentPosition = current.currentPosition + dragAmount)
                                }
                            },
                            onDragEnd = {
                                if (dragState is DragState.Dragging) {
                                    val current = dragState as DragState.Dragging
                                    if (cupDropZoneBounds.contains(current.currentPosition)) {
                                        orderState =
                                            orderState.copy(ingredients = orderState.ingredients + current.ingredient)
                                        dragState = DragState.None
                                    } else {
                                        dragState = DragState.Returning(
                                            current.ingredient,
                                            current.initialPosition
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- 8. CHECKOUT BUTTON ---
        val currentTotal = remember(orderState) {
            orderState.size.basePrice + orderState.ingredients.sumOf { it.price }
        }

        val buttonText = when (appState) {
            AppState.BREWING -> "SWIPE TO PAY • $${String.format("%.2f", currentTotal)}"
            AppState.PROCESSING -> "CRAFTING ORDER..."
            AppState.RECEIPT -> "BREW ANOTHER"
        }

        PremiumGradientButton(
            text = buttonText,
            onClick = {
                when (appState) {
                    AppState.BREWING -> {
                        appState = AppState.PROCESSING
                        popoverState = null

                        coroutineScope.launch {
                            delay(2500)
                            appState = AppState.RECEIPT
                        }
                    }

                    AppState.PROCESSING -> { /* No-op */
                    }

                    AppState.RECEIPT -> {
                        orderState = OrderState()
                        appState = AppState.BREWING
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 32.dp)
                .fillMaxWidth()
        )

        // --- 9. GLOBAL DRAG OVERLAY ---
        if (dragState !is DragState.None) {
            val isReturning = dragState is DragState.Returning
            val draggedIngredient = when (val state = dragState) {
                is DragState.Dragging -> state.ingredient
                is DragState.Returning -> state.ingredient
                else -> return@Box
            }

            val targetPosition = when (val state = dragState) {
                is DragState.Dragging -> state.currentPosition
                is DragState.Returning -> state.initialPosition
                else -> Offset.Zero
            }

            val superSlowReturnSpec =
                tween<Offset>(durationMillis = 1000, easing = FastOutSlowInEasing)
            val superSlowFloatSpec =
                tween<Float>(durationMillis = 1000, easing = FastOutSlowInEasing)

            val displayPosition by animateOffsetAsState(
                targetValue = targetPosition,
                animationSpec = if (isReturning) superSlowReturnSpec else snap(),
                finishedListener = {
                    if (dragState is DragState.Returning) {
                        dragState = DragState.None
                        popoverState = null
                    }
                },
                label = "dragPosition"
            )

            val overlayScale by animateFloatAsState(
                targetValue = if (isReturning) 1.0f else 1.2f,
                animationSpec = if (isReturning) superSlowFloatSpec else spring(
                    0.6f,
                    Spring.StiffnessMedium
                ),
                label = "overlayScale"
            )

            val overlayRotation by animateFloatAsState(
                targetValue = if (isReturning) 0f else 5f,
                animationSpec = if (isReturning) superSlowFloatSpec else spring(
                    0.6f,
                    Spring.StiffnessMedium
                ),
                label = "overlayRotation"
            )

            val density = LocalDensity.current
            val halfSizePx = with(density) { 36.dp.toPx() }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (displayPosition.x - halfSizePx).roundToInt(),
                            (displayPosition.y - halfSizePx).roundToInt()
                        )
                    }
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = overlayScale
                        scaleY = overlayScale
                        rotationZ = overlayRotation
                    }
                    .shadow(if (isReturning) 12.dp else 24.dp, CircleShape)
                    .background(draggedIngredient.bgColor, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = draggedIngredient.icon, fontSize = 28.sp)
            }
        }
    }
}

/**
 * Aesthetic CTA button supporting an animated inner gradient cycle.
 */
@Composable
fun PremiumGradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.96f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "buttonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "buttonGradient")
    val phase by infiniteTransition.animateFloat(
        0f,
        1000f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                if (isPressed) 8.dp else 24.dp,
                RoundedCornerShape(32.dp),
                spotColor = Color(0xFFD4AF37).copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF2C1E16),
                        Color(0xFF0A0502),
                        Color(0xFF3E2723)
                    ), Offset(phase, 0f), Offset(phase + 600f, 300f)
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFD54F).copy(alpha = 0.8f),
                        Color(0xFFFFD54F).copy(alpha = 0.1f),
                        Color(0xFFFFD54F).copy(alpha = 0.5f)
                    ), Offset(0f, 0f), Offset(1000f - phase, 500f)
                ),
                RoundedCornerShape(32.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFAF9F6),
            letterSpacing = 1.5.sp
        )
    }
}