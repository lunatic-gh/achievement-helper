package de.chloedev.achievementhelper.io;

import de.chloedev.achievementhelper.util.Logger;
import de.chloedev.achievementhelper.util.Util;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class Configuration {

  private static final Configuration INSTANCE = new Configuration();
  private final File file;
  private JSONObject obj;

  Configuration() {
    this.file = new File(Util.getStorageDirectory(), "config.json");
    this.obj = new JSONObject();
  }

  public static Configuration getInstance() {
    return INSTANCE;
  }

  public void load() {
    try {
      this.obj = new JSONObject(Files.readString(this.file.toPath()));
    } catch (Exception e) {
      Logger.error(e);
      this.obj = new JSONObject();
    }
  }

  public void save() {
    try {
      Files.writeString(this.file.toPath(), this.obj.toString(2));
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  public <T> T get(String key, T defaultValue) {
    try {
      if (this.has(key)) {
        //noinspection unchecked
        return (T) this.obj.opt(key);
      }
    } catch (ClassCastException e) {
    }
    return defaultValue;
  }

  public void set(String key, Object value) {
    this.obj.put(key, value);
    this.save();
  }

  public boolean has(String key) {
    return this.obj.has(key);
  }

  public void remove(String key) {
    this.obj.remove(key);
  }

}
