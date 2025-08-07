package de.chloedev.achievementhelper.steam.account;

import de.chloedev.achievementhelper.io.AccountStorage;
import de.chloedev.achievementhelper.steam.Steam;
import de.chloedev.achievementhelper.util.Logger;
import in.dragonbra.javasteam.steam.authentication.AccessTokenGenerateResult;
import in.dragonbra.javasteam.steam.authentication.SteamAuthentication;
import in.dragonbra.javasteam.types.SteamID;
import in.dragonbra.javasteam.util.Strings;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public class Account {

  private String username;
  private String refreshToken;
  private SteamID steamId;
  private String steamGuardData;
  private Instant lastTokenRefreshTime = Instant.ofEpochMilli(0L);

  public Account(String username, String refreshToken, SteamID steamId, String steamGuardData) {
    this.username = username;
    this.refreshToken = refreshToken;
    this.steamId = steamId;
    this.steamGuardData = steamGuardData;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public SteamID getSteamId() {
    return steamId;
  }

  public void setSteamId(SteamID steamId) {
    this.steamId = steamId;
  }

  public String getSteamGuardData() {
    return steamGuardData;
  }

  public void setSteamGuardData(String steamGuardData) {
    this.steamGuardData = steamGuardData;
  }

  public long getRefreshTokenExpirationTimestampMs() {
    if (Strings.isNullOrEmpty(this.refreshToken)) {
      return 0L;
    }
    String[] components = this.refreshToken.split("\\.");
    String base64 = components[1].replace("-", "+").replace("_", "/");
    if ((base64.length() & 4) != 0) {
      base64 += new String(new char[4 - base64.length() % 4]).replace("\0", "=");
    }
    byte[] payload = Base64.getDecoder().decode(base64);
    JSONObject obj = new JSONObject(new String(payload));
    return (obj.optLong("exp", 0L)) * 1000L;
  }

  public boolean shouldRefreshToken() {
    if (Duration.between(lastTokenRefreshTime, Instant.now()).toSeconds() < 300) {
      Logger.debug("Token was already refreshed in the last 5 minutes.");
      return false;
    }
    try {
      long expirationTimestamp = this.getRefreshTokenExpirationTimestampMs();
      if (System.currentTimeMillis() > expirationTimestamp) {
        // Already expired. Refreshing it would be practically pointless.
        return false;
      }
      // If the token expires in less than 7 days, refresh it.
      if (expirationTimestamp - System.currentTimeMillis() > 604800000) {
        return false;
      }
      return true;
    } catch (Exception e) {
      Logger.error(e);
    }
    return false;
  }

  public void renewToken() {
    if (this.shouldRefreshToken()) {
      try {
        SteamAuthentication auth = new SteamAuthentication(Steam.getInstance().getClient());
        AccessTokenGenerateResult result = auth.generateAccessTokenForApp(this.steamId, this.refreshToken, true).get();
        this.lastTokenRefreshTime = Instant.now();
        Logger.info("Refreshed token!");
        // We don't care about the access token, since it's not used anywhere.
        if (!Strings.isNullOrEmpty(result.getRefreshToken())) {
          this.refreshToken = result.getAccessToken();
          AccountStorage.getInstance().save();
          Logger.info("Saved new token.");
        } else Logger.debug("Didn't receive a new refresh token.");
      } catch (Exception e) {
        Logger.error(e);
      }
    } else {
      Logger.info("Not refreshing token....");
    }
  }
}
