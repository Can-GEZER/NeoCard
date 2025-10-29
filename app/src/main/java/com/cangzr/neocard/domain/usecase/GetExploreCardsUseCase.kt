package com.cangzr.neocard.domain.usecase

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

/**
 * GetExploreCardsUseCase handles retrieving public cards for exploration.
 * 
 * This use case retrieves publicly available cards that users can discover and view.
 * Cards are filtered to exclude the current user's own cards and are paginated
 * for efficient loading.
 * 
 * **Business Rules:**
 * - Only returns cards where [UserCard.isPublic] is true
 * - Excludes cards owned by [currentUserId]
 * - Returns paginated results for performance
 * 
 * @param cardRepository Repository for card data operations
 * 
 * @see [UserCard] Card data model
 * @see com.cangzr.neocard.data.repository.CardRepository Card repository interface
 * @see com.cangzr.neocard.ui.screens.home.viewmodels.HomeViewModel ViewModel using this use case
 * 
 * @since 1.0
 */
class GetExploreCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Retrieves public cards for exploration with pagination support.
     * 
     * This method fetches publicly available cards that are not owned by the current user.
     * Cards are returned in pages for efficient loading of large collections.
     * 
     * **Pagination:**
     * - First page: Pass `lastCardId = null`
     * - Subsequent pages: Pass the ID of the last card from previous page
     * 
     * @param currentUserId ID of the current user (their cards will be excluded)
     * @param pageSize Number of cards to retrieve per page (default: 10)
     * @param lastCardId ID of the last card from previous page, or null for first page
     * @return [Resource.Success] containing Triple of (cards list, lastCardId, hasMore),
     *         or [Resource.Error] if retrieval fails
     * 
     * @throws No exceptions thrown - all errors are wrapped in [Resource.Error]
     * 
     * @see [UserCard] Card data model
     * @see [Resource] Result wrapper
     * 
     * @since 1.0
     */
    suspend operator fun invoke(
        currentUserId: String,
        pageSize: Int = 10,
        lastCardId: String? = null
    ): Resource<Triple<List<UserCard>, String?, Boolean>> {
        return cardRepository.getExploreCards(
            currentUserId = currentUserId,
            pageSize = pageSize,
            lastCardId = lastCardId
        )
    }
}

