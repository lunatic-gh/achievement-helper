package de.chloedev.achievementhelper.impl;

import de.chloedev.achievementhelper.util.Logger;
import de.chloedev.achievementhelper.util.Util;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;

public final class Achievement {
  private final String id;
  private final String name;
  private final String description;
  private final String icon;
  private final String iconLocked;
  private final int stat;
  private final int bit;
  private final String statId;
  private final int maxProgress;
  private boolean achieved;

  public Achievement(String id, String name, String description, String icon, String iconLocked, int stat, int bit, String statId, int maxProgress, boolean achieved) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.icon = icon;
    this.iconLocked = iconLocked;
    this.stat = stat;
    this.bit = bit;
    this.statId = statId;
    this.maxProgress = maxProgress;
    this.achieved = achieved;
  }

  public static Achievement fromString(String jsonStr) {
    try {
      JSONObject obj = new JSONObject(jsonStr);
      return new Achievement(obj.getString("id"), obj.getString("name"), obj.getString("description"), obj.getString("icon"), obj.getString("iconLocked"), obj.getInt("stat"), obj.getInt("bit"), obj.getString("statId"), obj.getInt("maxProgress"), obj.getBoolean("achieved"));
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
    obj.put("id", id);
    obj.put("name", name);
    obj.put("description", description);
    obj.put("icon", icon);
    obj.put("iconLocked", iconLocked);
    obj.put("stat", stat);
    obj.put("bit", bit);
    obj.put("statId", statId);
    obj.put("maxProgress", maxProgress);
    obj.put("achieved", achieved);
    return obj;
  }

  @Nullable
  public Image createAchievementIcon(long appId) {
    try {
      return new Image(new File(Util.getCacheDirectory(), "icons/%s/%s".formatted(appId, this.achieved ? this.icon : this.iconLocked)).toURI().toURL().toExternalForm());
    } catch (Exception e) {
      Logger.error(e);
      return null;
    }
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getIcon() {
    return icon;
  }

  public String getIconLocked() {
    return iconLocked;
  }

  public int getStat() {
    return stat;
  }

  public int getBit() {
    return bit;
  }

  public String getStatId() {
    return statId;
  }

  public int getMaxProgress() {
    return maxProgress;
  }

  public boolean isAchieved() {
    return achieved;
  }

  public void setAchieved(boolean achieved) {
    this.achieved = achieved;
  }
}
