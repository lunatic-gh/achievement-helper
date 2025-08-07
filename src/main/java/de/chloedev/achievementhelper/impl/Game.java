package de.chloedev.achievementhelper.impl;

import de.chloedev.achievementhelper.util.Logger;
import de.chloedev.achievementhelper.util.Util;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public final class Game {
  private final String name;
  private final long appId;
  private final List<Achievement> achievements;

  public Game(String name, long appId, List<Achievement> achievements) {
    this.name = name;
    this.appId = appId;
    this.achievements = achievements;
  }

  public static Game fromString(String jsonStr) {
    try {
      JSONObject obj = new JSONObject(jsonStr);
      return new Game(obj.getString("name"), obj.getLong("appId"), obj.optJSONArray("achievements", new JSONArray()).toList().stream().map(o -> (Achievement) o).toList());
    } catch (Exception e) {
      Logger.error(e);
      return null;
    }
  }

  @Override
  public @NotNull String toString() {
    return this.toJson().toString(0);
  }

  public @NotNull JSONObject toJson() {
    JSONObject obj = new JSONObject();
    obj.put("name", this.name);
    obj.put("appId", this.appId);
    obj.put("achievements", new JSONArray(this.achievements));
    return obj;
  }

  @Nullable
  public Image createAppIcon() {
    try {
      return new Image(new File(Util.getCacheDirectory(), "icons/%s/game.jpg".formatted(this.appId)).toURI().toURL().toExternalForm());
    } catch (Exception e) {
      Logger.error(e);
      return null;
    }
  }

  public String getName() {
    return name;
  }

  public long getAppId() {
    return appId;
  }

  public List<Achievement> getAchievements() {
    return achievements;
  }
}
