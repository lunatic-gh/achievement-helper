package de.chloedev.achievementhelper.io;

import de.chloedev.achievementhelper.Main;
import de.chloedev.achievementhelper.impl.Achievement;
import de.chloedev.achievementhelper.impl.Game;
import de.chloedev.achievementhelper.steam.Steam;
import de.chloedev.achievementhelper.ui.MainScene;
import de.chloedev.achievementhelper.util.Util;
import de.chloedev.achievementhelper.watcher.AchievementWatcher;
import in.dragonbra.javasteam.steam.handlers.steamapps.PICSProductInfo;
import in.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest;
import in.dragonbra.javasteam.steam.handlers.steamapps.SteamApps;
import in.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSProductInfoCallback;
import in.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats;
import in.dragonbra.javasteam.steam.handlers.steamuserstats.callback.UserStatsCallback;
import in.dragonbra.javasteam.types.AsyncJobMultiple;
import in.dragonbra.javasteam.types.KeyValue;
import in.dragonbra.javasteam.types.SteamID;
import in.dragonbra.javasteam.util.Strings;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameDataStorage {

  private static final GameDataStorage INSTANCE = new GameDataStorage();
  private final File file;
  private final List<Game> games;

  GameDataStorage() {
    this.file = new File(Util.getCacheDirectory(), "localGameData.json");
    this.games = new ArrayList<>();
  }

  public static GameDataStorage getInstance() {
    return INSTANCE;
  }

  public void load() {
    this.games.clear();
    try {
      String content = Files.readString(this.file.toPath());
      JSONArray root = new JSONArray(content);

      for (Object obj : root) {
        JSONObject gameObj = (JSONObject) obj;

        long appId = gameObj.getLong("appId");
        String name = gameObj.getString("name");

        List<Achievement> achievements = new ArrayList<>();
        for (Object achObj : gameObj.optJSONArray("achievements")) {
          achievements.add(Achievement.fromString(achObj.toString()));
        }

        Game game = new Game(name, appId, achievements);
        this.games.add(game);
      }
    } catch (NoSuchFileException ignored) {
      // File doesn't exist — do nothing
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void save() {
    try {
      JSONArray root = new JSONArray();
      for (Game game : this.games) {
        JSONObject gameObj = new JSONObject();
        gameObj.put("appId", game.getAppId());
        gameObj.put("name", game.getName());

        JSONArray achievementsArr = new JSONArray();
        for (Achievement achievement : game.getAchievements()) {
          achievementsArr.put(achievement.toJson());
        }
        gameObj.put("achievements", achievementsArr);

        root.put(gameObj);
      }
      Files.writeString(this.file.toPath(), root.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void retrieveAndAddGame(long appId) {
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request = HttpRequest.newBuilder()
        // TODO: Research if "all" or "updated" have more reliable results.
        .uri(URI.create("https://store.steampowered.com/appreviews/%s?json=1&filter=all&day_range=365&review_type=all&num_per_page=100".formatted(appId))).build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        JSONObject obj = new JSONObject(response.body());
        if (obj.optInt("success") == 1) {
          JSONArray reviews = obj.optJSONArray("reviews", new JSONArray());
          for (Object reviewObj : reviews) {
            JSONObject review = (JSONObject) reviewObj;
            JSONObject author = review.optJSONObject("author", new JSONObject());
            if (author.optInt("num_games_owned") < 1) {
              continue;
            }
            if (author.has("steamid")) {
              SteamID steamId = new SteamID(author.optLong("steamid"));
              SteamUserStats userStatsHandler = Steam.getInstance().getClient().getHandler(SteamUserStats.class);
              if (userStatsHandler != null) {
                UserStatsCallback result = userStatsHandler.getUserStats(Math.toIntExact(appId), steamId).runBlock();
                KeyValue schema = result.getSchemaKeyValues();

                JSONObject apps = new JSONObject(Files.readString(new File(Util.getCacheDirectory(), "storeApps.json").toPath()));
                String gameName = apps.optString(Long.toString(appId), "");
                if (gameName.isEmpty()) {
                  return;
                }
                apps.clear(); // Hopefully frees some memory, since storeApps is HUGE.

                List<Achievement> achievements = new ArrayList<>();
                KeyValue statsObj = schema.get("stats");
                if (statsObj != KeyValue.INVALID) {
                  for (KeyValue statObj : statsObj.getChildren()) {
                    int stat = Integer.parseInt(statObj.getName());
                    KeyValue bitsObj = statObj.get("bits");
                    if (bitsObj != KeyValue.INVALID) {
                      for (KeyValue bitObj : bitsObj.getChildren()) {
                        KeyValue displayObj = bitObj.get("display");
                        KeyValue progressObj = bitObj.get("progress");
                        if (displayObj != KeyValue.INVALID) {
                          KeyValue achIdObj = bitObj.get("name");
                          KeyValue achNameObj = displayObj.get("name");
                          KeyValue achDescriptionObj = displayObj.get("desc");
                          KeyValue achIconObj = displayObj.get("icon");
                          KeyValue achIconLockedObj = displayObj.get("icon_gray");
                          KeyValue achBitObj = bitObj.get("bit");
                          if (achIdObj != KeyValue.INVALID && achNameObj != KeyValue.INVALID && achDescriptionObj != KeyValue.INVALID && achIconObj != KeyValue.INVALID && achIconLockedObj != KeyValue.INVALID && achBitObj != KeyValue.INVALID) {
                            String achId = achIdObj.getValue();
                            String achName = achNameObj.get("english") != KeyValue.INVALID ? achNameObj.get("english").getValue() : "";
                            String achDescription = achDescriptionObj.get("english") != KeyValue.INVALID ? achDescriptionObj.get("english").getValue() : "";
                            String achIcon = achIconObj.getValue();
                            String achIconLocked = achIconLockedObj.getValue();
                            int achMaxProgress = 0;
                            String achStatId = "";
                            int achBit = achBitObj.asInteger();
                            if (progressObj != KeyValue.INVALID) {
                              KeyValue maxValObj = progressObj.get("max_val");
                              KeyValue valueObj = progressObj.get("value");
                              if (maxValObj != KeyValue.INVALID) {
                                achMaxProgress = maxValObj.asInteger();
                              }
                              if (valueObj != KeyValue.INVALID) {
                                KeyValue operandObj = valueObj.get("operand1");
                                if (operandObj != KeyValue.INVALID) {
                                  achStatId = operandObj.getValue();
                                }
                              }
                            }
                            if (!Strings.isNullOrEmpty(achId) && !Strings.isNullOrEmpty(achName) && !Strings.isNullOrEmpty(achDescription)) {
                              Achievement achOld = this.getAchievementById(appId, achId);
                              Achievement achievement = new Achievement(achId, achName, achDescription, achIcon, achIconLocked, stat, achBit, achStatId, achMaxProgress, achOld != null && achOld.isAchieved());
                              achievements.add(achievement);
                            }
                          }
                        }
                      }
                    }
                  }
                }
                this.downloadIcons(appId, achievements);
                Game game = new Game(gameName, appId, achievements);
                if (this.games.stream().anyMatch(g -> g.getAppId() == appId)) {
                  this.games.replaceAll(g -> g.getAppId() == appId ? game : g);
                } else {
                  this.games.add(game);
                }
                this.save();
                Platform.runLater(() -> {
                  if (Main.getInstance().getCurrentScene() instanceof MainScene scene) {
                    scene.updateGameList();
                  }
                });
                AchievementWatcher watcher = Main.getInstance().getAchievementWatcher();
                if (watcher != null) {
                  watcher.scanAll();
                }
                return;
              }
              return;
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void downloadIcons(long appId, List<Achievement> achievements) {
    String appIconHash = "";
    int appIdInt = Math.toIntExact(appId);
    SteamApps appsHandler = Steam.getInstance().getClient().getHandler(SteamApps.class);
    assert appsHandler != null;
    AsyncJobMultiple.ResultSet<PICSProductInfoCallback> result = appsHandler.picsGetProductInfo(new PICSRequest(appIdInt), null).runBlock();
    for (PICSProductInfoCallback callback : result.getResults()) {
      PICSProductInfo info = callback.getApps().get(appIdInt);
      if (info != null) {
        KeyValue common = info.getKeyValues().get("common");
        if (common != KeyValue.INVALID) {
          appIconHash = common.get("icon").getValue();
          break;
        }
      }
    }
    // Download app icon
    if (!appIconHash.isEmpty()) {
      String url = "https://cdn.fastly.steamstatic.com/steamcommunity/public/images/apps/%s/%s.jpg".formatted(appId, appIconHash);
      try {
        File iconDir = new File(Util.getCacheDirectory(), "icons/%s".formatted(appId)).getAbsoluteFile();
        if (iconDir.exists() || iconDir.mkdirs()) {
          ReadableByteChannel channel = Channels.newChannel(URI.create(url).toURL().openStream());
          FileOutputStream out = new FileOutputStream(new File(iconDir, "game.jpg"));
          out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
          channel.close();
          out.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    // Download achievement icons
    String[] urls = {"https://cdn.akamai.steamstatic.com/steamcommunity/public/images/apps", "https://cdn.cloudflare.steamstatic.com/steamcommunity/public/images/apps"};
    achievements.forEach(achievement -> {
      String[] icons = {achievement.getIcon(), achievement.getIconLocked()};
      for (String icon : icons) {
        for (String url : urls) {
          try {
            String finalUrl = "%s/%s/%s".formatted(url, appId, icon);
            File iconDir = new File(Util.getCacheDirectory(), "icons/%s".formatted(appId)).getAbsoluteFile();
            ReadableByteChannel channel = Channels.newChannel(URI.create(finalUrl).toURL().openStream());
            FileOutputStream out = new FileOutputStream(new File(iconDir, "%s".formatted(icon)));
            out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            channel.close();
            out.close();
            // Wait 50ms after each icon.
            Util.waitUntil(() -> false, 50, null, null);
            System.out.println("Downloaded icon " + icon);
            break;
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    });
  }

  public List<Achievement> getAchievementsForGame(long appId) {
    return this.games.stream().filter(g -> g.getAppId() == appId).findFirst().map(Game::getAchievements).orElse(Collections.emptyList());
  }

  public Achievement getAchievementById(long appId, String achievementId) {
    Game game = this.games.stream().filter(g -> g.getAppId() == appId).findFirst().orElse(null);
    return game != null ? game.getAchievements().stream().filter(ach -> ach.getId().equals(achievementId)).findFirst().orElse(null) : null;
  }

  public List<Game> getGames() {
    return games;
  }
}
