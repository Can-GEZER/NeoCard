package com.cangzr.neocard.domain.usecase

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

/**
 * Keşif kartlarını getiren use case
 */
class GetExploreCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Keşif kartlarını sayfalı olarak getirir
     * (Kullanıcının bağlantılarında olmayanlar)
     * 
     * @param currentUserId Mevcut kullanıcı ID'si
     * @param pageSize Sayfa başına kart sayısı
     * @param lastCardId Son kartın ID'si (pagination için)
     * @return Resource içinde Triple<Kartlar, Son kart ID'si, Daha fazla var mı>
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

