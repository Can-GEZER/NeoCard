package com.cangzr.neocard.di

import com.cangzr.neocard.data.repository.AuthRepository
import com.cangzr.neocard.data.repository.CardRepository
import com.cangzr.neocard.data.repository.ReferralRepository
import com.cangzr.neocard.data.repository.impl.FirebaseAuthRepository
import com.cangzr.neocard.data.repository.impl.FirebaseCardRepository
import com.cangzr.neocard.data.repository.impl.FirebaseReferralRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(
        firebaseCardRepository: FirebaseCardRepository
    ): CardRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        firebaseAuthRepository: FirebaseAuthRepository
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindReferralRepository(
        firebaseReferralRepository: FirebaseReferralRepository
    ): ReferralRepository
}

