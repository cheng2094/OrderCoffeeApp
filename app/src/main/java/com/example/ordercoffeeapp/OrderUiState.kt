package com.example.ordercoffeeapp

data class OrderUiState(
    val order: OrderState = OrderState(),
    val baseCategories: List<OptionCategory> = listOf(
        OptionCategory(
            category = IngredientCategory.DRINK,
            options = DrinkOptions
        )
    ),
    val popoverVisibleFor: IngredientCategory? = null,
    val dragState: DragState = DragState.None
)

/** Represents the user's active configuration of their drink. */
data class OrderState(
    val size: CoffeeSize = CoffeeSize.MEDIUM,
    val ingredients: List<Ingredient> = emptyList()
) {
    val totalPrice: Double
        get() = size.basePrice + ingredients.sumOf { it.price }
}