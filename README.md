# ChatJump

<div align="center">
  <video src="https://github.com/user-attachments/assets/5db33177-59aa-45d1-a8c2-32b4a94b7199" controls width="500"></video>
</div>

ChatJump is an Android application that lets you chat with OpenAI-powered assistants both through text and voice.  It demonstrates a modern Android stack (Compose, Hilt, Room, Retrofit/OkHttp) organised around an **MVI (Model-View-Intent)** architecture.

---

## Table of Contents
1.  Features
2.  Tech Stack
3.  Project Structure
4.  MVI Architecture
5.  Build & Run
6.  API Keys
7.  Contributing

---

## 1. Features  
•  **Text chat** with streaming responses.  
•  **Voice chat** with automatic speech-to-text (Whisper) and text-to-speech (OpenAI TTS).  
•  **Conversation history** stored locally via Room.  
•  **Markdown rendering** for AI responses.  
•  Offline / network-error handling with Snackbars and retry.  
•  Clean, Material 3 Compose UI.

## 2. Tech Stack
* **Language** – Kotlin  
* **UI** – Jetpack Compose + Material 3  
* **Dependency Injection** – Hilt  
* **Persistence** – Room  
* **Networking** – Retrofit + OkHttp (OpenAI REST)  
* **Coroutines** – Flow / channels  
* **Audio** – Android Media3 ExoPlayer + OpenAI TTS / Whisper  
* **Architecture** – MVI (Model-View-Intent)

## 3. Project Structure (modules / layers)
```
app/
 ├─ data/           # Room DAOs, entities, Retrofit clients, repository implementations
 ├─ domain/         # Business models and repository interfaces + use-cases
 ├─ ui/             # Compose screens, view-models, state, events, components
 ├─ di/             # Hilt modules (DatabaseModule, NetworkModule, RepositoryModule, …)
 └─ util/           # Misc. helpers (NetworkUtil, etc.)
```

## 4. MVI Architecture in ChatJump
ChatJump follows a unidirectional-data-flow pattern inspired by MVI:

1. **Intent** (aka Event)  
   • `ChatEvent`, `ConversationHistoryEvent`, `VoiceChatEvent` describe *user actions* or *UI triggers*.

2. **ViewModel**  
   • Receives events via `onEvent(...)`.  
   • Executes business logic / use-cases.  
   • Reduces results into an immutable **State** (`ChatState`, `VoiceChatState`, …) exposed as `StateFlow`.

3. **View** (Compose)  
   • Collects `state` and renders UI.  
   • Sends new **Intent** back to ViewModel (`viewModel.onEvent(...)`).

4. **Side-effects**  
   • One-off UI commands (scroll, navigation, Snackbars) are sent through a `Channel` as `UiEvent`s to avoid state pollution.

The data flow is always: **View ➜ Intent ➜ ViewModel ➜ new State / UiEvent ➜ View**.

### Example: Sending a message
```
User taps Send  -->  ChatEvent.OnSendMessage
                        │
                        ▼
                ChatViewModel
                    • Validates input / connectivity
                    • Updates state isThinking = true
                    • Launches use-case GetAIResponseUseCase
                    • Emits UiEvent.ScrollToBottom
                        │
                        ▼
                 ChatScreen (Compose)
                    • Shows ThinkingBubble
                    • Scrolls list to bottom
```

### Why MVI?
* Predictable, testable UI: state is *single source of truth*.
* Easier to handle configuration changes.
* State immutability prevents unexpected UI glitches.

## 5. Build & Run
1.  **Clone** the repo.  
2.  Put your OpenAI API key in `apikeys.properties`:
    ```properties
    OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxx
    ```
3.  Open the project in **Android Studio**.  
4.  Connect a device or start an emulator.  
5.  Click *Run ▶️*.

> Minimum SDK 24 (Android 7.0)

## 6. API Keys
`build.gradle.kts` reads the key from `apikeys.properties`. **Never commit secrets** – the file is `.gitignore`d by default.

---
