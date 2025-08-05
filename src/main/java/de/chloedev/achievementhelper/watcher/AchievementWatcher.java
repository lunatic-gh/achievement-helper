package de.chloedev.achievementhelper.watcher;

import de.chloedev.achievementhelper.impl.Achievement;
import de.chloedev.achievementhelper.impl.Game;
import de.chloedev.achievementhelper.io.GameDataStorage;
import de.chloedev.achievementhelper.util.Pair;
import de.chloedev.achievementhelper.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AchievementWatcher {

  private final Queue<Pair<Long, String>> queue = new ConcurrentLinkedQueue<>();

  @SuppressWarnings("unchecked")
  private <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  public void listen() {
    Path basePath = getBasePath().toPath();
    Map<WatchKey, Path> watchKeys = new HashMap<>();

    CompletableFuture.runAsync(() -> {
      //noinspection InfiniteLoopStatement
      while (true) {
        Pair<Long, String> entry = queue.poll();
        if (entry != null) {
          Achievement achievement = GameDataStorage.getInstance().getAchievementById(entry.getLeft(), entry.getRight());
          if (achievement != null && !achievement.isAchieved()) {
            AchievementQueue.getInstance().addToQueue(entry.getLeft(), achievement);
            System.out.println("ADDED TO SECOND QUEUE");
            Util.waitUntil(() -> false, 10000, null, null);
          }
        }
      }
    });

    try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
      registerAllDirs(basePath, watcher, watchKeys);
      while (true) {
        WatchKey key = watcher.take();
        Path dir = watchKeys.get(key);
        if (dir == null) {
          key.reset();
          continue;
        }
        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }
          WatchEvent<Path> ev = cast(event);
          Path name = ev.context();
          Path child = dir.resolve(name);
          if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(child)) {
            registerAllDirs(child, watcher, watchKeys);
          }
          if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            CompletableFuture.runAsync(() -> {
              parseAchievements(basePath).forEach((id, achievementIds) -> {
                Game game = GameDataStorage.getInstance().getGames().stream().filter(g -> g.getAppId() == id).findFirst().orElse(null);
                if (game != null) {
                  for (String achievementId : achievementIds) {
                    synchronized (queue) {
                      queue.add(Pair.of(game.getAppId(), achievementId));
                      System.out.println("ADDED TO FIRST QUEUE");
                    }
                  }
                }
              });
            });
          }
        }
        boolean valid = key.reset();
        if (!valid) {
          watchKeys.remove(key);
          if (watchKeys.isEmpty()) {
            break;
          }
        }
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void registerAllDirs(Path start, WatchService watcher, Map<WatchKey, Path> watchKeys) throws IOException {
    Files.walkFileTree(start, new SimpleFileVisitor<>() {
      @Override
      public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
        System.out.println("Registering Watch Directory '%s'".formatted(dir.toString()));
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        watchKeys.put(key, dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * This should return the base path that contains the appId-folders.
   */
  public abstract @NotNull File getBasePath();

  /**
   * This should parse the achievement file(s) and return a map with the appid and all COMPLETED achievements from those files.
   * <p>
   * {@code basePath} should be used to traverse to the file(s).
   */
  public abstract Map<Long, List<String>> parseAchievements(Path basePath);
}
