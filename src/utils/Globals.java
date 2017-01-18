package utils;

import battlecode.common.*;

import java.util.Random;

public class Globals {

	public static final boolean DEBUG = false;

	public static RobotController rc;
	public static MapLocation here;
	public static Team us;
	public static Team them;
	public static int myID;
	public static RobotType myType;
	public static Random rand;
  public static final int PRODUCED_GARDENERS_CHANNEL = 50;
  public static final int EARLY_UNITS_CHANNEL = 5;
  public static final int DEFENSE_START_CHANNEL = 250;
  public static final int DEFENSE_END_CHANNEL = 350;
  public static final int ATTACK_START_CHANNEL = 500;
  public static final int ATTACK_END_CHANNEL = 600;
  public static final int PRODUCTION_GARDENERS_CHANNEL = 101;
  public static final int PRODUCED_PRODUCTION_GARDENERS_CHANNEL = 102;
  public static final int ATTACK_BLOCK_WIDTH = 5;
  public static Direction NORTH, SOUTH, EAST, WEST;
  public static final int UNKNOWN = Integer.MIN_VALUE;
  public static int minX = UNKNOWN;
  public static int maxX = UNKNOWN;
  public static int minY = UNKNOWN;
  public static int maxY = UNKNOWN;
  public static int penultimateRound = 0;
  public static int currentRoundNum = 0;

	public static void init(RobotController theRC){
		rc = theRC;
		us = rc.getTeam();
		them = us.opponent();
		myID = rc.getID();
		myType = rc.getType();
		here = rc.getLocation();
		rand = new Random(theRC.getID());
		NORTH = Direction.getNorth();
		SOUTH = Direction.getSouth();
		EAST = Direction.getEast();
		WEST = Direction.getWest();
		penultimateRound = rc.getRoundLimit() - 1;
		currentRoundNum = rc.getRoundNum();
	}
	
	public static void update(){
		here = rc.getLocation();
		currentRoundNum = rc.getRoundNum();
	}

  public static void updateMapBoundaries() throws GameActionException {
    if (minX == UNKNOWN) {
      int xBounds = rc.readBroadcast(BroadcastUtils.MAP_X_BOUNDS_CHANNEL);
      boolean isSet = (xBounds & (0xF0000000)) == 0xF0000000;
      if (isSet) {
        minX = (xBounds & (0x00000FFF));
      }
      else {
        float lookAhead = myType.sensorRadius - 1;
        MapLocation testLocation = here.add(WEST, lookAhead);
        while (lookAhead > 0 && !rc.onTheMap(testLocation)) {
          minX = Math.max(0, (int)testLocation.x);
          --lookAhead;
          testLocation = here.add(WEST, lookAhead);
        }
        if (minX != UNKNOWN) {
          int x = (minX & (0x00000FFF));
          rc.broadcast(BroadcastUtils.MAP_X_BOUNDS_CHANNEL, xBounds | 0xF0000000 | x);
        }
      }
    }
    if (maxX == UNKNOWN) {
      int xBounds = rc.readBroadcast(BroadcastUtils.MAP_X_BOUNDS_CHANNEL);
      boolean isSet = (xBounds & (0x0F000000)) == 0x0F000000;
      if (isSet) {
        maxX = (xBounds & (0x00FFF000)) >> 12;
      }
      else {
        float lookAhead = myType.sensorRadius - 1;
        MapLocation testLocation = here.add(EAST, lookAhead);
        while (lookAhead > 0 && !rc.onTheMap(testLocation)) {
          maxX = (int)testLocation.x;
          --lookAhead;
          testLocation = here.add(EAST, lookAhead);
        }
        if (maxX != UNKNOWN) {
          int x = (maxX & (0x00000FFF)) << 12;
          rc.broadcast(BroadcastUtils.MAP_X_BOUNDS_CHANNEL, xBounds | 0x0F000000 | x);
        }
      }
    }
    if (minY == UNKNOWN) {
      int yBounds = rc.readBroadcast(BroadcastUtils.MAP_Y_BOUNDS_CHANNEL);
      boolean isSet = (yBounds & (0xF0000000)) == 0xF0000000;
      if (isSet) {
        minY = (yBounds & (0x00000FFF));
      }
      else {
        float lookAhead = myType.sensorRadius - 1;
        MapLocation testLocation = here.add(SOUTH, lookAhead);
        while (lookAhead > 0 && !rc.onTheMap(testLocation)) {
          minY = Math.max(0, (int)testLocation.y);
          --lookAhead;
          testLocation = here.add(SOUTH, lookAhead);
        }
        if (minY != UNKNOWN) {
          int y = (minY & (0x00000FFF));
          rc.broadcast(BroadcastUtils.MAP_Y_BOUNDS_CHANNEL, yBounds | 0xF0000000 | y);
        }
      }
    }
    if (maxY == UNKNOWN) {
      int yBounds = rc.readBroadcast(BroadcastUtils.MAP_Y_BOUNDS_CHANNEL);
      boolean isSet = (yBounds & (0x0F000000)) == 0x0F000000;
      if (isSet) {
        maxY = (yBounds & (0x00FFF000)) >> 12;
      }
      else {
        float lookAhead = myType.sensorRadius - 1;
        MapLocation testLocation = here.add(NORTH, lookAhead);
        while (lookAhead > 0 && !rc.onTheMap(testLocation)) {
          maxY = (int)testLocation.y;
          --lookAhead;
          testLocation = here.add(NORTH, lookAhead);
        }
        if (maxY != UNKNOWN) {
          int y = (maxY & (0x00000FFF)) << 12;
          rc.broadcast(BroadcastUtils.MAP_Y_BOUNDS_CHANNEL, yBounds | 0x0F000000 | y);
        }
      }
    }
  }
}