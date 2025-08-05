package de.chloedev.achievementhelper.watcher.impl;

import de.chloedev.achievementhelper.watcher.AchievementWatcher;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementWatcherRune extends AchievementWatcher {

  @Override
  public @NotNull File getBasePath() {
    String s = System.getenv("PUBLIC");
    return Path.of(s, "Documents", "Steam", "RUNE").toFile().getAbsoluteFile();
  }

  @Override
  public Map<Long, List<String>> parseAchievements(Path basePath) {
    Map<Long, List<String>> map = new HashMap<>();
    // Only include dirs with numeric names.
    File[] appDirs = basePath.toFile().listFiles(file -> file.getName().matches("\\d+"));
    if (appDirs != null) {
      for (File appDir : appDirs) {
        File file = new File(appDir, "achievements.ini");
        if (file.exists()) {
          try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String section = "";
            List<String> achievements = new ArrayList<>();
            for (String line : reader.lines().toList().stream().map(String::trim).toList()) {
              if (line.isEmpty()) {
                continue;
              }
              if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                continue;
              }
              if (section.equals("SteamAchievements")) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2 && !parts[0].trim().equalsIgnoreCase("Count")) {
                  achievements.add(parts[1].trim());
                }
              }
            }
            map.put(Long.parseLong(appDir.getName()), achievements);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return map;
  }
}
