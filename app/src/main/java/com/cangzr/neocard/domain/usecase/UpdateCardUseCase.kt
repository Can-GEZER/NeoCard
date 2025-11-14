package com.cangzr.neocard.domain.usecase

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

class UpdateCardUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    suspend operator fun invoke(
        userId: String,
        cardId: String,
        card: UserCard,
        imageUri: Uri? = null
    ): Resource<Unit> {
        return cardRepository.updateCard(
            userId = userId,
            cardId = cardId,
            card = card,
            imageUri = imageUri
        )
    }
}

