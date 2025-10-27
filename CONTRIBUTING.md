# Contributing to NeoCard

First off, thank you for considering contributing to NeoCard! 🎉

It's people like you that make NeoCard such a great tool. Following these guidelines helps to communicate that you respect the time of the developers managing and developing this open source project.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)

---

## 📜 Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

### Our Standards

**Positive behavior includes:**
- Using welcoming and inclusive language
- Being respectful of differing viewpoints
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

**Unacceptable behavior includes:**
- Trolling, insulting/derogatory comments
- Public or private harassment
- Publishing others' private information
- Other conduct which could reasonably be considered inappropriate

---

## 🚀 Getting Started

### Types of Contributions

We welcome many different types of contributions:

- 🐛 **Bug Reports** - Found a bug? Let us know!
- 💡 **Feature Requests** - Have an idea? Share it!
- 📝 **Documentation** - Improve our docs
- 🔧 **Code Contributions** - Fix bugs or add features
- 🎨 **Design** - UI/UX improvements
- 🌍 **Translations** - Help translate the app

### First Time Contributors

Look for issues labeled with:
- `good first issue` - Perfect for newcomers
- `help wanted` - We need help with these
- `documentation` - Improve our docs

---

## 💻 How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates.

**When filing a bug report, include:**

1. **Clear title and description**
2. **Steps to reproduce**
   ```
   1. Go to '...'
   2. Click on '...'
   3. Scroll down to '...'
   4. See error
   ```
3. **Expected behavior**
4. **Actual behavior**
5. **Screenshots** (if applicable)
6. **Environment:**
   - Android Version:
   - Device:
   - App Version:
7. **Logcat output** (if applicable)

**Template:**
```markdown
## Bug Description
A clear and concise description of what the bug is.

## Steps to Reproduce
1. ...
2. ...
3. ...

## Expected Behavior
What you expected to happen.

## Actual Behavior
What actually happened.

## Environment
- Android Version: 
- Device: 
- App Version: 

## Screenshots
If applicable, add screenshots.

## Logcat
```
Paste relevant logcat here
```
```

### Suggesting Features

Feature requests are welcome! To suggest a feature:

1. **Check existing issues** to avoid duplicates
2. **Provide a clear title**
3. **Describe the feature** in detail
4. **Explain why** it would be useful
5. **Provide examples** or mockups if possible

**Template:**
```markdown
## Feature Description
A clear description of the feature.

## Problem It Solves
Explain what problem this feature solves.

## Proposed Solution
Describe how you'd like this to work.

## Alternatives Considered
Any alternative solutions you've thought about.

## Additional Context
Add any other context, screenshots, or mockups.
```

---

## 🛠️ Development Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Git
- Firebase account

### Setup Steps

1. **Fork the repository**
   ```bash
   # Click "Fork" button on GitHub
   ```

2. **Clone your fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/neo.git
   cd neo
   ```

3. **Add upstream remote**
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/neo.git
   ```

4. **Create a branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

5. **Set up Firebase** (see main README)

6. **Build the project**
   ```bash
   ./gradlew build
   ```

### Keeping Your Fork Updated

```bash
# Fetch upstream changes
git fetch upstream

# Merge upstream main into your local main
git checkout main
git merge upstream/main

# Push to your fork
git push origin main
```

---

## 📝 Coding Standards

### Kotlin Style Guide

Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

**Key points:**

#### Naming
```kotlin
// Classes: PascalCase
class UserCardViewModel

// Functions: camelCase
fun getUserCards()

// Constants: UPPER_SNAKE_CASE
const val MAX_CARDS = 10

// Variables: camelCase
val userName = "John"
```

#### Formatting
```kotlin
// Use 4 spaces for indentation
class Example {
    fun method() {
        if (condition) {
            // code
        }
    }
}

// Line length: 100-120 characters max
// Break long lines
val result = repository
    .getUserCards()
    .map { it.name }
    .filter { it.isNotEmpty() }
```

#### Functions
```kotlin
// Keep functions small and focused
// Single responsibility

// Good ✅
fun validateEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

// Bad ❌
fun processUser(email: String) {
    // validates email
    // saves to database
    // sends notification
    // updates UI
}
```

#### Comments
```kotlin
// Use comments to explain WHY, not WHAT

// Good ✅
// Firebase requires lowercase emails for consistency
val normalizedEmail = email.lowercase()

// Bad ❌
// Convert email to lowercase
val normalizedEmail = email.lowercase()
```

### Architecture Guidelines

#### MVVM Structure
```kotlin
// ViewModel: UI logic only
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserCardsUseCase: GetUserCardsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<Resource<HomeUiState>>(Resource.Loading)
    val uiState: StateFlow<Resource<HomeUiState>> = _uiState
    
    fun loadCards() {
        viewModelScope.launch {
            _uiState.value = Resource.Loading
            when (val result = getUserCardsUseCase()) {
                is Resource.Success -> _uiState.value = Resource.Success(result.data)
                is Resource.Error -> _uiState.value = Resource.Error(result.exception, result.message)
            }
        }
    }
}
```

