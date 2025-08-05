package de.chloedev.achievementhelper.ui.option.impl;

import de.chloedev.achievementhelper.ui.option.Option;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class IntegerOption extends Option<Integer> {

  public IntegerOption(String id, String name) {
    super(id, name);
  }

  @Override
  public Node createNode() {
    HBox node = new HBox();
    node.setAlignment(Pos.CENTER);

    Label label = new Label(this.name);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox valueBox = new HBox(5);

    TextField textField = new TextField(String.valueOf(this.getter.get()));
    textField.setPrefWidth(80);

    Button applyButton = new Button("Apply");

    applyButton.setOnAction(e -> {
      String text = textField.getText();
      try {
        int value = Integer.parseInt(text);
        setter.accept(value);
      } catch (NumberFormatException ignored) {
      }
    });

    textField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.matches("-?\\d+")) {
        if (applyButton.isDisabled()) {
          applyButton.setDisable(false);
        }
      } else {
        if (!applyButton.isDisabled()) {
          applyButton.setDisable(true);
        }
      }
    });

    valueBox.getChildren().addAll(textField, applyButton);
    node.getChildren().addAll(label, spacer, valueBox);
    return node;
  }
}
