# Achievement Helper

## This is still WIP!

## Runtime Requirements
- Java 21 or newer. You can download a runtime-jre from [here](https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/OpenJDK21U-jre_x64_windows_hotspot_21.0.8_9.msi)
  - During install, make sure you select `Modify Path variable` and `Set or override JAVA_HOME variable`. The registry keys option is not required, but recommended in general.
- Python 3.10+ (Only if using the "legit-steam" branch). This will be required until JavaSteam supports binary vdf's.

## How to build

- Make sure you have git installed. If not run the following in a commandline:
```cmd
winget install git
```
Or install it manually from [here](https://github.com/git-for-windows/git/releases/download/v2.50.1.windows.1/Git-2.50.1-64-bit.exe)
- Then run the following commands:
```cmd
git clone "https://github.com/lunatic-gh/achievement-helper" "%HOMEPATH%/Downloads/achievement-helper"
cd "%HOMEPATH%/Downloads/achievement-helper"
.\mvnw clean package
```
- When finished, the built jar will be at `C:/Users/<user>/Downloads/achievement-helper/target/achievement-helper.jar`. Do not use the `achievement-helper-<version>.jar`, it does not include the required dependencies.

## How to run
- To run it manually, you can run the following from a commandline:
 ```cmd
javaw -jar "path/to/achievement-helper.jar"
```
  - If you want to have a console logging window, use `java` instead of `javaw`

- Alternatively, when installing a JRE (see [Runtime Requirements](#runtime-requirements)), you can select `associate jar files` to make jar-files automatically open with your JRE when double-clicking. That way, you can open them the same way as any other executable.

## How does it work
- The first time you open the app, it will be quite empty. That is because it obviously has no idea what games you have installed. Searching your entire set of drives would take hours, or weeks on HDD's. And even if it didnt took that long, There's no way to know what steam app-id a game has. So you'll need to manually add every game you have installed.
  - Click `Add Game manually` at the top-left.
  - If you do this the first time, you'll get a login popup. To retrieve most data from steam servers via protobufs (the only way that exist to my knowledge), it requires authentication. Therefore, you'll have to login once, and the app will save a login-token. In the future, it will login using that token and your username, so it'll just happen silently.
  - You'll only have to manually login again if your token expires, which usually takes months. Token Auto-Renewal is planned.
  - Once logged in, You'll get a popup. Enter the steam-id of the game you'd like to add. You can get that app-id from the steam store page url of the game, it's always a digit number. For example, Elden ring uses `1245620`.
  - Once you click OK, The program will try retrieving the Game & Achievement-Data plus icons. This can take a while. Once done, the popup will close, and the game will appear in the sidebar.
- When clicking the game, you'll see a list of all achievements and their data. If you have one completed, it'll show on the right side, and also change from black/white to a colored icon.
- It will also run in the background, and show custom notifications once an achievement is completed, similar to Steam's own notifications.
  - For this to work, the game has to either be a legit steam game, or it has to use a supported emulator (Codex, RUNE, etc...). For a list of supported ones see the section below.
  - This works by keeping a local database with all completed ones, and polling the emulators' generated dumps for changes. once a new achievement was dumped by one of those, we'll update the database and show a popup.


## Supported Emulators/Environments
- Currently the program supports the following:
  - RUNE (C:/Users/Public/Documents/Steam/RUNE/...)
  - CODEX (C:/Users/Public/Documents/Steam/CODEX/...) (See the "legit-steam" branch)
  - STEAM (C:/Program Files (x86)/Steam/appcache/stats/...) (See the "legit-steam" branch)
 
- Support for the following is being worked on:
  - Goldberg - No idea how it works at all...
 
- For other emulators, please open an Issue. If you want to make it easier, attach a sample achievements file that the emulator in question generates, so i know how to read them.