#### Use Cases
```kotlin
// Use Case: Single responsibility business logic
class GetUserCardsUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    suspend operator fun invoke(userId: String): Resource<List<UserCard>> {
        return cardRepository.getCards(userId)
    }
}
```

#### Repository
```kotlin
// Repository: Data source abstraction
interface CardRepository {
    suspend fun getCards(userId: String): Resource<List<UserCard>>
    suspend fun saveCard(card: UserCard): Resource<String>
}

class FirebaseCardRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CardRepository {
    override suspend fun getCards(userId: String): Resource<List<UserCard>> {
        return safeApiCall {
            // Firebase implementation
        }
    }
}
```

### Compose Guidelines

```kotlin
// Keep Composables small and reusable

// Good ✅
@Composable
fun UserCard(
    card: UserCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        UserCardContent(card)
    }
}

@Composable
private fun UserCardContent(card: UserCard) {
    Column {
        Text(card.name)
        Text(card.title)
    }
}

// Bad ❌
@Composable
fun HomeScreen() {
    // 500 lines of UI code
    // Multiple nested composables
    // Business logic mixed in
}
```

---

## 📨 Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/).

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `ci`: CI/CD changes

### Examples

```bash
# Simple commit
feat: Add dark mode support

# With scope
feat(auth): Add Google Sign-In

# With body
fix(home): Fix card loading issue

Cards were not loading when user had more than 100 cards.
Implemented pagination to fix this issue.

# Breaking change
feat(api)!: Change card model structure

BREAKING CHANGE: Card model now requires `cardType` field.
Migrate existing cards by adding default cardType.
```

---

## 🔄 Pull Request Process

### Before Submitting

1. **Ensure your code builds**
   ```bash
   ./gradlew build
   ```

2. **Run tests**
   ```bash
   ./gradlew test
   ```

3. **Run lint**
   ```bash
   ./gradlew lint
   ```

4. **Format code**
   ```bash
   ./gradlew ktlintFormat
   ```

5. **Update documentation** if needed

### PR Guidelines

1. **Create from feature branch**
   ```bash
   git checkout -b feature/your-feature
   ```

2. **Make your changes**

3. **Commit with conventional commits**

4. **Push to your fork**
   ```bash
   git push origin feature/your-feature
   ```

5. **Create PR on GitHub**

### PR Template

```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] All tests pass

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings
- [ ] Tests added for new features
- [ ] Dependent changes merged

## Screenshots (if applicable)
Add screenshots of UI changes.

## Related Issues
Fixes #123
```

### Review Process

1. **Automated checks** must pass (CI/CD)
2. **At least one approval** required
3. **All comments** must be resolved
4. **Maintainer will merge** after approval

---

## 🧪 Testing Guidelines

### Writing Tests

```kotlin
// Unit test example
@Test
fun `saveCard returns Success when repository succeeds`() = runTest {
    // Arrange
    val card = createTestCard()
    coEvery { repository.saveCard(any()) } returns Resource.Success("id")
    
    // Act
    val result = useCase(card)
    
    // Assert
    assertTrue(result is Resource.Success)
    assertEquals("id", (result as Resource.Success).data)
}
```

### Test Coverage

- Aim for **>80% coverage** for new code
- Always test:
  - Happy path
  - Error cases
  - Edge cases
  - Null handling

### Running Tests

```bash
# All tests
./gradlew test

# Specific test
./gradlew test --tests "SaveCardUseCaseTest"

# With coverage
./gradlew testDebugUnitTest jacocoTestReport

# View coverage
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

---

## 📚 Documentation

### Code Documentation

```kotlin
/**
 * Saves a user card to Firestore.
 *
 * @param userId The ID of the user creating the card
 * @param card The card to save
 * @param imageUri Optional profile image URI
 * @return Resource containing the saved card ID or error
 * @throws IllegalArgumentException if userId is empty
 */
suspend fun saveCard(
    userId: String,
    card: UserCard,
    imageUri: Uri? = null
): Resource<String>
```

### README Updates

If your changes affect:
- Setup process
- Configuration
- Features
- Architecture

**→ Update the main README.md**

### Adding Examples

```kotlin
// Example: Using the new feature
val viewModel: HomeViewModel = hiltViewModel()
val cards by viewModel.userCards.collectAsLazyPagingItems()

LazyColumn {
    items(cards) { card ->
        UserCardItem(card)
    }
}
```

---

## ❓ Questions?

- **Check existing issues/discussions**
- **Join our Discord** (if available)
- **Email maintainers** (if urgent)

---

## 🎉 Recognition

Contributors will be:
- Listed in CONTRIBUTORS.md
- Mentioned in release notes
- Given credit in app About page

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

**Thank you for contributing to NeoCard! 🚀**

Your efforts help make this project better for everyone.
