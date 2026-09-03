# BattleshipJava: a code review

*Reviewed at commit `0133b2e` on `main`, the commit tagged v1.0.1, v2.0.0 and v2.0.1. Reviewed on Linux with JDK 21 and JavaFX 21 jars from Maven Central, so the Windows packaging script was read, not run. Line numbers refer to that commit.*

> **Status.** The commits on this branch fix most of what is below: the out-of-bounds crash, the placement message, forced fullscreen, the dead fire button, the AI's sunk-ship handling (74.3 to 58.0 shots per win), sunk-ship announcements including over the network, the game-start race, `.gitignore`, the Gradle build with `packageApp`, twelve JUnit tests, the GitHub Actions workflow that builds Windows, macOS and Linux releases from tags, and the README. Still open: pushing the newer source from your backup, and the longer-term items (CSS stylesheet, protocol clean-up, `AudioClip`, the swallowed exceptions).

## The short version

This is a complete, playable game with a real architecture behind it. The game rules live in a package with zero JavaFX imports, the networking hops every callback onto the JavaFX thread correctly, and the whole project compiles with zero warnings under `-Xlint:all`. That puts it ahead of most hobby Java projects.

The five things to do first, in order:

1. **Push the code that matches your releases.** Tags v1.0.1, v2.0.0 and v2.0.1 all point at the same commit as the v1.0 README update. The v1.0.1 notes mention victory/loss music and an end-of-game fix; neither is in this source. Anyone who clones the repo gets v1.0.
2. **Write a real README.** It is ten lines and doesn't say what Java version you need, how to build, how LAN play works, or where the download is.
3. **Stop forcing fullscreen plus always-on-top.** As shipped, no other window can appear above the game, and you can't test host and join on one PC.
4. **Fix the one crash in the network path.** A malformed attack message throws before your INVALID check can run (`src/ui/GameScene.java:197`).
5. **Add tests and a build tool.** Your `game` package is pure Java and begs for JUnit. Gradle makes that, and running on any OS, a one-liner.

Everything below is detail.

## What's genuinely good

Keep doing these.

- **Clean package boundaries.** `game` and `ai` don't import JavaFX at all. I compiled them on their own and drove them from a test harness without the UI. That separation is the single most important design decision in the project, and you got it right.
- **Modern Java, used well.** `Coordinate` is a record, `ShipType` and `Difficulty` are enums that carry their own data, `Board.getShips()` returns `List.copyOf`, and `SetupScene` uses switch expressions. Nothing is over-engineered.
- **Threading is correct.** `NetworkGameSession` runs sockets on daemon threads, guards writes with a lock, marks shared fields `volatile`, and routes every callback through `Platform.runLater`, so UI code never touches a socket thread. Most first networking projects get this wrong.
- **Testability was on your mind.** `BattleshipAI` has a constructor that takes a `Random`. That is exactly what made it possible to simulate 2,000 games below.
- **The listener interface uses default methods** (`NetworkMessageListener`), so `SetupScene` and `GameScene` each override only what they care about.
- **Tiles show a letter, not just a color** (X, o, S). That is an accessibility win most people skip.
- **Zero compiler warnings** with every lint check on. Since both scripts pass `-Xlint:none`, I suspect you didn't know this. Turn it on and keep it that way.
- **It ships.** A launch script with change detection, a jpackage script, a real exe, four tagged releases. Finishing is a skill.

## Fix first

### 1. The repo is behind your releases

`git ls-remote --tags` shows:

| Tag | Commit |
|---|---|
| v1.0.0 | `1c7490e` Battleship v1.0 |
| v1.0.1 | `0133b2e` Update launch instructions in README.md |
| v2.0.0 | `0133b2e` (same commit) |
| v2.0.1 | `0133b2e` (same commit) |

The v1.0.1 release notes say "Added victory/loss music" and "Fixed an issue where after the game ends it would shut down BattleshipJava." `AudioManager` has exactly four sounds: menu, fire, explosion, select. So the source for v1.0.1 through v2.0.1 exists only on your machine.

Fix: commit and push your current source, then tag from the commit you actually built. Habit going forward: commit after each change, tag when you release. Also, the v2.0.0 and v2.0.1 notes say "I'm lazy." Future you, six months from now, will want to know what changed. Two bullet points is enough.

### 2. README

Right now it is ten lines and tells people to download a zip "in the first page." A reader needs:

- One sentence on what it is, and a screenshot. Screenshots matter more than anything else on a GitHub page.
- How to play: R rotates, ships can't touch (see below), what Level 1 and Level 2 mean.
- A link to Releases for the download, and one line on why Windows warns (the exe isn't code-signed, so SmartScreen doesn't trust it).
- How to run from source: Java 21 or newer (`List.getFirst()` at `src/ai/BattleshipAI.java:48` fails on 17; I checked), a JavaFX SDK, and `.\run.ps1 -JavaFxLib <path>`. Say which JDK you build with, since the JavaFX 26 SDK may need a newer one.
- LAN play: both players on the same network, host shares the IP shown on screen, port 50505, expect a Windows Firewall prompt the first time.
- Credits for the images and audio, and the license.

