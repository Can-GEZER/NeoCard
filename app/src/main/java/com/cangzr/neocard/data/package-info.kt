/**
 * Data Package
 * 
 * This package contains data models, repositories, and data sources for the NeoCard application.
 * 
 * The data package implements the repository pattern and handles all data operations including:
 * - Firestore database interactions
 * - Local data caching
 * - Data transformation between domain and data layers
 * 
 * **Structure:**
 * - [model][com.cangzr.neocard.data.model] - Data classes representing entities
 * - [repository][com.cangzr.neocard.data.repository] - Repository interfaces and implementations
 * - [paging][com.cangzr.neocard.data.paging] - Paging data sources for lists
 * 
 * **Key Components:**
 * - [User][com.cangzr.neocard.data.model.User] - User data model
 * - [UserCard][com.cangzr.neocard.data.model.UserCard] - Business card data model
 * - [CardRepository][com.cangzr.neocard.data.repository.CardRepository] - Card data operations
 * - [AuthRepository][com.cangzr.neocard.data.repository.AuthRepository] - Authentication operations
 * 
 * @see com.cangzr.neocard.domain Domain layer interfaces
 * @see com.cangzr.neocard.ui UI layer for displaying data
 */
package com.cangzr.neocard.data

