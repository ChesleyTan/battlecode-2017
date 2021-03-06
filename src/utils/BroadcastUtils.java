package utils;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static utils.Globals.rc;

/**
 * Class that enables bots to easily read and write to the broadcast array.
 * NOTE: This class cache's its broadcast reads. To prevent out of date data,
 * call the invalidateDirectives() method at the start of each turn once.
 */
public class BroadcastUtils extends Globals {

  /*private static int hashID(int id) {
    //IDs start at 10000 and go in 4096 size blocks.
    //256 as modulus base because unlikely that collisions will occur with 256 possibilities.
    return MathUtils.clamp(0,255,  (id-10000)%256  );
  }*/

  /*public static int[] getSquadMemberHashes(int squadID) {

  }*/

  private static boolean areDirectivesValid = false;
  private static Directive[] directiveCache = new Directive[DIRECTIVE_NUM];

  /**
   * Invalidates the Region Directive cache. Should be called once at the start of each turn.
   */
  public static void invalidateDirectives() {
    areDirectivesValid = false;
  }

  /**
   * Removes the directive with the specified priority
   */
  public static void removeRegionDirective(int priority)
    throws GameActionException {
    if (!MathUtils.isInRange(0,DIRECTIVE_NUM-1,priority))
      throw new IllegalArgumentException("Priority must be between 0 and "+(DIRECTIVE_NUM-1)+", inclusive");
    rc.broadcast(DIRECTIVE_START_CHANNEL + priority, 0);
    directiveCache[priority] = null;
  }

  /**
   * Adds a region directive to the broadcast channels.
   * @param directive The ID of the directive. Use BroadcastUtils.Directives class to get the ID.
   * @param priority The priority of the directive. Values closer to 0 have higher priority.
   *                 Should be in the range of 0 to DIRECTIVE_NUM (exclusive)
   * @param center The location of the directive.
   * @param radius The radius of the directive. Must be from 0 to 64 (exclusive)
   * @throws GameActionException If the broadcast failed
   */
  public static void addRegionDirective(int directive, int priority, MapLocation center, float radius)
  throws GameActionException {
    addRegionDirective(directive, priority, (int)center.x, (int)center.y, (int)radius);
  }

  /**
   * Adds a region directive to the broadcast channels.
   * @param directive The ID of the directive. Use BroadcastUtils.Directives class to get the ID.
   * @param priority The priority of the directive. Values closer to 0 have higher priority.
   *                 Should be in the range of 0 to DIRECTIVE_NUM (exclusive)
   * @param x The x location of the directive. Must be from 0 to 1024 (exclusive)
   * @param y The y location of the directive. Must be from 0 to 1024 (exclusive)
   * @param radius The radius of the directive. Must be from 0 to 64 (exclusive)
   * @throws GameActionException If the broadcast failed
   */
  public static void addRegionDirective(int directive, int priority, int x, int y, int radius)
  throws GameActionException {
    if (!MathUtils.isInRange(0,DIRECTIVE_NUM-1,priority))
    throw new IllegalArgumentException("Priority must be between 0 and "+(DIRECTIVE_NUM-1)+", inclusive");
    int packedDirective = directive << 29; //set 3 MSB =8 of packedDirective to be directive id (8 numbers)
    packedDirective |= (x&0b1111111111) << 19; //set 10 next bits to be x coord (1024 numbers)
    packedDirective |= (y&0b1111111111) <<9; //set 10 next bits to be y coord (1024 numbers)
    packedDirective |= (radius&0b111111) <<3; //set next 6 bits to be radius (64 numbers)
    //this leaves the 3 LSB to allow for decay counter. They will start at 0, and can go up to 7 inclusive
    //if they are told to decay but are already at the maximum age (7), the directive gets set to null
    rc.broadcast(DIRECTIVE_START_CHANNEL + priority, packedDirective);
    directiveCache[priority] = new Directive(directive, priority, x, y, radius, 0);
  }

  /**
   * Gets all of the current region directives, sorted by priority.
   * NOTE: DO NOT MODIFY THIS ARRAY! This array is a direct reference to the internal
   * cache of directives used in this Class. Modifying this array will cause unexpected behavior.
   * If you want to mess with the array, clone it first!
   * @return An array of the Directive objects
   * @throws GameActionException
   */
  public static Directive[] getAllRegionDirectives() throws GameActionException {
    updateDirectiveCache();
    return directiveCache;
  }

  /**
   * Gets all directives active at the coordinates given.
   * The index of the array corresponds to the directive priority.
   * Null elements of the array correspond to the absence of a directive for a given priority.
   * Intelligently caches directives. Only reads broadcast when caches are invalid.
   */
  public static Directive[] getRegionDirectives(MapLocation location) throws GameActionException {
    return getRegionDirectives((int)location.x, (int)location.y);
  }

  /**
   * Gets all directives active at the coordinates given.
   * The index of the array corresponds to the directive priority.
   * Null elements of the array correspond to the absence of a directive for a given priority.
   * Intelligently caches directives. Only reads broadcast when caches are invalid.
   */
  public static Directive[] getRegionDirectives(int xCoordinate, int yCoordinate) throws GameActionException {
    updateDirectiveCache();
    Directive[] results = new Directive[DIRECTIVE_NUM];
    for (int i=0; i<DIRECTIVE_NUM;i++) {
      Directive d = directiveCache[i];
      int dx = d.x-xCoordinate;
      int dy = d.y-yCoordinate;
      if (dx*dx + dy*dy <= d.radius*d.radius) results[i] = d;
    }
    return results;
  }

  private static void updateDirectiveCache() throws GameActionException {
    if (!areDirectivesValid) {
      for (int i=0; i<DIRECTIVE_NUM; i++) {
        int packedDirective = rc.readBroadcast(DIRECTIVE_START_CHANNEL + i);
        int age = (packedDirective  &                              0b111);
        int radius=(packedDirective &                        0b111111000) >>> 3;
        int y = (packedDirective    &              0b1111111111000000000) >>> 9;
        int x = (packedDirective    &    0b11111111110000000000000000000) >>> 19;
        int type = (packedDirective & 0b11100000000000000000000000000000) >>> 29;
        Directive d = new Directive(type, i, x, y, radius, age);
        directiveCache[i] = d;
      }
      areDirectivesValid = true;
    }
  }

  /**
   * Encapsulates directive info to allow it to be returned as one cohesive object.
   */
  public static final class Directive {

    public static final int NULL = 0;
    public static final int ATTACK = 1;
    public static final int DEFEND = 2;
    public static final int SCOUT = 3;
    public static final int FARM = 4;
    public static final int DISTRACT = 5;
    public static final int FORTIFY = 6;
    public static final int CUT = 7;

    public final int type, priority, x, y, radius, age;

    /**
     * Represents a region directive.
     * @param type The type of the directive. Use BroadcastUtils.Directives class to get the ID.
     * @param priority The priority of the directive. Can also be thought as its unique identifier.
     * @param x The x location of the directive. Must be from 0 to 1024 (exclusive)
     * @param y The y location of the directive. Must be from 0 to 1024 (exclusive)
     * @param radius The radius of the directive. Must be from 0 to 64 (exclusive)
     */
    private Directive(int type, int priority, int x, int y, int radius, int age) {
      this.type = type;
      this.priority = priority;
      this.x = x;
      this.y = y;
      this.radius = radius;
      this.age = age;
    }

  }
}
