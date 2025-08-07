package de.chloedev.achievementhelper.ui;

import de.chloedev.achievementhelper.Main;
import de.chloedev.achievementhelper.ui.option.Option;
import de.chloedev.achievementhelper.ui.option.Options;
import de.chloedev.achievementhelper.util.Util;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class SettingsScene extends Scene {

  private final ListView<String> sidebarList;
  private final VBox content;
  private final Button backButton;

  public SettingsScene() {
    super(new BorderPane());

    BorderPane root = (BorderPane) getRoot();

    VBox sidebar = new VBox(8);
    sidebar.setPadding(new Insets(8));
    sidebar.setPrefWidth(200);

    backButton = new Button("Back");
    backButton.setStyle("-fx-background-radius: 0px;");
    backButton.setMaxWidth(Double.MAX_VALUE);
    backButton.setOnAction(e -> Main.getInstance().setCurrentScene(new MainScene()));

    sidebarList = new ListView<>();
    sidebarList.setFixedCellSize(30);
    sidebarList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    VBox.setVgrow(sidebarList, Priority.ALWAYS);

    sidebar.getChildren().setAll(backButton, sidebarList);
    root.setLeft(sidebar);

    Map<String, List<Option<?>>> options = Map.of("General", Options.GENERAL_OPTIONS, "Watcher", Options.WATCHER_OPTIONS);

    for (String cat : options.keySet()) {
      this.sidebarList.getItems().add(cat);
    }

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

    sidebarList.getSelectionModel().selectedItemProperty().addListener((obs, oldCat, newCat) -> {
      container.getChildren().clear();
      options.getOrDefault(newCat, List.of()).forEach(opt -> {
        Node node = opt.createNode();
        node.setStyle("-fx-border-color: #444; -fx-border-radius: 8; -fx-padding: 16;");
        container.getChildren().add(node);
      });
    });

    this.content.getChildren().addAll(Util.createHorizontalPlaceholder(32), scrollPane);
    root.setCenter(this.content);
  }
}
