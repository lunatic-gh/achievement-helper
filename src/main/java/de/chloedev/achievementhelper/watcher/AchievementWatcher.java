package de.chloedev.achievementhelper.watcher;

import de.chloedev.achievementhelper.impl.Achievement;
import de.chloedev.achievementhelper.io.Configuration;
import de.chloedev.achievementhelper.io.GameDataStorage;
import de.chloedev.achievementhelper.steam.Steam;
import de.chloedev.achievementhelper.util.Logger;
import de.chloedev.achievementhelper.util.Pair;
import de.chloedev.achievementhelper.util.Util;
import in.dragonbra.javasteam.types.KeyValue;
import in.dragonbra.javasteam.util.stream.BinaryReader;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AchievementWatcher {
  private final Map<WatchKey, Path> keyToDir = new HashMap<>();
  private final List<PathMatcher> matchers = new ArrayList<>();
  private WatchService watchService;

  public AchievementWatcher() {
  }

  public void init() {
    try {
      watchService = FileSystems.getDefault().newWatchService();
      Configuration config = Configuration.getInstance();

      // @formatter:off
      List<String> defaultPaths = List.of(
        "C:/Program Files (x86)/Steam/appcache/stats/UserGameStats_*.bin",
        "<PUBLIC>/Documents/Steam/RUNE/*/achievements.ini",
        "<PUBLIC>/Documents/Steam/RUNE/*/stats.ini",
        "<PUBLIC>/Documents/Steam/CODEX/*/achievements.ini",
        "<PUBLIC>/Documents/Steam/CODEX/*/stats.ini"
      );
      // @formatter:on
      List<String> paths = new ArrayList<>(config.get("watcherPaths", new JSONArray()).toList().stream().map(Object::toString).toList());
      for (int i = defaultPaths.size() - 1; i >= 0; i--) {
        String path = defaultPaths.get(i);
        if (!paths.contains(path)) {
          paths.addFirst(path);
        }
      }

      defaultPaths.forEach(path -> {
        if (!paths.contains(path)) {
          paths.addFirst(path);
        }
      });

      config.set("watcherPaths", new JSONArray(paths));

      for (String raw : paths) {
        String expanded = expandEnv(raw);
        Pair<Path, String> parts = splitGlob(expanded);
        Path baseDir = parts.getLeft();
        String glob = parts.getRight();
        registerAll(baseDir);
        String baseAsPattern = baseDir.toAbsolutePath().toString().replace("\\", "/");
        String pattern = glob.isEmpty() ? "glob:" + baseAsPattern : "glob:" + baseAsPattern + "/" + glob;
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
        matchers.add(matcher);
      }
      scanAll();
      processEvents();
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  private void processEvents() {
    while (true) {
      try {
        WatchKey key = watchService.take();
        Path dir = keyToDir.get(key);
        if (dir == null) {
          key.reset();
          continue;
        }
        for (WatchEvent<?> evt : key.pollEvents()) {
          Path path = dir.resolve((Path) evt.context()).toAbsolutePath();
          for (PathMatcher m : matchers) {
            if ((evt.kind() == StandardWatchEventKinds.ENTRY_CREATE || evt.kind() == StandardWatchEventKinds.ENTRY_MODIFY) && m.matches(path)) {
              CompletableFuture.runAsync(() -> {
                Pair<Long, List<String>> achievements = parse(path);
                Logger.info(achievements.getRight().toString());
                long appId = achievements.getLeft();
                for (String achId : achievements.getRight()) {
                  Achievement achievement = GameDataStorage.getInstance().getAchievementById(appId, achId);
                  if (achievement != null && !achievement.isAchieved()) {
                    AchievementQueue.getInstance().addToQueue(appId, achId);
                  }
                }
              });
            }
          }
          if (evt.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(path)) {
            registerAll(path);
          }
        }
        key.reset();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      } catch (Exception e) {
        Logger.error(e);
      }
    }
  }

  private void registerAll(Path start) {
    if (!Files.exists(start)) {
      Logger.warn("Ignoring Path '%s', because it does not exist", start);
      return;
    }
    try (Stream<Path> stream = Files.walk(start)) {
      stream.filter(Files::isDirectory).forEach(dir -> {
        try {
          WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
          keyToDir.put(key, dir);
        } catch (IOException e) {
          Logger.error(e);
        }
      });
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  private String expandEnv(String pattern) {
    for (Map.Entry<String, String> e : System.getenv().entrySet()) {
      pattern = pattern.replace("<" + e.getKey() + ">", e.getValue());
    }
    return pattern;
  }

  private Pair<Path, String> splitGlob(String pattern) {
    int globIdx = findFirstGlobChar(pattern);
    if (globIdx < 0) {
      return Pair.of(Paths.get(pattern).toAbsolutePath(), "");
    }
    int sepBack = pattern.lastIndexOf('\\', globIdx);
    int sepFwd = pattern.lastIndexOf('/', globIdx);
    int sepIdx = Math.max(sepBack, sepFwd);

    String basePart, globPart;
    if (sepIdx >= 0) {
      basePart = pattern.substring(0, sepIdx);
      globPart = pattern.substring(sepIdx + 1);
    } else {
      basePart = ".";
      globPart = pattern;
    }
    while (basePart.endsWith("\\") || basePart.endsWith("/")) {
      basePart = basePart.substring(0, basePart.length() - 1);
    }
    Path baseDir = Paths.get(basePart).toAbsolutePath();
    return Pair.of(baseDir, globPart);
  }

  private int findFirstGlobChar(String s) {
    return s.indexOf('*');
  }

  /**
   * Scans all files for achievements initially.
   * <p>
   * This is done at startup once so that already completed achievements get cached.
   */
  public void scanAll() {
    Set<Path> scanned = new HashSet<>();
    for (Path dir : keyToDir.values()) {
      try (Stream<Path> files = Files.walk(dir)) {
        files.filter(Files::isRegularFile).forEach(path -> {
          if (scanned.contains(path)) return;
          for (PathMatcher m : matchers) {
            if (m.matches(path)) {
              try {
                Pair<Long, List<String>> achievements = parse(path);
                long appId = achievements.getLeft();
                for (String achId : achievements.getRight()) {
                  Achievement achievement = GameDataStorage.getInstance().getAchievementById(appId, achId);
                  if (achievement != null) {
                    achievement.setAchieved(true);
                    GameDataStorage.getInstance().save();
                  }
                }
              } catch (Exception e) {
                Logger.error(e);
              }
              break;
            }
          }
          scanned.add(path);
          Logger.debug("Scanned path '%s'", path.toAbsolutePath());
        });
      } catch (IOException e) {
        Logger.error(e);
      }
    }
  }

  private Pair<Long, List<String>> parse(Path path) {
    return switch (Util.getFileExtension(path)) {
      case "ini" -> parseIni(path.toFile());
      case "json" -> parseJson(path.toFile());
      case "bin" -> parseBin(path.toFile());
      default -> Pair.of(0L, Collections.emptyList());
    };
  }

  /**
   * Below are the parsing functions for various types of achievement files.
   */
  private Pair<Long, List<String>> parseIni(File file) {
    Pair<Long, List<String>> EMPTY = Pair.of(0L, Collections.emptyList());
    try {
      File parentDir = file.getParentFile();
      if (parentDir == null || !parentDir.getName().matches("\\d+")) {
        return EMPTY;
      }
      long appId = Long.parseLong(parentDir.getName());
      switch (file.getName()) {
        case "achievements.ini" -> {
          // This file is relatively easy to parse even without any ini libraries.
          // It just contains a "[SteamAchievements]" category that contains a list with all completed achievements with the following format:
          //
          //     [SteamAchievements]
          //     XXXXX=AchievementId
          //     XXXXX=OtherAchievementId
          //     ...
          //     Count=X
          //
          // Not sure if the ID matters...
          List<String> achievements = new ArrayList<>();
          BufferedReader reader = new BufferedReader(new FileReader(file));
          String category = "";
          for (String line : reader.lines().toList()) {
            if (line.isEmpty()) {
              continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
              category = line.substring(1, line.length() - 1);
              continue;
            }
            if (category.equals("SteamAchievements") && Character.isDigit(line.charAt(0)) && line.contains("=")) {
              String[] split = line.split("=");
              if (split.length == 2) {
                achievements.add(split[1]);
              }
            }
          }
          return Pair.of(appId, achievements);
        }
        case "stats.ini" -> {
          // This is more complicated than the above. It rather stores StatId's with their current progress.
          // But because we do have their max progress saved, we can check their current progress against the max.
          // Format:
          //
          //     [UserStats]
          //     StatName=X
          //     OtherStatName=X
          //     ...
          List<String> stats = new ArrayList<>();
          BufferedReader reader = new BufferedReader(new FileReader(file));
          String category = "";
          for (String line : reader.lines().toList()) {
            if (line.isEmpty()) {
              continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
              category = line.substring(1, line.length() - 1);
              continue;
            }
            if (category.equals("UserStats") && line.contains("=")) {
              String[] split = line.split("=");
              if (split.length != 2) {
                continue;
              }
              String statId = split[0];
              int statValue = Integer.parseInt(split[1]);
              List<Achievement> cachedAchievements = GameDataStorage.getInstance().getAchievementsForGame(appId);
              Achievement achievement = cachedAchievements.stream().filter(ach -> !ach.getStatId().isEmpty() && ach.getStatId().equals(statId)).findFirst().orElse(null);
              // No idea if it can be bigger than max, but let's be safe
              if (achievement != null && statValue >= achievement.getMaxProgress()) {
                stats.add(achievement.getId());
              }
            }
          }
          return Pair.of(appId, stats);
        }
        default -> {
          return EMPTY;
        }
      }
    } catch (Exception e) {
      Logger.error(e);
    }
    return EMPTY;
  }

  private Pair<Long, List<String>> parseJson(File file) {
    Pair<Long, List<String>> EMPTY = Pair.of(0L, Collections.emptyList());
    // TODO: Implement
    return EMPTY;
  }

  /**
   * TODO: Replace {@code python} dependencies once {@link BinaryReader} properly supports Binary VDF's
   */
  private Pair<Long, List<String>> parseBin(File file) {
    Pair<Long, List<String>> EMPTY = Pair.of(0L, Collections.emptyList());
    var matcher = Pattern.compile("^UserGameStats_(\\d+)_(\\d+)\\.bin$").matcher(file.getName());
    if (!matcher.matches()) {
      return EMPTY;
    }
    long steamId = Long.parseLong(matcher.group(1));
    long appId = Long.parseLong(matcher.group(2));
    if (Steam.getInstance().getClient().getSteamID() == null || Steam.getInstance().getClient().getSteamID().getAccountID() != steamId) {
      return EMPTY;
    }
    try {
      KeyValue result = KeyValue.tryLoadAsBinary(file.getAbsolutePath());
      if (result == null || result == KeyValue.INVALID) {
        return EMPTY;
      }
      List<Achievement> achList = GameDataStorage.getInstance().getAchievementsForGame(appId);
      if (achList.isEmpty() || result.getChildren().isEmpty()) {
        return Pair.of(appId, Collections.emptyList());
      }
      List<String> unlocked = result.getChildren().stream().filter(key -> key.getName().matches("\\d+")).flatMap(statObj -> {
        int stat = Integer.parseInt(statObj.getName());
        KeyValue achTimesObj = statObj.get("AchievementTimes");
        if (achTimesObj == null || achTimesObj == KeyValue.INVALID) {
          return Stream.empty();
        }
        return achTimesObj.getChildren().stream().map(kv -> Long.parseLong(kv.getName())).flatMap(bit -> {
          return achList.stream().filter(a -> a.getStat() == stat && a.getBit() == bit).map(Achievement::getId);
        });
      }).collect(Collectors.toList());
      return Pair.of(appId, unlocked);
    } catch (Exception e) {
      Logger.error(e);
      return EMPTY;
    }
  }
}
