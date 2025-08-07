package de.chloedev.achievementhelper.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Logger {

  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss:SSS");
  private static final Object lock = new Object();
  private static PrintWriter writer;

  static {
    try {
      File logFile = new File(Util.getStorageDirectory(), "latest.log");
      writer = new PrintWriter(new FileWriter(logFile, false), true);
    } catch (IOException e) {
      System.err.println("Failed to initialize log file writer: " + e.getMessage());
      writer = null;
    }
  }

  private Logger() {
    // no instances
  }

  public static void info(String format, Object... args) {
    log("INFO", String.format(format, args), null);
  }

  public static void debug(String format, Object... args) {
    log("DEBUG", String.format(format, args), null);
  }

  public static void warn(String format, Object... args) {
    log("WARN", String.format(format, args), null);
  }

  public static void error(String format, Object... args) {
    log("ERROR", String.format(format, args), null);
  }

  public static void error(Throwable t) {
    log("ERROR", "Error", t);
  }

  public static void error(Throwable t, String format, Object... args) {
    log("ERROR", String.format(format, args), t);
  }

  private static void log(String level, String msg, Throwable t) {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    String line = String.format("[%s] [%s] %s", timestamp, level, msg);
    if ("ERROR".equals(level) || "WARN".equals(level)) {
      System.err.println(line);
    } else {
      System.out.println(line);
    }
    if (t != null) {
      t.printStackTrace(("ERROR".equals(level) || "WARN".equals(level)) ? System.err : System.out);
    }
    synchronized (lock) {
      if (writer != null) {
        writer.println(line);
        if (t != null) {
          t.printStackTrace(writer);
        }
        writer.flush();
      }
    }
  }
}
