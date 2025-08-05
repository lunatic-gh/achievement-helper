package de.chloedev.achievementhelper;

import atlantafx.base.theme.CupertinoDark;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import de.chloedev.achievementhelper.io.AccountStorage;
import de.chloedev.achievementhelper.io.Configuration;
import de.chloedev.achievementhelper.io.GameDataStorage;
import de.chloedev.achievementhelper.ui.MainScene;
import de.chloedev.achievementhelper.util.Util;
import de.chloedev.achievementhelper.watcher.AchievementWatcher;
import de.chloedev.achievementhelper.watcher.AchievementWatchers;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Main extends Application {

  private static Main INSTANCE;
  private Stage stage;
  private Configuration config;

  public static void main(String[] args) {
    launch();
  }

  public static Main getInstance() {
    return INSTANCE;
  }

  @Override
  public void start(Stage stage) throws IOException {
    INSTANCE = this;
    this.stage = stage;
    // We need tray support, otherwise JavaFX will auto-close the program when the last window is hidden or closed.
    if (!FXTrayIcon.isSupported()) {
      throw new UnsupportedOperationException("System Tray is not supported on your Platform. On Linux, please ensure you are using a supported Desktop Environment.");
    }

    Util.downloadAllSteamApps();

    AccountStorage.getInstance().load();
    Configuration config = Configuration.getInstance();
    config.load();

    Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
    stage.setMinWidth(1280);
    stage.setWidth(1280);
    stage.setMinHeight(720);
    stage.setHeight(720);
    GameDataStorage gameDataStorage = GameDataStorage.getInstance();
    gameDataStorage.load();

    this.setCurrentScene(new MainScene());

    FXTrayIcon tray = new FXTrayIcon(stage, ImageIO.read(Objects.requireNonNull(FXLauncher.class.getResource("/tray_icon.png"))));
    MenuItem showTrayItem = new MenuItem("Show");
    MenuItem minimizeTrayItem = new MenuItem("Minimize (Tray)");
    MenuItem closeTrayItem = new MenuItem("Exit");
    showTrayItem.setOnAction(e -> {
      stage.show();
      stage.toFront();
    });
    minimizeTrayItem.setOnAction(e -> {
      stage.hide();
    });
    closeTrayItem.setOnAction(e -> {
      stage.close();
      System.exit(0);
    });
    tray.addMenuItems(showTrayItem, minimizeTrayItem, new MenuItem("-"), closeTrayItem);
    tray.show();

    if (!config.get("launchMinimized", false)) {
      stage.show();
      stage.toFront();
    }

    for (AchievementWatcher watcher : AchievementWatchers.WATCHERS) {
      CompletableFuture.runAsync(watcher::listen);
    }
  }

  public Stage getStage() {
    return stage;
  }

  public Scene getCurrentScene() {
    return this.stage.getScene();
  }

  public void setCurrentScene(Scene scene) {
    this.stage.setScene(scene);
  }
}
