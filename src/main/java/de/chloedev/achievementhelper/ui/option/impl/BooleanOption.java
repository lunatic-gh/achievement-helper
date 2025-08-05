package de.chloedev.achievementhelper.ui.option.impl;

import de.chloedev.achievementhelper.ui.option.Option;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class BooleanOption extends Option<Boolean> {
  public BooleanOption(String id, String title) {
    super(id, title);
  }

  @Override
  public Node createNode() {
    HBox box = new HBox();
    box.setAlignment(Pos.CENTER);

    Label label = new Label(this.name);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    CheckBox checkBox = new CheckBox();
    checkBox.setSelected(this.getter.get());
    checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> setter.accept(newValue));

    box.getChildren().addAll(label, spacer, checkBox);

    return box;
  }
}