### 3. Fullscreen kiosk mode

`src/Main.java:26-46` and `110-123`: undecorated window, `setAlwaysOnTop(true)`, fullscreen re-applied on every focus change, and Esc disabled with `KeyCombination.NO_MATCH`. In practice: nobody can look at Discord or a browser while the game is open, if the game hangs the only way out is a Task Manager you can't see, and you can't run host and join side by side on one PC to test networking.

Fix: a normal decorated window, F11 to toggle fullscreen, no always-on-top, delete the focus listener and `enforceFullscreen`. While you're there, the stage is set to 1600x900 with a 1280x720 minimum, and every scene is created at 1100x760. Pick one size and let the layouts be the source of truth.

### 4. Crash on a malformed attack

`src/ui/GameScene.java:197` indexes `playerButtons[y][x]` before `receiveAttack` runs, so the `INVALID` check on line 199 can never trigger. An `ATTACK:10,0` from the other side throws `ArrayIndexOutOfBoundsException` on the JavaFX thread. Add `if (!playerBoard.isInBounds(x, y))` before `animateShot`, and treat it like the illegal-move case you already handle.

### 5. Tests, Gradle, CI

Your `game` package is pure Java: no JavaFX, no threads, injectable randomness. That is the easiest kind of code to test, and there are no tests. Five to start:

- `canPlaceShip` rejects out-of-bounds, overlapping, and touching placements.
- `receiveAttack` reports `sunkShip` on the last tile of a ship, and `gameOver` after all 17.
- `randomize` always places all five ships.
- `BattleshipAI.nextShot()` never returns the same tile twice.
- Level 2 shoots a neighbor after a hit.

Put the project on Gradle with the `org.openjfx.javafxplugin` plugin. Then `gradlew run` works on Windows, Mac and Linux, JavaFX downloads itself, `gradlew test` runs JUnit, and a ten-line GitHub Actions workflow runs the tests on every push. It also removes the hardcoded `C:\Users\samue\Downloads\...` path from both scripts.

## Gameplay and AI

**Ships can't touch, but the game doesn't say so.** `src/game/Board.java:126` forbids placing a ship on any tile adjacent to another ship, including diagonally. I verified it: a destroyer directly below a carrier is rejected, one row further down is accepted. But the message at `src/ui/SetupScene.java:311` says "avoid overlap," so a player who does exactly that gets rejected with no explanation. Either explain the rule in the UI and README, or drop it (the classic rules allow touching).

**Level 2 keeps shooting around ships it already sank.** `handleShotResult` receives only hit or miss (`src/ui/GameScene.java:372`), so after a sink the queue still holds the sunk ship's neighbors. I drove the shipped AI through 2,000 random games per level:

| AI | Average shots to win | Shots into the ring around already-sunk ships |
|---|---|---|
| Level 1 | 95.4 | 8.9 |
| Level 2 | 74.3 | 16.5 |

Under your no-touch rule, those ring tiles can never hold a ship, so Level 2 wastes about 16 of its 74 shots. Two easy steps: pass `outcome.isSunkShip()` into the AI and clear the queue on a sink; then mark every tile around a sunk ship as attempted. After that, a real Level 3: when two hits line up, keep going along that line, and during the hunt only shoot tiles where `(x + y) % 2 == 0` (parity hunting: the smallest ship is two long, so it must cross one of them). Keep the simulator as a test so you can watch each change move the number.

**"You sank the Cruiser!"** `AttackOutcome` already carries `isSunkShip()` and `getShipType()`, and nothing in the UI reads them. Cheapest satisfying feature in the project. In network mode the result message only says HIT or MISS, so the opponent can never know a ship sank, which is why the enemy panel shows "Shots fired" instead of "Enemy ships remaining." Extend the message: `RESULT:HIT:SUNK:CRUISER`.

## Networking

- **The end-of-game handshake is fragile.** The loser sends RESULT, then LOSE, then closes the socket. The winner then sends TURN and WIN to a closed socket (`src/ui/GameScene.java:239` and `247`). It works today because TCP buffers the first write, not because the protocol is right. Put everything the other side needs into one message (`RESULT:HIT:SUNK:CARRIER:GAMEOVER`) and let the winner close the connection.
- **Message names read backwards.** Sending `WIN` triggers the receiver's `onLose`. Name messages by what happened, not who is speaking: `ALL_SHIPS_SUNK` needs no translation.
- **A small race in `attemptGameStart`** (`src/network/NetworkGameSession.java:203`). It runs on the JavaFX thread from `sendReady` and on the reader thread from the READY handler. The check-then-set on `started` isn't atomic, so both can pass and the host sends TURN twice. Rare, but real; `synchronized` on the method fixes it. Good lesson: `volatile` makes a read fresh, it doesn't make check-then-act safe.
- **The host can't recover from a dropped joiner.** `readLoop`'s `finally` calls `close()`, which closes the `ServerSocket`. The host has to go back to the menu and re-host. Reopen the listener on a disconnect during setup.
- **`getLocalIpAddress`** (`src/ui/SetupScene.java:454`) picks the first non-loopback IPv4. On a PC with VirtualBox, WSL, Hyper-V or a VPN adapter, that is often the wrong one. Show all candidates, or prefer `isSiteLocalAddress()` ones.
- **`NetworkGameSession` imports JavaFX** for `Platform.runLater`. Pass a `Consumer<Runnable>` dispatcher into the constructor and the class becomes testable with plain sockets and no UI.

