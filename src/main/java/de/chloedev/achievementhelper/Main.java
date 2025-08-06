package de.chloedev.achievementhelper;

import atlantafx.base.theme.CupertinoDark;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import de.chloedev.achievementhelper.io.AccountStorage;
import de.chloedev.achievementhelper.io.Configuration;
import de.chloedev.achievementhelper.io.GameDataStorage;
import de.chloedev.achievementhelper.steam.Steam;
import de.chloedev.achievementhelper.ui.MainScene;
import de.chloedev.achievementhelper.util.Util;
import de.chloedev.achievementhelper.watcher.AchievementWatcher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class Main extends Application {

  private static Main INSTANCE;
  private Stage stage;
  private Configuration config;
  private File pyResources;
  private AchievementWatcher achievementWatcher;
  private boolean running;

  public static void main(String[] args) {
    launch();
  }

  public static Main getInstance() {
    return INSTANCE;
  }

  @Override
  public void start(Stage stage) {
    INSTANCE = this;
    this.stage = stage;
    // We need tray support, otherwise JavaFX will auto-close the program when the last window is hidden or closed.
    if (!FXTrayIcon.isSupported()) {
      throw new UnsupportedOperationException("System Tray is not supported on your Platform.");
    }

    this.pyResources = Util.extractPyResources();

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

    Steam steam = Steam.getInstance();
    Platform.runLater(() -> {
      try {
        if (!steam.ensureAuthenticated()) {
          Alert alert = new Alert(Alert.AlertType.ERROR);
          alert.setTitle("Error");
          alert.setHeaderText(null);
          alert.setContentText("Could not login to steam. Without a steam login, this program will not work!");
          alert.showAndWait();
          System.exit(1);
        } else {
          this.running = true;
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

          this.achievementWatcher = new AchievementWatcher();

          CompletableFuture.runAsync(this.achievementWatcher::init);

          // Refresh the account token every 5 minutes if required
          Timer timer = new Timer();
          timer.schedule(new TimerTask() {

            @Override
            public void run() {
              if (!Main.this.running){
                return;
              }
              Platform.runLater(() -> {
                if (steam.ensureAuthenticated()) {
                  AccountStorage.getInstance().getAccount().renewToken();
                } else {
                  Main.this.running = false;
                  Alert alert = new Alert(Alert.AlertType.ERROR);
                  alert.setTitle("Error");
                  alert.setHeaderText(null);
                  alert.setContentText("Could not login to steam. Without a steam login, this program will not work!");
                  alert.showAndWait();
                  System.exit(1);
                }
              });
            }
          }, 1000L, 60000L);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  public Stage getStage() {
    return stage;
  }

  public File getPyResources() {
    return pyResources;
  }

  public Scene getCurrentScene() {
    return this.stage.getScene();
  }

  public void setCurrentScene(Scene scene) {
    this.stage.setScene(scene);
  }

  public AchievementWatcher getAchievementWatcher() {
    return achievementWatcher;
  }
}
