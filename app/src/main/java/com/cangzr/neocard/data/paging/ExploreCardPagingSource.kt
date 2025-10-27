package com.cangzr.neocard.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository

/**
 * PagingSource for loading explore cards with pagination
 * 
 * @param currentUserId Current user ID (to exclude their cards)
 * @param cardRepository Repository to fetch cards from
 */
class ExploreCardPagingSource(
    private val currentUserId: String,
    private val cardRepository: CardRepository
) : PagingSource<String, UserCard>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, UserCard> {
        return try {
            val pageSize = params.loadSize
            val lastCardId = params.key
            
            // Call repository to get explore cards
            when (val result = cardRepository.getExploreCards(
                currentUserId = currentUserId,
                pageSize = pageSize,
                lastCardId = lastCardId
            )) {
                is Resource.Success -> {
                    val (cards, nextKey, hasMore) = result.data
                    
                    LoadResult.Page(
                        data = cards,
                        prevKey = null, // We don't support backward pagination
                        nextKey = if (hasMore) nextKey else null
                    )
                }
                is Resource.Error -> {
                    LoadResult.Error(result.exception)
                }
                is Resource.Loading -> {
                    // Should not happen in this context
                    LoadResult.Error(Exception("Unexpected loading state"))
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, UserCard>): String? {
        // Return null to always start from the beginning on refresh
        return null
    }
}

