package com.cangzr.neocard

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Business : Screen("business")
    object Profile : Screen("profile")
    object Auth : Screen("auth")
    object ConnectionRequests : Screen("connection_requests")
    object CreateCard : Screen("create_card")
    object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: String) = "card_detail/$cardId"
    }
    object SharedCardDetail : Screen("shared_card_detail/{cardId}") {
        fun createRoute(cardId: String) = "shared_card_detail/$cardId"
    }
    object CardStatistics : Screen("statistics/{cardId}") {
        fun createRoute(cardId: String) = "statistics/$cardId"
    }
    object ExploreAllCards : Screen("explore_all_cards")
    object Notifications : Screen("notifications")
}
