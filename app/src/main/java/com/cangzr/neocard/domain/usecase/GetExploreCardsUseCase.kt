package com.cangzr.neocard.domain.usecase

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

class GetExploreCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
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

