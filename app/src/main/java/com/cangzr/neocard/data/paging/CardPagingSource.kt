package com.cangzr.neocard.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository

class CardPagingSource(
    private val userId: String,
    private val cardRepository: CardRepository
) : PagingSource<String, UserCard>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, UserCard> {
        return try {
            val pageSize = params.loadSize
            val lastCardId = params.key
            
            when (val result = cardRepository.getCards(
                userId = userId,
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
                    LoadResult.Error(Exception("Unexpected loading state"))
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, UserCard>): String? {
        return null
    }
}

