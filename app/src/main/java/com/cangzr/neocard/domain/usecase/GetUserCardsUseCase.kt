package com.cangzr.neocard.domain.usecase

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

/**
 * Kullanıcının kartlarını getiren use case
 */
class GetUserCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Kullanıcının kartlarını sayfalı olarak getirir
     * 
     * @param userId Kullanıcı ID'si
     * @param pageSize Sayfa başına kart sayısı
     * @param lastCardId Son kartın ID'si (pagination için)
     * @return Resource içinde Triple<Kartlar, Son kart ID'si, Daha fazla var mı>
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

