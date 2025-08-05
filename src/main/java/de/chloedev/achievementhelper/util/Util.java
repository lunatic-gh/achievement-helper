package de.chloedev.achievementhelper.util;

import de.chloedev.achievementhelper.impl.Achievement;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Optional;
import java.util.function.Supplier;

public class Util {

  public static File getStorageDirectory() {
    try {
      String appData = System.getenv("APPDATA");
      if (appData != null && !appData.isEmpty()) {
        File f = new File(appData, "achievement-helper").getAbsoluteFile();
        if (f.exists() || f.mkdirs()) {
          return f;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static File getCacheDirectory() {
    try {
      File f = new File(getStorageDirectory(), ".cache").getAbsoluteFile();
      if (f.exists() || f.mkdirs()) {
        return f;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void waitUntil(Supplier<Boolean> condition, long timeoutMs, @Nullable Runnable onCompleted, @Nullable Runnable onTimeout) {
    long l = System.currentTimeMillis();
    while (true) {
      if (condition.get()) {
        if (onCompleted != null) {
          onCompleted.run();
        }
        return;
      }
      if (timeoutMs != -1 && (System.currentTimeMillis() - l) > timeoutMs) {
        if (onTimeout != null) {
          onTimeout.run();
        }
        return;
      }
      try {
        //noinspection BusyWait - This is fine... probably...
        Thread.sleep(1);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public static Region createHorizontalPlaceholder(double height) {
    Region placeholder = new Region();
    placeholder.setMinHeight(height);
    placeholder.setMaxHeight(height);
    placeholder.setPrefHeight(height);
    return placeholder;
  }

  @Nullable
  public static String showInputDialog(String title, String prompt, String fieldName, boolean allowEmpty) {
    Dialog<String> dialog = new Dialog<>();
    dialog.setTitle(title);
    dialog.setHeaderText(prompt);
    ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
    TextField inputField = new TextField();
    inputField.setPromptText(fieldName);
    VBox content = new VBox(10);
    content.getChildren().add(inputField);
    dialog.getDialogPane().setContent(content);
    Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
    okButton.setDisable(!allowEmpty);
    inputField.textProperty().addListener((obs, oldVal, newVal) -> {
      okButton.setDisable((!allowEmpty && newVal.trim().isEmpty()) || !newVal.matches("\\d+"));
    });
    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == okButtonType) {
        return inputField.getText();
      }
      return null;
    });
    String result = dialog.showAndWait().orElse("");
    return result.isEmpty() ? null : result;
  }

  public static Stage showProgressDialog(String title, String description) {
    VBox box = new VBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20));
    Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    Label descLabel = new Label(description);
    ProgressIndicator progressIndicator = new ProgressIndicator();
    box.getChildren().addAll(titleLabel, descLabel, progressIndicator);
    Stage stage = new Stage();
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setTitle(title);
    stage.setScene(new Scene(box));
    stage.setResizable(false);
    stage.setAlwaysOnTop(true);
    stage.setOnCloseRequest(Event::consume);
    stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        event.consume();
      }
    });
    stage.show();
    return stage;
  }

  @NotNull
  public static Pair<String, String> showLoginDialog() {
    Dialog<Pair<String, String>> dialog = new Dialog<>();
    dialog.setTitle("Login");
    dialog.setHeaderText("Please enter your username and password:");
    ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));
    TextField usernameField = new TextField();
    usernameField.setPromptText("Username");
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Password");
    grid.add(new Label("Username"), 0, 0);
    grid.add(usernameField, 1, 0);
    grid.add(new Label("Password"), 0, 1);
    grid.add(passwordField, 1, 1);
    dialog.getDialogPane().setContent(grid);
    Platform.runLater(usernameField::requestFocus);
    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == loginButtonType) {
        return Pair.of(usernameField.getText(), passwordField.getText());
      }
      return null;
    });
    Optional<Pair<String, String>> result = dialog.showAndWait();
    return result.orElse(Pair.of("", ""));
  }

  public static void showMessageDialog(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.show();
  }

  /**
   * Returns a list of appid>name pairs of all apps registered on the steam store.
   * Note that this includes EVERYTHING, e.g. the steam client itself or apps like "Source SDK"
   */
  public static void downloadAllSteamApps() {
    try (HttpClient client = HttpClient.newHttpClient()) {
      JSONObject finalObj = new JSONObject();
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.steampowered.com/ISteamApps/GetAppList/v2")).build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        JSONObject obj = new JSONObject(response.body());
        if (obj.has("applist")) {
          JSONObject appsObj = obj.getJSONObject("applist");
          JSONArray apps = appsObj.optJSONArray("apps", new JSONArray());
          apps.forEach(o -> {
            JSONObject app = (JSONObject) o;
            long appId = app.optLong("appid", 0L);
            String appIdStr = Long.toString(appId);
            String appName = app.optString("name", "");
            if (appId != 0L && !appName.isEmpty()) {
              if (!finalObj.has(appIdStr)) {
                finalObj.put(appIdStr, appName);
              }
            }
          });
        }
      }
      Files.writeString(new File(Util.getCacheDirectory(), "storeApps.json").toPath(), finalObj.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static String winReadRegistryKey(String location, String key) {
    try {
      Process process = Runtime.getRuntime().exec(new String[]{"reg", "query", location, "/v", key});
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.contains(key)) {
            return line.split("\\s{4,}")[line.split("\\s{4,}").length - 1].trim();
          }
        }
      }
    } catch (Exception e) {
    }
    return null;
  }

  public static void showAchievementNotification(Long appId, Achievement achievement) {
    Platform.runLater(() -> {
      ImageView view = null;
      Image img = achievement.createAchievementIcon(appId);
      if (img != null) {
        view = new ImageView(img);
        view.setFitWidth(64);
        view.setFitHeight(64);
      }

      String name = achievement.getName();
      String description = achievement.getDescription();

      VBox textBox = new VBox(new Label(name), new Label(description));
      textBox.setSpacing(4);
      textBox.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

      HBox content = new HBox(view, textBox);
      content.setSpacing(10);
      content.setPadding(new javafx.geometry.Insets(10));
      content.setBackground(new Background(new BackgroundFill(Color.DARKSLATEGRAY, new CornerRadii(5), null)));
      content.setStyle("-fx-effect: dropshadow(gaussian, black, 10, 0, 0, 2);");

      // Create an invisible owner stage
      Stage stage = new Stage();
      stage.initStyle(StageStyle.TRANSPARENT);
      stage.setOpacity(0);
      stage.setAlwaysOnTop(true);
      stage.setWidth(1);
      stage.setHeight(1);
      stage.setX(-10000); // Move offscreen
      stage.setY(-10000);
      stage.setScene(new Scene(new Pane(), Color.TRANSPARENT));
      stage.show();

      // Create popup and position it
      Popup popup = new Popup();
      popup.getContent().add(content);
      popup.setAutoHide(true);

      Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
      double x = screenBounds.getMaxX() - 300;
      double y = screenBounds.getMaxY() - 100;

      popup.show(stage, x, y);

      FadeTransition fade = new FadeTransition(Duration.seconds(3), content);
      fade.setFromValue(1.0);
      fade.setToValue(0.0);
      fade.setOnFinished(e -> {
        popup.hide();
        stage.close();
      });

      PauseTransition pause = new PauseTransition(Duration.seconds(5));
      pause.setOnFinished(event -> fade.play());
      pause.play();
    });
  }
}
