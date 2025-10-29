package com.cangzr.neocard.data.repository

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard

/**
 * CardRepository interface defines operations for managing business cards in the system.
 * 
 * This repository provides a clean abstraction layer for card data operations, following
 * the repository pattern. Implementations handle Firebase Firestore interactions,
 * image uploads, and data transformation.
 * 
 * **Key Operations:**
 * - CRUD operations for user cards
 * - Pagination support for card lists
 * - Public card retrieval for exploration
 * - Profile image management
 * 
 * **Implementation:**
 * - [FirebaseCardRepository][com.cangzr.neocard.data.repository.impl.FirebaseCardRepository] - Firestore implementation
 * 
 * @see [UserCard] Card data model
 * @see [Resource] Result wrapper for all operations
 * @see com.cangzr.neocard.domain.usecase.SaveCardUseCase Use cases using this repository
 * 
 * @since 1.0
 */
interface CardRepository {
    
    /**
     * Retrieves user's cards with pagination support.
     * 
     * Returns cards belonging to the specified user in pages for efficient loading.
     * 
     * @param userId ID of the user whose cards to retrieve
     * @param pageSize Number of cards per page
     * @param lastCardId ID of last card from previous page, or null for first page
     * @return [Resource.Success] containing Triple of (cards, lastCardId, hasMore),
     *         or [Resource.Error] if retrieval fails
     * 
     * @see [UserCard] Card data model
     * @see com.cangzr.neocard.domain.usecase.GetUserCardsUseCase Use case using this method
     */
    suspend fun getCards(
        userId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>>
    
    /**
     * Retrieves a specific card by its ID.
     * 
     * @param userId ID of the user who owns the card
     * @param cardId ID of the card to retrieve
     * @return [Resource.Success] containing the [UserCard] if found, or null if not found,
     *         or [Resource.Error] if retrieval fails
     * 
     * @see [UserCard] Card data model
     */
    suspend fun getCardById(
        userId: String,
        cardId: String
    ): Resource<UserCard?>
    
    /**
     * Saves a new card to Firestore.
     * 
     * This method handles card creation including optional profile image upload.
     * If the card is public, it also creates an entry in the public_cards collection.
     * 
     * @param userId ID of the user who owns the card
     * @param card The [UserCard] to save (must be validated)
     * @param imageUri Optional URI to profile image to upload
     * @return [Resource.Success] containing the saved card ID, or [Resource.Error] if save fails
     * 
     * @throws No exceptions - errors are wrapped in [Resource.Error]
     * 
     * @see [UserCard] Card data model
     * @see com.cangzr.neocard.domain.usecase.SaveCardUseCase Use case using this method
     */
    suspend fun saveCard(
        userId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<String>
    
    /**
     * Updates an existing card in Firestore.
     * 
     * Updates card data and optionally replaces the profile image. If the card's
     * public status changes, it synchronizes with the public_cards collection.
     * 
     * @param userId ID of the user who owns the card
     * @param cardId ID of the card to update
     * @param card Updated [UserCard] data
     * @param imageUri Optional URI to new profile image to upload
     * @return [Resource.Success] if update succeeds, or [Resource.Error] if update fails
     * 
     * @throws No exceptions - errors are wrapped in [Resource.Error]
     * 
     * @see [UserCard] Card data model
     */
    suspend fun updateCard(
        userId: String,
        cardId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<Unit>
    
    /**
     * Deletes a card from Firestore and optionally removes its profile image.
     * 
     * Removes the card from user's cards collection and public_cards collection if applicable.
     * Also deletes the profile image from Firebase Storage if [profileImageUrl] is provided.
     * 
     * @param userId ID of the user who owns the card
     * @param cardId ID of the card to delete
     * @param profileImageUrl URL of profile image to delete from storage (if any)
     * @return [Resource.Success] if deletion succeeds, or [Resource.Error] if deletion fails
     * 
     * @throws No exceptions - errors are wrapped in [Resource.Error]
     */
    suspend fun deleteCard(
        userId: String,
        cardId: String,
        profileImageUrl: String
    ): Resource<Unit>
    
    /**
     * Retrieves public cards for exploration with pagination.
     * 
     * Returns publicly available cards excluding those owned by [currentUserId].
     * Used for the explore/discovery feature.
     * 
     * @param currentUserId ID of current user (their cards will be excluded)
     * @param pageSize Number of cards per page
     * @param lastCardId ID of last card from previous page, or null for first page
     * @return [Resource.Success] containing Triple of (cards, lastCardId, hasMore),
     *         or [Resource.Error] if retrieval fails
     * 
     * @see [UserCard] Card data model
     * @see com.cangzr.neocard.domain.usecase.GetExploreCardsUseCase Use case using this method
     */
    suspend fun getExploreCards(
        currentUserId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>>
    
    /**
     * Retrieves a public card by its ID.
     * 
     * Used for viewing public cards without authentication. The card must have
     * [UserCard.isPublic] set to true.
     * 
     * @param cardId ID of the public card to retrieve
     * @return [Resource.Success] containing the [UserCard] if found and public, or null,
     *         or [Resource.Error] if retrieval fails
     * 
     * @see [UserCard] Card data model
     */
    suspend fun getPublicCardById(
        cardId: String
    ): Resource<UserCard?>
}

