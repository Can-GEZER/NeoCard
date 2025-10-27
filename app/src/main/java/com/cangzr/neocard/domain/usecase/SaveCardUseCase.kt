package com.cangzr.neocard.domain.usecase

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

/**
 * Yeni kart kaydeden use case
 */
class SaveCardUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Yeni bir kart kaydeder
     * 
     * @param userId Kullanıcı ID'si
     * @param card Kaydedilecek kart
     * @param imageUri Profil resmi URI'si (opsiyonel)
     * @return Resource içinde kaydedilen kartın ID'si
     */
    suspend operator fun invoke(
        userId: String,
        card: UserCard,
        imageUri: Uri? = null
    ): Resource<String> {
        return cardRepository.saveCard(
            userId = userId,
            card = card,
            imageUri = imageUri
        )
    }
}

