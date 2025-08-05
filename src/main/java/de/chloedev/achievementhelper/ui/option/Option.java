package de.chloedev.achievementhelper.ui.option;

import de.chloedev.achievementhelper.io.Configuration;
import javafx.scene.Node;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class Option<T> {
  protected final String id;
  protected final String name;
  protected final Supplier<T> getter;
  protected final Consumer<T> setter;

  public Option(String id, String name, Supplier<T> getter, Consumer<T> setter) {
    this.id = id;
    this.name = name;
    this.getter = getter;
    this.setter = setter;
  }

  public Option(String id, String name) {
    this.id = id;
    this.name = name;
    this.getter = () -> Configuration.getInstance().get(id, (T) null);
    this.setter = val -> Configuration.getInstance().set(id, val);
  }

  public abstract Node createNode();

}
