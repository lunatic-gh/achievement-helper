package de.chloedev.achievementhelper.watcher;

import de.chloedev.achievementhelper.impl.Achievement;
import de.chloedev.achievementhelper.io.GameDataStorage;
import de.chloedev.achievementhelper.util.Pair;
import de.chloedev.achievementhelper.util.Util;

import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AchievementQueue {

  private static final AchievementQueue INSTANCE = new AchievementQueue();

  private final Queue<Pair<Long, Achievement>> completionQueue = new ConcurrentLinkedQueue<>();
  private final Timer timer = new Timer();

  private AchievementQueue() {
    startQueueWatcher();
  }

  public static AchievementQueue getInstance() {
    return INSTANCE;
  }

  private void startQueueWatcher() {
    CompletableFuture.runAsync(() -> {
      //noinspection InfiniteLoopStatement
      while (true) {
        Pair<Long, Achievement> entry = completionQueue.poll();
        if (entry != null) {
          Util.showAchievementNotification(entry.getLeft(), entry.getRight());
          Util.waitUntil(() -> false, 10000, null, null);
        } else {
          Util.waitUntil(() -> false, 50, null, null);
        }
      }
    });
  }

  public void addToQueue(long appId, String achievementId) {
    synchronized (completionQueue) {
      Achievement achievement = GameDataStorage.getInstance().getAchievementById(appId, achievementId);
      if (achievement != null && !achievement.isAchieved()) {
        achievement.setAchieved(true);
        GameDataStorage.getInstance().save();
        completionQueue.add(Pair.of(appId, achievement));
      }
    }
  }
}
