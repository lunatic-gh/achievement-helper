package de.chloedev.achievementhelper.ui.option;

import de.chloedev.achievementhelper.ui.option.impl.BooleanOption;
import de.chloedev.achievementhelper.ui.option.impl.StringListOption;

import java.util.List;

// @formatter:off
public class Options {

  /**
   * General Options
   */
  public static final List<Option<?>> GENERAL_OPTIONS = List.of(
    new BooleanOption("launchMinimized", "Launch Minimized")
  );

  /**
   * Watcher Options
   */
  public static final List<Option<?>> WATCHER_OPTIONS = List.of(
    new StringListOption("watcherPaths", "Watcher Paths (Changes require a restart)")
  );
}
// @formatter:on
