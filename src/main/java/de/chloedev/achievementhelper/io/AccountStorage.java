package de.chloedev.achievementhelper.io;

import de.chloedev.achievementhelper.steam.account.Account;
import de.chloedev.achievementhelper.util.Util;
import in.dragonbra.javasteam.types.SteamID;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

public class AccountStorage {

  private static final AccountStorage INSTANCE = new AccountStorage();
  private final File file;
  private Account account;

  AccountStorage() {
    this.file = new File(Util.getStorageDirectory(), "account.json");
    this.account = null;
  }

  public static AccountStorage getInstance() {
    return INSTANCE;
  }

  public void load() {
    try {
      JSONObject obj = new JSONObject(Files.readString(this.file.toPath()));
      if (obj.has("username") && obj.has("refreshToken") && obj.has("steamId")) {
        this.account = new Account(obj.getString("username"), obj.getString("refreshToken"), new SteamID(obj.getLong("steamId")), obj.optString("steamGuardData", ""));
        return;
      }
    } catch (FileNotFoundException | NoSuchFileException ígnored) {
      // IGNORED
    } catch (Exception e) {
      e.printStackTrace();
    }
    this.account = null;
  }

  public void save() {
    try {
      JSONObject obj = new JSONObject();
      obj.put("username", this.account.getUsername());
      obj.put("refreshToken", this.account.getRefreshToken());
      obj.put("steamId", this.account.getSteamId().convertToUInt64());
      obj.put("steamGuardData", this.account.getSteamGuardData());
      Files.writeString(this.file.toPath(), obj.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }
}
