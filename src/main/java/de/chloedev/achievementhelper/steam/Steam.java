package de.chloedev.achievementhelper.steam;

import de.chloedev.achievementhelper.io.AccountStorage;
import de.chloedev.achievementhelper.steam.account.Account;
import de.chloedev.achievementhelper.util.Pair;
import de.chloedev.achievementhelper.util.Util;
import in.dragonbra.javasteam.enums.EResult;
import in.dragonbra.javasteam.steam.authentication.AuthSessionDetails;
import in.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails;
import in.dragonbra.javasteam.steam.handlers.steamuser.SteamUser;
import in.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback;
import in.dragonbra.javasteam.steam.steamclient.SteamClient;
import in.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Steam {

  private static final Steam INSTANCE = new Steam();
  private final SteamClient client;
  private final SteamUser user;
  private final CallbackManager callbackManager;
  private final List<Closeable> callbacks;

  Steam() {
    this.client = new SteamClient();
    this.user = client.getHandler(SteamUser.class);
    this.callbackManager = new CallbackManager(this.client);
    this.callbacks = new ArrayList<>();

    CompletableFuture.runAsync(() -> {
      while (true) {
        this.callbackManager.runWaitCallbacks(100);
      }
    });
  }

  public static Steam getInstance() {
    return INSTANCE;
  }

  /**
   * Calling this method will try it's best to ensure that the steam-connection is authenticated.
   * If not logged in already, it will login via either token or username+password prompt, and block the current thread until done.
   * If already logged in, it'll just return true.
   * <p>
   * Realistically this should be called before ANY action that requires the client to be authenticated, e.g. sending protobuf requests.
   *
   * @return true if logged in, false if not.
   */
  public synchronized boolean ensureAuthenticated() {
    if (client.isConnected() && user.getSteamID() != null && user.getSteamID().convertToUInt64() != 0L) {
      // Already logged in
      return true;
    }

    AccountStorage storage = AccountStorage.getInstance();
    Account stored = storage.getAccount();

    if (!client.isConnected()) {
      client.connect();
      Util.waitUntil(this.client::isConnected, 5000, null, null);
    }

    try {
      if (stored != null && stored.getRefreshToken() != null && !stored.getRefreshToken().isEmpty()) {
        System.out.println("Attempting token login for user '%s'".formatted(stored.getUsername()));

        LogOnDetails tokenDetails = new LogOnDetails();
        tokenDetails.setUsername(stored.getUsername());
        tokenDetails.setAccessToken(stored.getRefreshToken());

        CompletableFuture<LoggedOnCallback> tokenFuture = new CompletableFuture<>();
        Closeable tokenSub = callbackManager.subscribe(LoggedOnCallback.class, tokenFuture::complete);

        user.logOn(tokenDetails);
        LoggedOnCallback tokenResult = tokenFuture.get(20, TimeUnit.SECONDS);
        tokenSub.close();

        if (tokenResult.getResult() == EResult.OK) {
          System.out.println("Token login succeeded");
          return true;
        } else {
          System.out.println("Token login failed: " + tokenResult.getResult());
        }
      }

      // 4) Fallback: prompt user for username/password
      Pair<String, String> creds = Util.showLoginDialog();
      String username = creds.getLeft();
      String password = creds.getRight();

      AuthSessionDetails authDetails = new AuthSessionDetails();
      authDetails.username = username;
      authDetails.password = password;
      authDetails.persistentSession = true;
      authDetails.guardData = (stored != null ? stored.getSteamGuardData() : null);
      authDetails.authenticator = new in.dragonbra.javasteam.steam.authentication.UserConsoleAuthenticator();

      // Begin credential‐based auth + 2FA polling
      var authSession = client.getAuthentication().beginAuthSessionViaCredentials(authDetails).get();
      var poll = authSession.pollingWaitForResult().get();

      // Extract new tokens and guard data
      String newRefresh = poll.getRefreshToken();
      String newGuard = (poll.getNewGuardData() != null) ? poll.getNewGuardData() : authDetails.guardData;
      String accountName = poll.getAccountName();

      // 5) Full logon with the new refresh token
      LogOnDetails pwdDetails = new LogOnDetails();
      pwdDetails.setUsername(accountName);
      pwdDetails.setAccessToken(newRefresh);

      CompletableFuture<LoggedOnCallback> pwdFuture = new CompletableFuture<>();
      Closeable pwdSub = callbackManager.subscribe(LoggedOnCallback.class, pwdFuture::complete);

      user.logOn(pwdDetails);
      LoggedOnCallback pwdResult = pwdFuture.get(20, TimeUnit.SECONDS);
      pwdSub.close();

      if (pwdResult.getResult() != EResult.OK) {
        System.err.println("Final logOn failed: " + pwdResult.getResult());
        return false;
      }

      // 6) Persist the fresh Account (now that SteamID is available)
      Account newAccount = new Account(accountName, newRefresh, user.getSteamID(), newGuard);
      storage.setAccount(newAccount);
      storage.save();
      System.out.println("Saved new account credentials.");

      return true;

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public SteamClient getClient() {
    return client;
  }

  public SteamUser getUser() {
    return user;
  }
}
