package de.chloedev.achievementhelper;

/**
 * This is a workaround for javafx being stupid on non-modular projects.
 * when using the Main class directly, it'll complain about missing runtime components.
 * <p>
 * Thank you javafx for being such a piece of garbage.
 */
public class FXLauncher {
  public static void main(String[] args) {
    Main.main(args);
  }
}
