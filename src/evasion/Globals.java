package evasion;

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
  public static final int EARLY_SCOUTS_CHANNEL = 5;
  public static Direction NORTH, SOUTH, EAST, WEST;

  public static void init(RobotController theRC) {
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
  }

  public static void update() {
    here = rc.getLocation();
  }
}