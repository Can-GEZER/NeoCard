# ViewModel Testing Guide - NeoCard

Bu kılavuz, NeoCard projesinde ViewModel'lerin nasıl test edileceğini gösterir.

## 📋 İçindekiler

1. [Test Setup](#test-setup)
2. [MainDispatcherRule Kullanımı](#maindispatcherrule-kullanımı)
3. [State Testing](#state-testing)
4. [Flow Testing with Turbine](#flow-testing-with-turbine)
5. [Coroutine Testing](#coroutine-testing)
6. [Mocking Strategies](#mocking-strategies)
7. [Best Practices](#best-practices)

---

## 🛠️ Test Setup

### Gerekli Dependencies

```kotlin
// build.gradle.kts (app module)
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
```

### Basic Test Class Structure

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock dependencies
    private lateinit var repository: MyRepository
    private lateinit var useCase: MyUseCase
    private lateinit var viewModel: MyViewModel

    @Before
    fun setup() {
        // Mock dependencies
        repository = mockk(relaxed = true)
        useCase = mockk(relaxed = true)
        
        // Initialize ViewModel
        viewModel = MyViewModel(repository, useCase)
    }

    @Test
    fun `test name describes what is being tested`() = runTest {
        // Given: Test setup
        
        // When: Action
        
        // Then: Assertion
    }
}
```

---

## 🔄 MainDispatcherRule Kullanımı

### Nedir?

`MainDispatcherRule`, ViewModelScope'da çalışan coroutine'lerin test thread'inde çalışmasını sağlar.

### Implementasyon

```kotlin
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
```

### Kullanım

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @Test
    fun myTest() = runTest {
        // viewModelScope kullanımları burada test edilir
    }
}
```

---

## 🎯 State Testing

### StateFlow Testing

#### Simple State Testing

```kotlin
@Test
fun `updateName updates state correctly`() = runTest {
    // Given: ViewModel is initialized
    viewModel = MyViewModel(repository)
    
    // When: Name is updated
    val testName = "John"
    viewModel.updateName(testName)
    
    // Then: State should reflect the change
    viewModel.name.test {
        assertEquals(testName, awaitItem())
    }
}
```

#### Multiple State Updates

```kotlin
@Test
fun `multiple updates work correctly`() = runTest {
    // Given: ViewModel is initialized
    viewModel = MyViewModel(repository)
    
    // When: Multiple updates
    viewModel.updateName("John")
    viewModel.updateEmail("john@example.com")
    
    // Then: All states should be updated
    viewModel.name.test { assertEquals("John", awaitItem()) }
    viewModel.email.test { assertEquals("john@example.com", awaitItem()) }
}
```

#### State Transitions

```kotlin
@Test
fun `state transitions are emitted correctly`() = runTest {
    // Given: ViewModel is initialized
    viewModel = MyViewModel(repository)
    
    // When/Then: Track state transitions
    viewModel.uiState.test {
        // Initial state
        val initial = awaitItem()
        assertEquals(UiState.Initial, initial)
        
        // Update state
        viewModel.performAction()
        val updated = awaitItem()
        assertEquals(UiState.Updated, updated)
    }
}
```

### Data Class State Testing

```kotlin
@Test
fun `data class state updates correctly`() = runTest {
    // Given: ViewModel with data class state
    viewModel = MyViewModel(repository)
    
    // When: State field is updated
    viewModel.updateFilter("Business")
    
    // Then: State should reflect the change
    viewModel.uiState.test {
        val state = awaitItem()
        assertEquals("Business", state.selectedFilter)
        assertEquals("", state.searchQuery) // Other fields unchanged
    }
}
```

---

## 🌊 Flow Testing with Turbine

### Simple Flow Test

```kotlin
@Test
fun `flow emits values correctly`() = runTest {
    // Given: ViewModel with flow
    viewModel = MyViewModel(repository)
    
    // When/Then: Collect flow values
    viewModel.dataFlow.test {
        val firstItem = awaitItem()
        assertNotNull(firstItem)
        
        // Trigger update
        viewModel.refreshData()
        
        val secondItem = awaitItem()
        assertNotEquals(firstItem, secondItem)
    }
}
```

### Testing Resource States

```kotlin
@Test
fun `resource flow emits loading then success`() = runTest {
    // Given: Repository returns success
    coEvery { repository.getData() } returns Resource.Success(testData)
    viewModel = MyViewModel(repository)
    
    // When: Data is loaded
    viewModel.loadData()
    
    // Then: Should emit Loading then Success
    viewModel.dataState.test {
        // Skip initial state if needed
        skipItems(1)
        
        val loading = awaitItem()
        assertTrue(loading is Resource.Loading)
        
        val success = awaitItem()
        assertTrue(success is Resource.Success)
        assertEquals(testData, (success as Resource.Success).data)
    }
}
```

### Testing Flow with Errors

```kotlin
@Test
fun `flow handles errors correctly`() = runTest {
    // Given: Repository returns error
    val exception = Exception("Network error")
    coEvery { repository.getData() } returns Resource.Error(exception, "Error")
    viewModel = MyViewModel(repository)
    
    // When: Data is loaded
    viewModel.loadData()
    
    // Then: Should emit Error state
    viewModel.dataState.test {
        skipItems(1) // Skip Loading
        
        val error = awaitItem()
        assertTrue(error is Resource.Error)
        assertEquals("Error", (error as Resource.Error).message)
    }
}
```

---

## ⏱️ Coroutine Testing

### Testing Suspending Functions

```kotlin
@Test
fun `suspending function completes correctly`() = runTest {
    // Given: Repository with suspending function
    coEvery { repository.saveData(any()) } returns Resource.Success("id")
    viewModel = MyViewModel(repository)
    
    // When: Suspending function is called
    viewModel.saveData(testData)
    
    // Wait for coroutine to complete
    advanceUntilIdle()
    
    // Then: Verify result
    coVerify(exactly = 1) { repository.saveData(testData) }
}
```

### Testing Loading States

```kotlin
@Test
fun `loading state is set during async operation`() = runTest {
    // Given: Repository with delay
    coEvery { repository.getData() } coAnswers {
        delay(100)
        Resource.Success(testData)
    }
    viewModel = MyViewModel(repository)
    
    // When: Async operation starts
    viewModel.loadData()
    
    // Then: Loading should be true initially
    viewModel.isLoading.test {
        assertTrue(awaitItem()) // Loading = true
        
        advanceUntilIdle()
        
        assertFalse(awaitItem()) // Loading = false after completion
    }
}
```

### Testing ViewModelScope Launch

```kotlin
@Test
fun `viewModelScope launch executes correctly`() = runTest {
    // Given: ViewModel with viewModelScope.launch
    coEvery { useCase() } returns Resource.Success(Unit)
    viewModel = MyViewModel(useCase)
    
    // When: Action triggers viewModelScope.launch
    viewModel.performAction()
    
    // Wait for all coroutines to complete
    advanceUntilIdle()
    
    // Then: UseCase should be called
    coVerify(exactly = 1) { useCase() }
}
```

---

## 🎭 Mocking Strategies

### Strict Mocking (Default)

```kotlin
@Test
fun `strict mocking requires explicit setup`() = runTest {
    // Given: Strict mock
    val repository = mockk<MyRepository>()
    
    // Must explicitly define behavior
    coEvery { repository.getData() } returns Resource.Success(testData)
    
    viewModel = MyViewModel(repository)
    viewModel.loadData()
    
    // Verify exact calls
    coVerify(exactly = 1) { repository.getData() }
}
```

### Relaxed Mocking

```kotlin
@Test
fun `relaxed mocking allows undefined calls`() = runTest {
    // Given: Relaxed mock (useful for complex objects)
    val context = mockk<Context>(relaxed = true)
    
    // Can call any method without explicit setup
    val string = context.getString(R.string.app_name)
    
    // Verify if needed
    verify { context.getString(R.string.app_name) }
}
```

### Mocking FirebaseUser

```kotlin
@Before
fun setup() {
    mockFirebaseUser = mockk(relaxed = true)
    every { mockFirebaseUser.uid } returns "test-user-123"
    every { mockFirebaseUser.email } returns "test@example.com"
    
    every { authRepository.getCurrentUser() } returns mockFirebaseUser
}
```

### Mocking Context with String Resources

```kotlin
@Before
fun setup() {
    mockContext = mockk(relaxed = true)
    every { mockContext.getString(R.string.error_occurred, any()) } returns "Hata oluştu"
    every { mockContext.getString(R.string.success) } returns "Başarılı"
}
```

### Mocking UseCases

```kotlin
@Test
fun `usecase success flow`() = runTest {
    // Given: UseCase returns success
    coEvery { 
        saveCardUseCase(
            userId = testUserId,
            card = any(),
            imageUri = null
        ) 
    } returns Resource.Success(testCardId)
    
    // When: ViewModel calls useCase
    viewModel.saveCard()
    advanceUntilIdle()
    
    // Then: Verify useCase was called
    coVerify(exactly = 1) { saveCardUseCase(testUserId, any(), null) }
}
```

---

## ✅ Best Practices

### 1. Test Naming Convention

```kotlin
// ✅ Good: Descriptive, uses backticks
@Test
fun `saveCard returns error when user is not logged in`() = runTest {
    // ...
}

// ❌ Bad: Not descriptive
@Test
fun testSaveCard() = runTest {
    // ...
}
```

### 2. Given-When-Then Structure

```kotlin
@Test
fun `example test with clear structure`() = runTest {
    // Given: Setup test data and mocks
    every { authRepository.getCurrentUser() } returns mockUser
    viewModel = MyViewModel(authRepository)
    
    // When: Perform action
    viewModel.performAction()
    advanceUntilIdle()
    
    // Then: Assert results
    assertTrue(viewModel.isActionComplete.value)
    verify(exactly = 1) { authRepository.getCurrentUser() }
}
```

### 3. Test Isolation

```kotlin
// ✅ Good: Each test is independent
@Before
fun setup() {
    // Create fresh mocks for each test
    repository = mockk(relaxed = true)
    viewModel = MyViewModel(repository)
}

@Test
fun `test 1`() = runTest {
    // Test in isolation
}

@Test
fun `test 2`() = runTest {
    // Independent from test 1
}
```

### 4. Verify Mock Interactions

```kotlin
@Test
fun `verify exact number of calls`() = runTest {
    // Given
    viewModel = MyViewModel(repository)
    
    // When
    viewModel.loadData()
    advanceUntilIdle()
    
    // Then: Verify exact interactions
    coVerify(exactly = 1) { repository.getData() }
    coVerify(exactly = 0) { repository.deleteData() }
}
```

### 5. Test Edge Cases

```kotlin
@Test
fun `handles empty string correctly`() = runTest {
    viewModel.updateName("")
    viewModel.name.test {
        assertEquals("", awaitItem())
    }
}

@Test
fun `handles special characters correctly`() = runTest {
    viewModel.updateName("José@#$%")
    viewModel.name.test {
        assertEquals("José@#$%", awaitItem())
    }
}

@Test
fun `handles very long strings correctly`() = runTest {
    val longString = "A".repeat(1000)
    viewModel.updateName(longString)
    viewModel.name.test {
        assertEquals(longString, awaitItem())
    }
}
```

### 6. Test All State Paths

```kotlin
@Test
fun `test success path`() = runTest {
    coEvery { useCase() } returns Resource.Success(data)
    // Test success scenario
}

@Test
fun `test error path`() = runTest {
    coEvery { useCase() } returns Resource.Error(exception, "Error")
    // Test error scenario
}

@Test
fun `test loading state`() = runTest {
    // Test loading state
}
```

### 7. Don't Test Implementation Details

```kotlin
// ✅ Good: Test behavior
@Test
fun `updating name changes state`() = runTest {
    viewModel.updateName("John")
    viewModel.name.test {
        assertEquals("John", awaitItem())
    }
}

// ❌ Bad: Testing internal implementation
@Test
fun `_name MutableStateFlow is updated`() = runTest {
    // Don't test private fields or implementation
}
```

### 8. Use advanceUntilIdle() for Async Operations

```kotlin
@Test
fun `async operation completes`() = runTest {
    // When: Trigger async operation
    viewModel.performAsyncAction()
    
    // Wait for all coroutines to complete
    advanceUntilIdle()
    
    // Then: Assert results
    assertTrue(viewModel.isComplete.value)
}
```

---

## 📊 Test Coverage Checklist

### ViewModel Testing Checklist

- ✅ Initial state values
- ✅ State updates for all fields
- ✅ Success paths for all operations
- ✅ Error paths for all operations
- ✅ Loading states
- ✅ Authentication checks
- ✅ Form validation
- ✅ Edge cases (empty, special chars, long strings)
- ✅ Multiple rapid updates
- ✅ State isolation (changes don't affect unrelated state)
- ✅ Flow emissions
- ✅ Coroutine completion
- ✅ Mock verification

---

## 🎯 Real Examples

### HomeViewModel Test Example

```kotlin
@Test
fun `updateSelectedCardType updates state correctly`() = runTest {
    // Given: ViewModel is initialized
    every { authRepository.getCurrentUser() } returns null
    viewModel = HomeViewModel(authRepository, cardRepository)
    
    // When: Card type is updated
    val newCardType = "Business"
    viewModel.updateSelectedCardType(newCardType)
    
    // Then: State should reflect the new card type
    viewModel.uiState.test {
        val state = awaitItem()
        assertEquals(newCardType, state.selectedCardType)
        assertEquals("", state.searchQuery) // Other fields unchanged
    }
}
```

### CreateCardViewModel Test Example

```kotlin
@Test
fun `saveCard succeeds when repository returns success`() = runTest {
    // Given: User is logged in
    every { authRepository.getCurrentUser() } returns mockFirebaseUser
    viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
    
    // Set up form data
    viewModel.updateName("John")
    viewModel.updateSurname("Doe")
    
    // Mock getUserCardsUseCase to return empty (no limit)
    coEvery {
        getUserCardsUseCase(testUserId, 1, null)
    } returns Resource.Success(Triple(emptyList(), null, false))
    
    // Mock saveCardUseCase to return success
    coEvery {
        saveCardUseCase(userId = testUserId, card = any(), imageUri = null)
    } returns Resource.Success(testCardId)
    
    var successCalled = false
    
    // When: Saving card
    viewModel.saveCard(mockContext) {
        successCalled = true
    }
    
    // Wait for coroutine to complete
    advanceUntilIdle()
    
    // Then: Should call success callback
    assertTrue("Success callback should be called", successCalled)
    
    // Verify use cases were called
    coVerify(exactly = 1) { getUserCardsUseCase(testUserId, 1, null) }
    coVerify(exactly = 1) { saveCardUseCase(testUserId, any(), null) }
}
```

---

## 🚀 Running Tests

### Tüm ViewModel Testleri

```bash
./gradlew test --tests "*ViewModel*"
```

### Belirli Bir ViewModel

```bash
./gradlew test --tests "HomeViewModelTest"
```

### Belirli Bir Test

```bash
./gradlew test --tests "HomeViewModelTest.updateSelectedCardType updates state correctly"
```

---

## 📚 Kaynaklar

- [Kotlin Coroutines Test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
- [Turbine - Flow Testing](https://github.com/cashapp/turbine)
- [MockK Documentation](https://mockk.io/)
- [Android ViewModel Testing](https://developer.android.com/codelabs/advanced-android-kotlin-training-testing-survey)

---

*ViewModel Testing Guide - NeoCard Project*

