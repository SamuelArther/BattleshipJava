# Battleship

[![Build](https://github.com/SamuelArther/BattleshipJava/actions/workflows/build.yml/badge.svg)](https://github.com/SamuelArther/BattleshipJava/actions/workflows/build.yml)

A desktop Battleship game written in Java and JavaFX. Play against the computer, or host a game on your local network and have a friend join.

![A battle in progress](docs/screenshots/battle.png)

## Download

Get the latest build for your system from the [Releases page](https://github.com/SamuelArther/BattleshipJava/releases/latest). Each download carries its own Java runtime, so nothing else needs to be installed.

| System | File | How to run it |
|---|---|---|
| Windows | `Battleship-windows.zip` | Extract it, open the `Battleship` folder, run `Battleship.exe`. |
| macOS (Apple Silicon) | `Battleship-macos-apple-silicon.zip` | Extract it, drag `Battleship.app` into Applications, open it. |
| Linux | `Battleship-linux.tar.gz` | Extract it, run `Battleship/bin/Battleship`. |

**Windows says "Windows protected your PC".** The app isn't code-signed, so SmartScreen doesn't recognise it. Click *More info*, then *Run anyway*.

**macOS says the app can't be opened or is damaged.** Same reason. Open *System Settings*, *Privacy & Security*, scroll down and click *Open Anyway*. Or run `xattr -cr /Applications/Battleship.app` once in Terminal.

Intel Macs don't have a build yet. They can run the game from source (see below).

## How to play

![Placing the fleet](docs/screenshots/setup.png)

1. **Place your fleet.** Click a ship in the Fleet list, then click a square on the grid. Press **R** (or *Rotate*) to switch between horizontal and vertical. *Randomize* places everything for you.
2. **Ships can't touch.** Ships can't overlap, and they can't sit next to each other, not even corner to corner. There has to be at least one square of water between them.
3. **Fire.** In the battle, click a square on the enemy grid. A red **X** is a hit, a grey **o** is a miss. The status line tells you when you sink a ship. Sink all five to win.

The fleet:

| Ship | Length |
|---|---|
| Carrier | 5 |
| Battleship | 4 |
| Cruiser | 3 |
| Submarine | 3 |
| Destroyer | 2 |

**Keys:** **R** rotates the ship you're placing. **F11** toggles fullscreen and **Esc** leaves it.

### Against the computer

Pick a difficulty on the placement screen.

- **Level 1** fires at random.
- **Level 2** hunts: after a hit it fires at the neighbouring squares until the ship is sunk, then goes back to searching. Over thousands of simulated games it wins in about 58 shots on average, where Level 1 needs about 95.

### Over the network

One player hosts and the other joins. Both computers need to be on the same network (the same Wi-Fi, for example).

1. The host clicks **Host Game**. The screen shows an IP address. Tell it to the other player.
2. The joiner clicks **Join Game**, types that IP address, and clicks **Connect**.
3. Both players place their fleet and click **Ready**. The host fires first.

The game uses port 50505. The first time you host, Windows Firewall may ask whether to allow Battleship through; say yes. If the joiner can't connect, check that both computers are on the same network and that the host's firewall isn't blocking it. If the host has virtual network adapters (VirtualBox, WSL, a VPN), the address shown on screen may be the wrong one; `ipconfig` on Windows or `ifconfig` on macOS and Linux lists them all.

## Building from source

You need a JDK, version 21 or newer. [Adoptium Temurin](https://adoptium.net/) is a good free one. Everything else, including JavaFX, downloads by itself the first time you build.

```
git clone https://github.com/SamuelArther/BattleshipJava.git
cd BattleshipJava
./gradlew run
```

On Windows, use `gradlew run` (no `./`).

| Command | What it does |
|---|---|
| `./gradlew run` | Runs the game. |
| `./gradlew test` | Runs the tests in `test/`. |
| `./gradlew packageApp` | Builds a self-contained app for the system you're on, into `build/jpackage/`. |

### Making a release

Every push runs the tests and builds the app for Windows, macOS and Linux on GitHub Actions. To publish a release, tag a commit with a version number and push the tag:

```
git tag v1.4
git push origin v1.4
```

In GitHub Desktop: open *History*, right-click the commit, choose *Create Tag*, name it `v1.4`, then *Push origin*. A few minutes later the release appears on the Releases page with all three downloads attached and release notes generated from the commits.

## Project layout

```
src/Main.java         Opens the window and switches between screens
src/game/             The rules: Board, Ship, Tile, AttackOutcome. No JavaFX in here.
src/ai/               The computer opponent
src/ui/               The JavaFX screens: main menu, ship placement, battle, end
src/network/          Hosting and joining a game over a socket
src/audio/            Music and sound effects
resources/            Images and sounds
test/                 JUnit tests for game/ and ai/
packaging/            App icons for each system
.github/workflows/    Test, build and release automation
```

## Credits and license

The code is released under CC0 (see [LICENSE](LICENSE)).

Ship, board and bomb images and the sound effects: (add where they came from here, or note that you made them yourself).
