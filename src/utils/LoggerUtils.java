package utils;

import v1.Globals;

/**
 * Provides useful logging functionality to be used throughout the application.
 *
 * @author Ryan Butler
 */
public class LoggerUtils {

  private static String activeChannel = "";

  /**
   * Sets the active channel of the logger.
   * Only calls to log() that correspond to this channel will output.
   * @param channel The channel to make active
   */
  public static void setActiveChannel(String channel) {
    activeChannel = channel;
  }

  public static String getActiveChannel() {
    return activeChannel;
  }

  /**
   * Logs the message to the current active channel.
   * Will do nothing if the logger has DEBUG = false
   * @param msg The message to output to the console
   */
  public static void log(String msg) {
    if (Globals.DEBUG) System.out.println(msg);
  }

  /**
   * Logs the message to the current active channel.
   * Will do nothing if the logger has DEBUG = false
   * OR if the channel specified is not the current active channel.
   * @param msg The message to output to the console
   */
  public static void log(String channel, String msg) {
    if (Globals.DEBUG && channel.equalsIgnoreCase(activeChannel)) System.out.println(msg);
  }


}
