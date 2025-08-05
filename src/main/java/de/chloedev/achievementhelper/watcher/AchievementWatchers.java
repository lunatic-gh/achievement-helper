package de.chloedev.achievementhelper.watcher;

import de.chloedev.achievementhelper.watcher.impl.AchievementWatcherRune;
import de.chloedev.achievementhelper.watcher.impl.AchievementWatcherSteam;

public class AchievementWatchers {
  // @formatter:off

  /**
   * When creating new watchers, add them to this array, and they'll automatically be used.
   */
  public static final AchievementWatcher[] WATCHERS = {
    new AchievementWatcherRune()
  };
  // @formatter:on
}
