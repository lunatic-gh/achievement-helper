package de.chloedev.achievementhelper.ui;

import de.chloedev.achievementhelper.Main;
import de.chloedev.achievementhelper.impl.Game;
import de.chloedev.achievementhelper.io.GameDataStorage;
import de.chloedev.achievementhelper.steam.Steam;
import de.chloedev.achievementhelper.util.Util;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainScene extends Scene {

  private final ListView<Game> sidebarList;
  private final VBox content;
  private final Button addGameButton;
  private final Button settingsButton;

  public MainScene() {
    super(new BorderPane());
    BorderPane root = (BorderPane) getRoot();
    VBox sidebar = new VBox(8);
    sidebar.setPadding(new Insets(8));
    sidebar.setPrefWidth(200);
    this.addGameButton = new Button("Add Game Manually");
    this.addGameButton.setStyle("-fx-background-radius: 0px;");
    this.addGameButton.setMaxWidth(Double.MAX_VALUE);
    this.addGameButton.setOnAction(e -> {
      if (!Steam.getInstance().ensureAuthenticated()) {
        Util.showMessageDialog("Error", "Could not authenticate, cancelling...");
      }
      String appId = Util.showInputDialog("Add Game Manually", "Please enter the Steam AppID of the game you want to add. You can find it in the URL of the Game's Steam Store-Page.", "AppID", false);
      if (appId == null) {
        return;
      }
      if (!appId.matches("\\d+")) {
        Util.showMessageDialog("Warning", "Invalid AppID provided, cancelling...");
        return;
      }
      Stage progressStage = Util.showProgressDialog("Retrieving Game Data", "Currently retrieving Game Data, Please wait...");
      CompletableFuture.runAsync(() -> {
        GameDataStorage.getInstance().retrieveAndAddGame(Long.parseLong(appId));
        Platform.runLater(progressStage::close);
      });
    });
    this.settingsButton = new Button("Settings");
    this.settingsButton.setStyle("-fx-background-radius: 0px;");
    settingsButton.setMaxWidth(Double.MAX_VALUE);
    settingsButton.setOnAction(event -> Main.getInstance().setCurrentScene(new SettingsScene()));
    this.sidebarList = new ListView<>();
    this.sidebarList.setFixedCellSize(30);
    this.sidebarList.setItems(FXCollections.observableArrayList());
    this.sidebarList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    this.sidebarList.setMaxHeight(Double.MAX_VALUE);
    VBox.setVgrow(this.sidebarList, Priority.ALWAYS);
    // this.sidebarList.getSelectionModel().selectFirst();
    this.sidebarList.skinProperty().addListener((obs, oldSkin, newSkin) -> {
      if (newSkin instanceof ListViewSkin<?> skin) {
        VirtualFlow<?> flow = (VirtualFlow<?>) skin.getChildren().getFirst();
        flow.getChildrenUnmodifiable().stream().filter(child -> child instanceof ScrollBar).forEach(scrollBar -> {
          ((ScrollBar) scrollBar).setPrefSize(0, 0);
        });
        flow.requestLayout();
      }
    });
    this.sidebarList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(Game game, boolean empty) {
        super.updateItem(game, empty);
        if (empty || game == null) {
          setText(null);
        } else {
          setText(game.getName());
          ImageView img = new ImageView(game.createAppIcon());
          img.setFitWidth(24);
          img.setFitHeight(24);
          setGraphic(img);
        }
      }
    });
    sidebar.getChildren().setAll(addGameButton, sidebarList, settingsButton);
    root.setLeft(sidebar);

    this.updateGameList();

    this.content = new VBox(8);
    this.content.setPadding(new Insets(8));
    this.content.setFillWidth(true);

    VBox container = new VBox(8);
    container.setPadding(new Insets(8));

    ScrollPane scrollPane = new ScrollPane(container);
    scrollPane.setFitToWidth(true);
    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scrollPane.setStyle("-fx-background-color: transparent;");
    VBox.setVgrow(scrollPane, Priority.ALWAYS);

    sidebarList.getSelectionModel().selectedItemProperty().addListener((obs, oldGame, newGame) -> {
      container.getChildren().clear();
      scrollPane.setVvalue(0);
      if (newGame == null) {
        return;
      }
      for (var achievement : newGame.getAchievements()) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-border-color: #444; " + "-fx-border-radius: 8; " + "-fx-padding: 16;");

        ImageView icon = new ImageView(achievement.createAchievementIcon(newGame.getAppId()));
        icon.setFitWidth(64);
        icon.setFitHeight(64);
        icon.setPreserveRatio(true);

        if (!achievement.isAchieved()) {
          ColorAdjust adjust = new ColorAdjust();
          adjust.setSaturation(-1.0);
          adjust.setBrightness(-0.75);
          icon.setEffect(adjust);
        }

        Label nameLabel = new Label(achievement.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label descLabel = new Label(achievement.getDescription());
        descLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        descLabel.setWrapText(true);

        VBox textBox = new VBox(4, nameLabel, descLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean completed = achievement.isAchieved();
        Label statusLabel = new Label(completed ? "Completed" : "Locked");
        statusLabel.setAlignment(Pos.CENTER_RIGHT);
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        statusLabel.setTextFill(completed ? Color.LIMEGREEN : Color.DARKGRAY);

        box.getChildren().addAll(icon, textBox, spacer, statusLabel);
        container.getChildren().add(box);
      }
    });

    this.content.getChildren().addAll(Util.createHorizontalPlaceholder(32), scrollPane);
    root.setCenter(this.content);
  }

  public void addGame(Game game) {
    ObservableList<Game> items = this.sidebarList.getItems();
    items.add(game);
    this.sidebarList.setItems(items);
  }

  public void setGames(List<Game> games) {
    ObservableList<Game> items = this.sidebarList.getItems();
    items.setAll(games);
    this.sidebarList.setItems(items);
  }

  public void updateGameList() {
    this.setGames(GameDataStorage.getInstance().getGames());
  }

  public Button getAddGameButton() {
    return addGameButton;
  }

  public Button getSettingsButton() {
    return settingsButton;
  }
}
