package de.chloedev.achievementhelper.steam;

import de.chloedev.achievementhelper.io.AccountStorage;
import de.chloedev.achievementhelper.steam.account.Account;
import de.chloedev.achievementhelper.util.Logger;
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
import java.util.concurrent.atomic.AtomicReference;

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
  public boolean ensureAuthenticated() {
    // If already connected and authenticated, return true
    if (client.isConnected() && user.getSteamID() != null && user.getSteamID().convertToUInt64() != 0L) {
      return true;
    }

    AccountStorage storage = AccountStorage.getInstance();
    Account stored = storage.getAccount();

    // Connect the client if needed
    if (!client.isConnected()) {
      client.connect();
      Util.waitUntil(client::isConnected, 5000, null, null);
    }

    try {
      // Try logging in with existing refresh token
      if (stored != null && stored.getRefreshToken() != null && !stored.getRefreshToken().isEmpty()) {
        LogOnDetails logOnDetails = new LogOnDetails();
        logOnDetails.setUsername(stored.getUsername());
        logOnDetails.setAccessToken(stored.getRefreshToken());
        logOnDetails.setLoginID(912359125);

        CompletableFuture<LoggedOnCallback> tokenFuture = new CompletableFuture<>();
        try (Closeable tokenSub = callbackManager.subscribe(LoggedOnCallback.class, tokenFuture::complete)) {
          user.logOn(logOnDetails);
          LoggedOnCallback tokenResult = tokenFuture.get(20, TimeUnit.SECONDS);

          if (tokenResult.getResult() == EResult.OK) {
            Logger.info("Token login succeeded");
            return true;
          } else {
            Logger.error("Token login failed: %s", tokenResult.getResult().name());
          }
        }
      }

      // Get credentials from the user via UI
      AtomicReference<Pair<String, String>> credentials = new AtomicReference<>(null);

      credentials.set(Util.showLoginDialog());

      Util.waitUntil(() -> credentials.get() != null, -1, null, null);

      String username = credentials.get().getLeft();
      String password = credentials.get().getRight();

      AuthSessionDetails authDetails = new AuthSessionDetails();
      authDetails.username = username;
      authDetails.password = password;
      authDetails.persistentSession = true;
      authDetails.guardData = (stored != null && stored.getSteamGuardData() != null) ? stored.getSteamGuardData() : "";
      SteamAuthenticator authenticator = authenticator = new SteamAuthenticator();
      authDetails.authenticator = authenticator;

      // Begin authentication session and poll for result
      var authSession = client.getAuthentication().beginAuthSessionViaCredentials(authDetails).get();
      var poll = authSession.pollingWaitForResult().get();

      authenticator.cleanup();

      String newRefresh = poll.getRefreshToken();
      String newGuard = (poll.getNewGuardData() != null) ? poll.getNewGuardData() : authDetails.guardData;
      String accountName = poll.getAccountName();

      // Final login with new credentials
      LogOnDetails logOnDetails = new LogOnDetails();
      logOnDetails.setUsername(accountName);
      logOnDetails.setAccessToken(newRefresh);
      logOnDetails.setLoginID(912359125);

      CompletableFuture<LoggedOnCallback> pwdFuture = new CompletableFuture<>();
      try (Closeable pwdSub = callbackManager.subscribe(LoggedOnCallback.class, pwdFuture::complete)) {
        user.logOn(logOnDetails);
        LoggedOnCallback result = pwdFuture.get(20, TimeUnit.SECONDS);

        if (result.getResult() != EResult.OK) {
          Logger.error("Final logOn failed:  %s", result.getResult().name());
          return false;
        }

        // Save new account credentials
        Account newAccount = new Account(accountName, newRefresh, user.getSteamID(), newGuard);
        storage.setAccount(newAccount);
        storage.save();
        Logger.info("Saved new account credentials.");

        return true;
      }

    } catch (Exception e) {
      Logger.error(e);
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
