package com.example.ordercoffeeapp

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class CoffeeOrderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState

    fun setOrderState(order: OrderState) {
        _uiState.update {
            it.copy(order = order)
        }
    }
    fun setPopoverState(category: IngredientCategory?) {
        _uiState.update {
            it.copy(popoverVisibleFor = category)
        }
    }

    fun setDragState(dragState: DragState) {
        _uiState.update {
            it.copy(dragState = dragState)
        }
    }

    fun clearDrag() {
        _uiState.update {
            it.copy(dragState = DragState.None)
        }
    }

    // ------------------------------------------------
    //      BASE CATEGORY MANAGEMENT
    // ------------------------------------------------

    fun resetCategories() {
        _uiState.update {
            it.copy(
                baseCategories = listOf(
                    OptionCategory(
                        category = IngredientCategory.DRINK,
                        options = DrinkOptions
                    )
                )
            )
        }
    }

    fun addCategory(category: OptionCategory) {
        _uiState.update {
            it.copy(baseCategories = it.baseCategories + category)
        }
    }

    // ------------------------------------------------
    //      INGREDIENTS UPDATE (REPLACE OR ADD)
    // ------------------------------------------------

    fun addOrReplaceIngredient(newIngredient: Ingredient) {
        _uiState.update { state ->

            val updatedList = when (newIngredient.category) {

                IngredientCategory.SUGAR -> {
                    // Siempre se agrega (no reemplaza)
                    state.order.ingredients + newIngredient
                }

                IngredientCategory.DRINK,
                IngredientCategory.MILK -> {

                    val exists = state.order.ingredients.any {
                        it.category == newIngredient.category
                    }

                    if (exists) {
                        state.order.ingredients.map {
                            if (it.category == newIngredient.category) newIngredient else it
                        }
                    } else {
                        state.order.ingredients + newIngredient
                    }
                }
            }

            val updatedCategories =
                if (updatedList.isNotEmpty() && state.baseCategories.size == 1) {
                    state.baseCategories + listOf(
                        OptionCategory(IngredientCategory.SUGAR, SugarOptions),
                        OptionCategory(IngredientCategory.MILK, MilkOptions)
                    )
                } else state.baseCategories

            val resetCategories =
                if (updatedList.isEmpty()) listOf(
                    OptionCategory(IngredientCategory.DRINK, DrinkOptions)
                ) else updatedCategories

            state.copy(
                order = state.order.copy(
                    ingredients = updatedList
                ),
                baseCategories = resetCategories,
                popoverVisibleFor = null
            )
        }
    }

    fun removeIngredient(ingredient: Ingredient) {
        _uiState.update { state ->

            val updatedIngredients =
                if (ingredient.category == IngredientCategory.DRINK) {
                    emptyList()
                } else {
                    state.order.ingredients.toMutableList().apply {
                        remove(ingredient)
                    }
                }

            val hasDrink =
                updatedIngredients.any { it.category == IngredientCategory.DRINK }

            val updatedCategories =
                if (hasDrink) {
                    listOf(
                        OptionCategory(IngredientCategory.DRINK, DrinkOptions),
                        OptionCategory(IngredientCategory.MILK, MilkOptions),
                        OptionCategory(IngredientCategory.SUGAR, SugarOptions)
                    )
                } else {
                    listOf(
                        OptionCategory(IngredientCategory.DRINK, DrinkOptions)
                    )
                }

            state.copy(
                order = state.order.copy(
                    ingredients = updatedIngredients
                ),
                baseCategories = updatedCategories
            )
        }
    }

    // ------------------------------------------------
    // POPUP
    // ------------------------------------------------

    fun openPopover(category: IngredientCategory) {
        _uiState.update {
            it.copy(popoverVisibleFor = category)
        }
    }

    fun closePopover() {
        _uiState.update {
            it.copy(popoverVisibleFor = null)
        }
    }

    // ------------------------------------------------
    // DRAG
    // ------------------------------------------------

    fun startDrag(ingredient: Ingredient, start: Offset) {
        _uiState.update {
            it.copy(
                dragState = DragState.Dragging(
                    ingredient,
                    start,
                    start
                )
            )
        }
    }

    fun updateDrag(offset: Offset) {
        val current = _uiState.value.dragState
        if (current is DragState.Dragging) {
            _uiState.update {
                it.copy(
                    dragState = current.copy(currentPosition = offset)
                )
            }
        }
    }

    fun endDrag(isDroppedInsideCup: Boolean, cupPosition: Offset) {
        val current = _uiState.value.dragState
        if (current is DragState.Dragging) {

            if (isDroppedInsideCup) {
                addOrReplaceIngredient(current.ingredient)

                _uiState.update {
                    it.copy(
                        dragState = DragState.None,
                        popoverVisibleFor = null
                    )
                }

            } else {
                _uiState.update {
                    it.copy(
                        dragState = DragState.Returning(
                            ingredient = current.ingredient,
                            targetPosition = current.initialPosition
                        )
                    )
                }
            }
        }
    }
}