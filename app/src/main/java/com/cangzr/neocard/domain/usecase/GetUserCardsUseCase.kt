package com.cangzr.neocard.domain.usecase

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

/**
 * GetUserCardsUseCase handles retrieving a user's business cards with pagination support.
 * 
 * This use case provides paginated access to user cards, allowing efficient loading
 * of large card collections. Cards are returned in creation order (newest first).
 * 
 * **Pagination:**
 * - First page: Pass `lastCardId = null`
 * - Subsequent pages: Pass the ID of the last card from previous page
 * - Page size defaults to 10 but can be customized
 * 
 * **Return Value:**
 * Returns a [Triple] containing:
 * 1. List of [UserCard] for current page
 * 2. Last card ID (for next page) or null if no more pages
 * 3. Boolean indicating if more cards are available
 * 
 * @param cardRepository Repository for card data operations
 * 
 * @see [UserCard] Card data model
 * @see com.cangzr.neocard.data.repository.CardRepository Card repository interface
 * @see com.cangzr.neocard.ui.screens.home.viewmodels.HomeViewModel ViewModel using this use case
 * 
 * @since 1.0
 */
class GetUserCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Retrieves user's business cards with pagination support.
     * 
     * This method fetches cards for the specified user in pages. For the first page,
     * pass `null` as [lastCardId]. For subsequent pages, pass the ID of the last card
     * from the previous page.
     * 
     * **Example Usage:**
     * ```
     * // First page
     * val result = useCase(userId, pageSize = 20, lastCardId = null)
     * 
     * // Next page
     * val nextResult = useCase(userId, pageSize = 20, lastCardId = result.data.second)
     * ```
     * 
     * @param userId The ID of the user whose cards to retrieve (must be authenticated)
     * @param pageSize Number of cards to retrieve per page (default: 10)
     * @param lastCardId ID of the last card from previous page, or null for first page
     * @return [Resource.Success] containing Triple of (cards list, lastCardId, hasMore),
     *         or [Resource.Error] if retrieval fails
     * 
     * @throws No exceptions thrown - all errors are wrapped in [Resource.Error]
     * 
     * @see [UserCard] Card data model
     * @see [Resource] Result wrapper
     * @see com.cangzr.neocard.ui.screens.home.viewmodels.HomeViewModel ViewModel implementation
     * 
     * @since 1.0
     */
    suspend operator fun invoke(
        userId: String,
        pageSize: Int = 10,
        lastCardId: String? = null
    ): Resource<Triple<List<UserCard>, String?, Boolean>> {
        return cardRepository.getCards(
            userId = userId,
            pageSize = pageSize,
            lastCardId = lastCardId
        )
    }
}

