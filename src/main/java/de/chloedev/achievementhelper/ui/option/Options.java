package de.chloedev.achievementhelper.ui.option;

import de.chloedev.achievementhelper.ui.option.impl.BooleanOption;

import java.util.List;

// @formatter:off
public class Options {

  /**
   * General Options
   */
  public static final List<Option<?>> GENERAL_OPTIONS = List.of(
    new BooleanOption("launchMinimized", "Launch Minimized")
  );
}
// @formatter:on
