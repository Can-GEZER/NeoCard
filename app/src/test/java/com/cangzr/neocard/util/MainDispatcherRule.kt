package com.cangzr.neocard.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Test Rule for coroutines testing
 * 
 * Replaces the main dispatcher with a test dispatcher for proper coroutine testing.
 * This ensures that coroutines launched with Dispatchers.Main run on the test thread.
 * 
 * Usage:
 * ```
 * @OptIn(ExperimentalCoroutinesApi::class)
 * class MyViewModelTest {
 *     @get:Rule
 *     val mainDispatcherRule = MainDispatcherRule()
 *     
 *     @Test
 *     fun myTest() = runTest {
 *         // Test code using viewModelScope or Dispatchers.Main
 *     }
 * }
 * ```
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

