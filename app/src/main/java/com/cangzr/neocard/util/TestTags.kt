package com.cangzr.neocard.util

/**
 * Test tag constants for Compose UI testing
 * 
 * These tags are used to identify UI components in tests.
 * They should be applied to key composables using Modifier.testTag()
 */
object TestTags {
    
    // HomeScreen tags
    const val HOME_SCREEN = "home_screen"
    const val HOME_LOGIN_PROMPT = "home_login_prompt"
    const val HOME_LOADING = "home_loading"
    const val HOME_ERROR = "home_error"
    const val HOME_EMPTY_STATE = "home_empty_state"
    const val HOME_CARD_LIST = "home_card_list"
    const val HOME_CARD_ITEM = "home_card_item_"  // Append card ID
    const val HOME_FILTER_DROPDOWN = "home_filter_dropdown"
    const val HOME_SEARCH_FIELD = "home_search_field"
    const val HOME_EXPLORE_SECTION = "home_explore_section"
    
    // CreateCardScreen tags
    const val CREATE_CARD_SCREEN = "create_card_screen"
    const val CREATE_CARD_TITLE = "create_card_title"
    const val CREATE_CARD_SAVE_BUTTON = "create_card_save_button"
    const val CREATE_CARD_LOADING = "create_card_loading"
    const val CREATE_CARD_PREVIEW = "create_card_preview"
    
    // Form field tags
    const val FIELD_NAME = "field_name"
    const val FIELD_SURNAME = "field_surname"
    const val FIELD_EMAIL = "field_email"
    const val FIELD_PHONE = "field_phone"
    const val FIELD_COMPANY = "field_company"
    const val FIELD_TITLE = "field_title"
    const val FIELD_WEBSITE = "field_website"
    const val FIELD_LINKEDIN = "field_linkedin"
    const val FIELD_INSTAGRAM = "field_instagram"
    const val FIELD_TWITTER = "field_twitter"
    const val FIELD_FACEBOOK = "field_facebook"
    const val FIELD_GITHUB = "field_github"
    
    // Card type selector
    const val CARD_TYPE_SELECTOR = "card_type_selector"
    const val CARD_TYPE_CHIP = "card_type_chip_"  // Append card type name
    
    // Background selector
    const val BACKGROUND_TYPE_SOLID = "background_type_solid"
    const val BACKGROUND_TYPE_GRADIENT = "background_type_gradient"
    const val COLOR_PICKER = "color_picker"
    const val GRADIENT_PICKER = "gradient_picker"
    
    // Public/Private toggle
    const val PUBLIC_PRIVATE_SWITCH = "public_private_switch"
    
    // Premium dialog
    const val PREMIUM_DIALOG = "premium_dialog"
    const val PREMIUM_DIALOG_CLOSE = "premium_dialog_close"
    const val PREMIUM_DIALOG_UPGRADE = "premium_dialog_upgrade"
    
    // Card detail dialog
    const val CARD_DIALOG = "card_dialog"
    const val CARD_DIALOG_VIEW_DETAILS = "card_dialog_view_details"
    const val CARD_DIALOG_SHARE = "card_dialog_share"
    const val CARD_DIALOG_DISMISS = "card_dialog_dismiss"
    
    // Share bottom sheet
    const val SHARE_BOTTOM_SHEET = "share_bottom_sheet"
    
    // Common tags
    const val ERROR_MESSAGE = "error_message"
    const val SUCCESS_MESSAGE = "success_message"
    const val RETRY_BUTTON = "retry_button"
    const val CIRCULAR_PROGRESS = "circular_progress"
}

