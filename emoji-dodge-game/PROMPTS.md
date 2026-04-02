# Prompts used during development

Below are example prompts that match the kind of AI-assisted workflow this challenge expects. Adapt them to your own session when you submit.

**Prompt 1**

Implement the “Emoji Dodge” Android challenge: home screen with title “Emoji Dodge”, short description, and Start Game; gameplay with player at bottom, random falling emojis from the top, horizontal movement via drag, AABB collision ending the game; score from survival time and dodged emojis; game over with score, Play Again, and optional Home. Use Jetpack Compose and Kotlin only, no backend.

**Prompt 2**

Add a frame-based game loop in Compose using `LaunchedEffect` and `withFrameNanos`: update positions with delta time, spawn new emojis on an interval that gets harder over time, remove emojis that pass the bottom and increment a dodged counter.

**Prompt 3**

Fix spawn timing so accumulated time uses the previous frame timestamp before updating `lastFrameNs`, and ensure `onGameOver` is only invoked once on collision.

**Prompt 4**

Add `README.md`, `TOOLS.md`, and `PROMPTS.md` under `emoji-dodge-game/` and mirror the Gradle project into `project-source/` for the required repository layout.
