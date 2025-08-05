package de.chloedev.achievementhelper.steam;

import de.chloedev.achievementhelper.util.Util;
import in.dragonbra.javasteam.steam.authentication.IAuthenticator;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class SteamAuthenticator implements IAuthenticator {

  private boolean cancelled;

  private Dialog<ButtonType> acceptDialog;

  @Override
  public @NotNull CompletableFuture<String> getDeviceCode(boolean prevFailed) {
    return CompletableFuture.supplyAsync(() -> {
      // TODO: Implement
      return "";
    });
  }

  @Override
  public @NotNull CompletableFuture<String> getEmailCode(@Nullable String address, boolean prevFailed) {
    return CompletableFuture.supplyAsync(() -> {
      // TODO: Implement
      return "";
    });
  }

  @Override
  public @NotNull CompletableFuture<Boolean> acceptDeviceConfirmation() {
    return CompletableFuture.supplyAsync(() -> {
      Platform.runLater(() -> {
        this.acceptDialog = new Alert(Alert.AlertType.NONE);
        this.acceptDialog.setTitle("Steam Guard");
        this.acceptDialog.setHeaderText(null);
        this.acceptDialog.setContentText("Please confirm the login in your Steam Mobile App.");
        this.acceptDialog.setOnShown(e -> {
          Stage s = (Stage) this.acceptDialog.getDialogPane().getScene().getWindow();
          s.setOnCloseRequest(Event::consume);
          s.centerOnScreen();
          s.setAlwaysOnTop(true);
          s.toFront();
          s.requestFocus();
        });
        this.acceptDialog.show();
      });
      return true;
    });
  }

  public void cleanup() {
    Platform.runLater(() -> {
      Stage stage = Util.getOrDefault((Stage) this.acceptDialog.getDialogPane().getScene().getWindow(), null);
      if (stage != null) {
        stage.close();
      }
    });
  }
}
