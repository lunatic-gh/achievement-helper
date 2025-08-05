package de.chloedev.achievementhelper.watcher.impl;

import de.chloedev.achievementhelper.util.Util;
import de.chloedev.achievementhelper.watcher.AchievementWatcher;
import in.dragonbra.javasteam.types.KeyValue;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementWatcherSteam extends AchievementWatcher {

  @Override
  public @NotNull File getBasePath() {
    String steamInstallPath = Util.winReadRegistryKey("HKLM\\SOFTWARE\\WOW6432Node\\Valve\\Steam", "InstallPath");
    return new File(steamInstallPath, "appcache/stats").getAbsoluteFile();
  }

  /**
   * TODO: Implement once JavaSteam's {@link KeyValue#tryLoadAsBinary(String)} is fixed.
   */
  @Override
  public Map<Long, List<String>> parseAchievements(Path basePath) {
    Map<Long, List<String>> map = new HashMap<>();
    return map;
  }
}
