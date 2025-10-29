/**
 * Dependency Injection Package
 * 
 * This package contains Hilt modules and dependency injection configuration for the NeoCard application.
 * 
 * The DI package uses Hilt (built on Dagger) to provide dependency injection throughout the app.
 * Modules in this package define how dependencies are provided and scoped.
 * 
 * **Structure:**
 * - [AppModule][com.cangzr.neocard.di.AppModule] - Application-level dependencies
 * - [RepositoryModule][com.cangzr.neocard.di.RepositoryModule] - Repository implementations
 * 
 * **Key Components:**
 * - Firebase instances (Firestore, Storage, Auth)
 * - Repository implementations
 * - Use case instances
 * - ViewModel factory bindings
 * 
 * **Scopes:**
 * - [SingletonComponent][dagger.hilt.components.SingletonComponent] - Application-wide singletons
 * - [ViewModelComponent][dagger.hilt.android.components.ViewModelComponent] - ViewModel-scoped instances
 * 
 * @see <a href="https://developer.android.com/training/dependency-injection/hilt-android">Hilt Documentation</a>
 */
package com.cangzr.neocard.di

