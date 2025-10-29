# Compose UI Testing Guide - NeoCard

Bu kılavuz, NeoCard projesinde Jetpack Compose UI testlerinin nasıl yazılacağını gösterir.

## 📋 İçindekiler

1. [Test Setup](#test-setup)
2. [ComposeTestRule Kullanımı](#composetestrule-kullanımı)
3. [Test Tags](#test-tags)
4. [Finding UI Elements](#finding-ui-elements)
5. [Assertions](#assertions)
6. [User Interactions](#user-interactions)
7. [Testing States](#testing-states)
8. [Best Practices](#best-practices)

---

## 🛠️ Test Setup

### Gerekli Dependencies

```kotlin
// build.gradle.kts (app module)
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("io.mockk:mockk-android:1.13.8")

debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.0")
```

### Test Class Structure

```kotlin
class MyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        // Setup mocks and dependencies
    }

    @Test
    fun myTest() {
        // Given: Setup
        composeTestRule.setContent {
            MyScreen()
        }
        
        // When: Action
        
        // Then: Assertion
    }
}
```

---

## 📐 ComposeTestRule Kullanımı

### Creating ComposeTestRule

```kotlin
class MyScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
}
```

### Setting Content

```kotlin
@Test
fun testMyScreen() {
    composeTestRule.setContent {
        NeoCardTheme {
            MyScreen()
        }
    }
}
```

### With ViewModel

```kotlin
@Test
fun testWithViewModel() {
    val mockViewModel = mockk<MyViewModel>(relaxed = true)
    
    composeTestRule.setContent {
        NeoCardTheme {
            MyScreen(viewModel = mockViewModel)
        }
    }
}
```

### With Navigation

```kotlin
@Test
fun testWithNavigation() {
    composeTestRule.setContent {
        NeoCardTheme {
            val navController = rememberNavController()
            MyScreen(navController = navController)
        }
    }
}
```

---

## 🏷️ Test Tags

### Defining Test Tags

```kotlin
object TestTags {
    const val HOME_SCREEN = "home_screen"
    const val SAVE_BUTTON = "save_button"
    const val NAME_FIELD = "name_field"
}
```

### Applying Test Tags

```kotlin
@Composable
fun MyButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.testTag(TestTags.SAVE_BUTTON)
    ) {
        Text("Save")
    }
}
```

### Finding by Test Tag

```kotlin
@Test
fun testButton() {
    composeTestRule
        .onNodeWithTag(TestTags.SAVE_BUTTON)
        .assertExists()
        .assertIsDisplayed()
}
```

---

## 🔍 Finding UI Elements

### By Text

```kotlin
// Exact match
composeTestRule
    .onNodeWithText("Save")
    .assertExists()

// Substring match
composeTestRule
    .onNodeWithText("Save", substring = true)
    .assertExists()

// Ignore case
composeTestRule
    .onNodeWithText("save", ignoreCase = true)
    .assertExists()
```

### By Content Description

```kotlin
composeTestRule
    .onNodeWithContentDescription("Profile Image")
    .assertExists()
```

### By Test Tag

```kotlin
composeTestRule
    .onNodeWithTag(TestTags.SAVE_BUTTON)
    .assertExists()
```

### Multiple Nodes

```kotlin
// Get all nodes with text
composeTestRule
    .onAllNodesWithText("Card")
    .assertCountEquals(3)

// Get first node
composeTestRule
    .onAllNodesWithText("Card")
    .onFirst()
    .assertExists()

// Get node at index
composeTestRule
    .onAllNodesWithText("Card")[1]
    .assertExists()
```

### Root Node

```kotlin
composeTestRule
    .onRoot()
    .assertExists()
```

---

## ✅ Assertions

### Existence

```kotlin
// Assert exists
composeTestRule
    .onNodeWithText("Save")
    .assertExists()

// Assert does not exist
composeTestRule
    .onNodeWithText("Login")
    .assertDoesNotExist()
```

### Visibility

```kotlin
// Assert is displayed
composeTestRule
    .onNodeWithTag(TestTags.SAVE_BUTTON)
    .assertIsDisplayed()

// Assert is not displayed
composeTestRule
    .onNodeWithTag(TestTags.HIDDEN_CONTENT)
    .assertIsNotDisplayed()
```

### State

```kotlin
// Assert is enabled
composeTestRule
    .onNodeWithTag(TestTags.SAVE_BUTTON)
    .assertIsEnabled()

// Assert is not enabled (disabled)
composeTestRule
    .onNodeWithTag(TestTags.DISABLED_BUTTON)
    .assertIsNotEnabled()

// Assert is selected
composeTestRule
    .onNodeWithTag(TestTags.CHECKBOX)
    .assertIsSelected()
```

### Semantics

```kotlin
// Assert has click action
composeTestRule
    .onNodeWithTag(TestTags.SAVE_BUTTON)
    .assertHasClickAction()

// Assert has text
composeTestRule
    .onNodeWithTag(TestTags.TEXT_FIELD)
    .assertTextEquals("John Doe")

// Assert contains text
composeTestRule
    .onNodeWithTag(TestTags.TEXT_FIELD)
    .assertTextContains("John")
```

---

## 👆 User Interactions

### Click

```kotlin
@Test
fun testButtonClick() {
    var clicked = false
    
    composeTestRule.setContent {
        Button(onClick = { clicked = true }) {
            Text("Click Me")
        }
    }
    
    // Perform click
    composeTestRule
        .onNodeWithText("Click Me")
        .performClick()
    
    // Verify action was performed
    assertTrue(clicked)
}
```

### Text Input

```kotlin
@Test
fun testTextInput() {
    composeTestRule.setContent {
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.testTag(TestTags.NAME_FIELD)
        )
    }
    
    // Type text
    composeTestRule
        .onNodeWithTag(TestTags.NAME_FIELD)
        .performTextInput("John Doe")
    
    // Verify text was entered
    composeTestRule
        .onNodeWithTag(TestTags.NAME_FIELD)
        .assertTextEquals("John Doe")
}
```

### Clear Text

```kotlin
composeTestRule
    .onNodeWithTag(TestTags.NAME_FIELD)
    .performTextClearance()
```

### Replace Text

```kotlin
composeTestRule
    .onNodeWithTag(TestTags.NAME_FIELD)
    .performTextReplacement("New Text")
```

### Scroll

```kotlin
// Scroll to node
composeTestRule
    .onNodeWithText("Bottom Item")
    .performScrollTo()

// Scroll vertically
composeTestRule
    .onNodeWithTag(TestTags.SCROLLABLE_LIST)
    .performTouchInput {
        swipeUp()
    }
```

---

## 🔄 Testing States

### Testing Loading State

```kotlin
@Test
fun testLoadingState() {
    val mockViewModel = mockk<MyViewModel>()
    every { mockViewModel.isLoading } returns MutableStateFlow(true)
    
    composeTestRule.setContent {
        MyScreen(viewModel = mockViewModel)
    }
    
    // Assert loading indicator is shown
    composeTestRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        .assertExists()
}
```

### Testing Success State

```kotlin
@Test
fun testSuccessState() {
    val mockViewModel = mockk<MyViewModel>()
    every { mockViewModel.uiState } returns MutableStateFlow(
        Resource.Success(testData)
    )
    
    composeTestRule.setContent {
        MyScreen(viewModel = mockViewModel)
    }
    
    // Assert success content is shown
    composeTestRule
        .onNodeWithText("Success!")
        .assertExists()
}
```

### Testing Error State

```kotlin
@Test
fun testErrorState() {
    val errorMessage = "Network error occurred"
    val mockViewModel = mockk<MyViewModel>()
    every { mockViewModel.uiState } returns MutableStateFlow(
        Resource.Error(Exception(errorMessage), errorMessage)
    )
    
    composeTestRule.setContent {
        MyScreen(viewModel = mockViewModel)
    }
    
    // Assert error message is shown
    composeTestRule
        .onNodeWithText(errorMessage, substring = true)
        .assertExists()
}
```

### Testing State Transitions

```kotlin
@Test
fun testStateTransitions() {
    val stateFlow = MutableStateFlow<Resource<String>>(Resource.Loading)
    val mockViewModel = mockk<MyViewModel>()
    every { mockViewModel.uiState } returns stateFlow
    
    composeTestRule.setContent {
        MyScreen(viewModel = mockViewModel)
    }
    
    // Initially loading
    composeTestRule
        .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
        .assertExists()
    
    // Update to success
    stateFlow.value = Resource.Success("Data loaded")
    composeTestRule.waitForIdle()
    
    // Assert success state
    composeTestRule
        .onNodeWithText("Data loaded")
        .assertExists()
}
```

---

## ✨ Best Practices

### 1. Use waitForIdle()

```kotlin
@Test
fun testWithWait() {
    composeTestRule.setContent {
        MyScreen()
    }
    
    // Wait for composition to settle
    composeTestRule.waitForIdle()
    
    // Then perform assertions
    composeTestRule
        .onNodeWithText("Content")
        .assertExists()
}
```

### 2. Test User-Facing Behavior

```kotlin
// ✅ Good: Test what user sees
@Test
fun `save button is visible and clickable`() {
    composeTestRule.setContent { CreateCardScreen() }
    
    composeTestRule
        .onNodeWithText("Save")
        .assertIsDisplayed()
        .assertHasClickAction()
}

// ❌ Bad: Test implementation details
@Test
fun `viewModel state is initialized`() {
    // Don't test ViewModel internal state in UI tests
}
```

### 3. Use Descriptive Test Names

```kotlin
// ✅ Good
@Test
fun `login button shows loading indicator when authenticating`()

// ❌ Bad
@Test
fun testLogin()
```

### 4. Test One Thing Per Test

```kotlin
// ✅ Good: Single responsibility
@Test
fun `save button is visible`() {
    composeTestRule.setContent { MyScreen() }
    composeTestRule.onNodeWithText("Save").assertExists()
}

@Test
fun `save button is clickable`() {
    composeTestRule.setContent { MyScreen() }
    composeTestRule.onNodeWithText("Save").assertHasClickAction()
}

// ❌ Bad: Multiple concerns
@Test
fun testSaveButton() {
    // Tests visibility, clickability, color, text...
}
```

### 5. Use Test Tags for Complex Hierarchies

```kotlin
// ✅ Good: Use test tags
@Composable
fun ComplexList(items: List<Item>) {
    LazyColumn(modifier = Modifier.testTag(TestTags.ITEM_LIST)) {
        items(items) { item ->
            ItemCard(
                item = item,
                modifier = Modifier.testTag("${TestTags.ITEM_CARD}_${item.id}")
            )
        }
    }
}

@Test
fun testSpecificItem() {
    composeTestRule.onNodeWithTag("${TestTags.ITEM_CARD}_123").assertExists()
}
```

### 6. Mock Dependencies Properly

```kotlin
@Test
fun testWithMocks() {
    // Mock all dependencies
    val mockAuthRepo = mockk<AuthRepository>(relaxed = true)
    val mockUseCase = mockk<SaveCardUseCase>(relaxed = true)
    
    // Setup specific behavior
    every { mockAuthRepo.getCurrentUser() } returns mockUser
    coEvery { mockUseCase(any(), any(), any()) } returns Resource.Success("id")
    
    // Use mocks in ViewModel
    val viewModel = MyViewModel(mockAuthRepo, mockUseCase)
    
    composeTestRule.setContent {
        MyScreen(viewModel = viewModel)
    }
}
```

### 7. Test Accessibility

```kotlin
@Test
fun `all interactive elements have content descriptions`() {
    composeTestRule.setContent { MyScreen() }
    
    // Test buttons have descriptions
    composeTestRule
        .onNodeWithContentDescription("Save card")
        .assertExists()
    
    // Test images have descriptions
    composeTestRule
        .onNodeWithContentDescription("Profile picture")
        .assertExists()
}
```

### 8. Verify Error Recovery

```kotlin
@Test
fun `retry button appears after error and can be clicked`() {
    val mockViewModel = mockk<MyViewModel>()
    val stateFlow = MutableStateFlow<Resource<Data>>(
        Resource.Error(Exception("Network error"))
    )
    every { mockViewModel.uiState } returns stateFlow
    every { mockViewModel.retry() } answers {
        stateFlow.value = Resource.Success(testData)
    }
    
    composeTestRule.setContent {
        MyScreen(viewModel = mockViewModel)
    }
    
    // Assert error and retry button shown
    composeTestRule.onNodeWithText("Retry").assertExists()
    
    // Click retry
    composeTestRule.onNodeWithText("Retry").performClick()
    composeTestRule.waitForIdle()
    
    // Assert success state
    composeTestRule.onNodeWithText("Success").assertExists()
}
```

---

## 📊 Test Coverage Checklist

### Screen Testing Checklist

- ✅ Screen renders without crashing
- ✅ All major UI elements are visible
- ✅ Loading state displays correctly
- ✅ Success state displays content
- ✅ Error state shows error message
- ✅ Empty state handled properly
- ✅ Buttons are clickable
- ✅ Text fields accept input
- ✅ Navigation works correctly
- ✅ Dialogs show and dismiss
- ✅ Scrolling works
- ✅ Forms validate input

---

## 🎯 Real Examples from NeoCard

### HomeScreen Test Example

```kotlin
@Test
fun homeScreen_showsLoginPrompt_whenNotAuthenticated() {
    // Given: User is not authenticated
    every { mockAuthRepository.getCurrentUser() } returns null
    
    // When: HomeScreen is displayed
    composeTestRule.setContent {
        NeoCardTheme {
            val navController = rememberNavController()
            val viewModel = HomeViewModel(mockAuthRepository, mockCardRepository)
            HomeScreen(navController = navController)
        }
    }
    
    // Then: Login prompt should be visible
    composeTestRule
        .onNodeWithText("Lütfen giriş yapın", substring = true, ignoreCase = true)
        .assertExists()
        .assertIsDisplayed()
}
```

### CreateCardScreen Test Example

```kotlin
@Test
fun createCardScreen_saveButtonIsClickable() {
    // When: CreateCardScreen is displayed
    composeTestRule.setContent {
        NeoCardTheme {
            val navController = rememberNavController()
            val viewModel = CreateCardViewModel(
                mockAuthRepository,
                mockSaveCardUseCase,
                mockGetUserCardsUseCase
            )
            CreateCardScreen(navController = navController)
        }
    }
    
    // Then: Save button should be clickable
    composeTestRule.waitForIdle()
    
    composeTestRule
        .onAllNodesWithText("Kaydet", substring = true, ignoreCase = true)
        .onFirst()
        .assertHasClickAction()
}
```

---

## 🚀 Running UI Tests

### Run All UI Tests

```bash
./gradlew connectedAndroidTest
```

### Run Specific Test Class

```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cangzr.neocard.ui.screens.HomeScreenTest
```

### Run on Specific Device

```bash
./gradlew connectedDebugAndroidTest
```

---

## 🔧 Troubleshooting

### Test Times Out

```kotlin
// Add longer timeout
composeTestRule.waitForIdle()
Thread.sleep(1000) // Last resort
```

### Node Not Found

```kotlin
// Use substring match
composeTestRule.onNodeWithText("text", substring = true)

// Use printToLog to debug
composeTestRule.onRoot().printToLog("UI_TREE")
```

### Flaky Tests

```kotlin
// Add explicit waits
composeTestRule.waitForIdle()

// Use waitUntil
composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule
        .onAllNodesWithText("Text")
        .fetchSemanticsNodes()
        .isNotEmpty()
}
```

---

## 📚 Kaynaklar

- [Compose Testing Cheatsheet](https://developer.android.com/jetpack/compose/testing-cheatsheet)
- [Compose Testing Guide](https://developer.android.com/jetpack/compose/testing)
- [Testing State](https://developer.android.com/jetpack/compose/state-testing)
- [Semantics in Compose](https://developer.android.com/jetpack/compose/semantics)

---

*Compose UI Testing Guide - NeoCard Project*

