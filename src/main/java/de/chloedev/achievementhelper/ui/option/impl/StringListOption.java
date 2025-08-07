package de.chloedev.achievementhelper.ui.option.impl;

import de.chloedev.achievementhelper.io.Configuration;
import de.chloedev.achievementhelper.ui.option.Option;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.json.JSONArray;

import java.util.List;

public class StringListOption extends Option<List<String>> {

  private final ObservableList<String> values;

  public StringListOption(String id, String name) {
    super(id, name, () -> {
      return Configuration.getInstance().get(id, new JSONArray()).toList().stream().map(String::valueOf).toList();
    }, list -> {
      Configuration.getInstance().set(id, new JSONArray(list));
    });
    this.values = FXCollections.observableArrayList(this.getter.get());
  }

  @Override
  public Node createNode() {
    HBox root = new HBox();
    root.setAlignment(Pos.TOP_LEFT);
    root.setSpacing(5);

    Label label = new Label(this.name);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    VBox valueBox = new VBox(5);
    HBox.setHgrow(valueBox, Priority.ALWAYS);

    ListView<String> listView = new ListView<>(values);
    listView.setFixedCellSize(24);
    listView.setPrefHeight(100);
    listView.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(empty || item == null ? null : item);
      }
    });

    TextField inputField = new TextField();
    inputField.setPromptText("Enter value...");
    inputField.setPrefWidth(120);

    Button addButton = new Button("Add");
    Button removeButton = new Button("Remove Selected");

    addButton.setOnAction(e -> {
      String input = inputField.getText();
      if (input != null && !input.isBlank()) {
        values.add(input);
        inputField.clear();
        setter.accept(List.copyOf(values));
      }
    });

    removeButton.setOnAction(e -> {
      String selected = listView.getSelectionModel().getSelectedItem();
      if (selected != null) {
        values.remove(selected);
        setter.accept(List.copyOf(values));
      }
    });

    HBox controls = new HBox(5, inputField, addButton, removeButton);
    controls.setAlignment(Pos.CENTER_LEFT);

    valueBox.getChildren().addAll(listView, controls);
    root.getChildren().addAll(label, spacer, valueBox);
    return root;
  }
}
