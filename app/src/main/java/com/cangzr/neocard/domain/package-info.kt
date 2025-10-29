/**
 * Domain Package
 * 
 * This package contains use cases and business logic for the NeoCard application.
 * 
 * The domain package follows Clean Architecture principles and contains:
 * - Use cases (interactors) that encapsulate business logic
 * - Domain-specific rules and validations
 * - Business entity interfaces
 * 
 * **Structure:**
 * - [usecase][com.cangzr.neocard.domain.usecase] - Use case implementations
 * 
 * **Key Components:**
 * - [SaveCardUseCase][com.cangzr.neocard.domain.usecase.SaveCardUseCase] - Save business card use case
 * - [GetUserCardsUseCase][com.cangzr.neocard.domain.usecase.GetUserCardsUseCase] - Get user cards use case
 * - [GetExploreCardsUseCase][com.cangzr.neocard.domain.usecase.GetExploreCardsUseCase] - Explore public cards use case
 * 
 * **Architecture:**
 * Use cases in this package orchestrate data operations from repositories and enforce
 * business rules. They are pure Kotlin classes that don't depend on Android framework.
 * 
 * @see com.cangzr.neocard.data Data layer for repository implementations
 * @see com.cangzr.neocard.ui UI layer that uses these use cases
 */
package com.cangzr.neocard.domain