## UI code

- **Dead "select then fire" mechanic.** `fireButton` is created, hidden, and permanently disabled (`src/ui/GameScene.java:145-150` and `473-475`). `selectedTargetX/Y` and `styleSelectedTargetButton` exist only to support it, and the yellow selected style never renders because the grid isn't refreshed between selecting and clearing. Delete all of it.
- **Five copies of "try classpath, fall back to a file."** `Main.loadWindowIcon`, `MainMenuScene.loadShipImage`, `GameScene.loadBombImage`, `UiFactory.loadImage`, `AudioManager.loadMedia`. Put `resources` on the classpath in `run.ps1` (`-cp "out;resources"`), delete every fallback, keep one `Resources.image(path)` helper.
- **Styling by string.** 57 hex color literals across 12 distinct colors, `"Georgia"` 12 times, 9 inline `setStyle` strings. JavaFX has real CSS: one `battleship.css` with `.menu-button`, `.tile`, `.tile-hit`, `.tile-miss`, `.tile-ship`, and `button.getStyleClass().setAll("tile", "tile-hit")`. Biggest cleanup available, and a skill that transfers.
- **The 10x10 button loop is written three times** (`src/ui/SetupScene.java:103`, `src/ui/GameScene.java:272` and `288`). One `UiFactory.createGrid(Button[][] into, BiConsumer<Integer, Integer> onClick)`.
- **18 `catch (RuntimeException ignored)` blocks.** When audio silently stops working, `AudioManager` sets `audioAvailable = false` and tells nobody why. Catch the specific exception you expect, and at minimum print it. Let the ones you don't expect crash while you're developing; that is how you find bugs.
- **Use `AudioClip` for the short effects.** It is built for exactly this (low latency, overlapping plays, no dispose bookkeeping) and removes most of `AudioManager`. Keep `MediaPlayer` for the menu loop. Also `setCycleCount(INDEFINITE)` (line 61) and the seek-and-replay in `setOnEndOfMedia` (line 63) do the same job; keep one.
- **Smaller:** `AttackOutcome` could be a record like `Coordinate`. `Difficulty` uses `toString()` for its display name while `ShipType` uses `getDisplayName()`; pick one. Three fully qualified names where an import belongs (`src/ui/GameScene.java:82`, `src/network/NetworkGameSession.java:239`, `src/Main.java:127`). `Main` shuts down the setup scene on exit but not the game scene.

## Build and repo hygiene

- **`build-exe.ps1:45` bakes your Downloads path into the packaged app** via `--java-options --module-path`. On another PC that folder doesn't exist, so JavaFX can't load. Unless you copied the SDK by hand, the app-image can't start elsewhere. Pass `--module-path` and `--add-modules` to jpackage itself so jlink bundles JavaFX into the runtime image. I couldn't run this on Linux, so check it on a second machine.
- **`-Xlint:none`** in both scripts hides warnings. Your code has none. Switch to `-Xlint:all`.
- **`.gitignore`** doesn't cover `out/`, `dist/` or `package/`. Add them before a `git add .` commits copied resources.
- **Packages** are `game`, `ui`, `ai` with `Main` in the default package. Convention is a reverse-domain prefix like `dev.samuelarther.battleship.game`. Cosmetic, but every Java codebase you'll ever join does it.
- **One commit holds the whole game** ("Battleship v1.0"). Smaller commits with messages that say what changed give you a history you can search and bisect.
- **Credits.** Say where `ship.png`, `board.png`, `icon.jpg` and the audio came from, or that you made them. CC0 for the code is fine; MIT is the more common choice, and either works.

## A suggested order

1. Push your current source and write two lines of release notes for v2.0.1. Half an hour.
2. README with a screenshot. An hour.
3. Windowed mode with F11. Half an hour.
4. Bounds check on incoming attacks. Five minutes.
5. Gradle, JUnit, the five tests above, GitHub Actions. An afternoon.
6. CSS stylesheet. An afternoon.
7. AI: clear the queue on sink, ring marking, then parity and line-following, with the simulator as a test. A weekend.
8. Protocol v2 with sunk info and a clean game-over message. A weekend.

Do the first four before anything else. They are small, and they change how the project looks to anyone who finds it.
