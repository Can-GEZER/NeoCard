package com.cangzr.neocard.domain.usecase

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

class GetCardUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    suspend operator fun invoke(
        userId: String,
        cardId: String
    ): Resource<UserCard?> {
        return cardRepository.getCardById(
            userId = userId,
            cardId = cardId
        )
    }
}

