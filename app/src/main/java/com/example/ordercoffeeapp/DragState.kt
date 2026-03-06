package com.example.ordercoffeeapp

import androidx.compose.ui.geometry.Offset


/**
 * Defines the physics-based drag states for ingredient interaction.
 * Provides granular tracking of an ingredient's origin and current floating point.
 */

sealed class DragState {
    object None : DragState()

    data class Dragging(
        val ingredient: Ingredient,
        val initialPosition: Offset,
        val currentPosition: Offset
    ) : DragState()

    data class Returning(
        val ingredient: Ingredient,
        val targetPosition: Offset
    ) : DragState()
}