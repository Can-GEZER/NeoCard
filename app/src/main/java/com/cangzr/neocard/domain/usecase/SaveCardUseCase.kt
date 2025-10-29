package com.cangzr.neocard.domain.usecase

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import javax.inject.Inject

/**
 * SaveCardUseCase handles the business logic for saving a new business card.
 * 
 * This use case orchestrates the card saving operation including:
 * - Validating card data
 * - Uploading profile image if provided
 * - Saving card to Firestore
 * - Handling public card synchronization if card is public
 * 
 * **Business Rules:**
 * - Card must have valid user ID
 * - Card validation is enforced by repository
 * - Profile image upload is optional
 * 
 * @param cardRepository Repository for card data operations
 * 
 * @see [UserCard] Card data model
 * @see com.cangzr.neocard.data.repository.CardRepository Card repository interface
 * @see com.cangzr.neocard.ui.screens.createcard.viewmodels.CreateCardViewModel ViewModel using this use case
 * 
 * @since 1.0
 */
class SaveCardUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Saves a new business card for the user.
     * 
     * This method saves the card to Firestore and handles profile image upload
     * if provided. Returns the card ID upon successful save.
     * 
     * **Validation:**
     * Card validation is performed by the repository layer. Invalid cards will
     * result in [Resource.Error] being returned.
     * 
     * **Profile Image:**
     * If [imageUri] is provided, the image is uploaded to Firebase Storage
     * and the resulting URL is stored in the card's [UserCard.profileImageUrl].
     * 
     * @param userId The ID of the user who owns the card (must be authenticated)
     * @param card The [UserCard] to save (must be validated)
     * @param imageUri Optional URI to profile image to upload
     * @return [Resource.Success] containing the saved card ID, or [Resource.Error] if save fails
     * 
     * @throws No exceptions thrown - all errors are wrapped in [Resource.Error]
     * 
     * @see [UserCard] Card data model
     * @see [Resource] Result wrapper
     * @see com.cangzr.neocard.ui.screens.createcard.viewmodels.CreateCardViewModel ViewModel implementation
     * 
     * @since 1.0
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

