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
  public static final int DIRECTIVE_START_CHANNEL = 0;
  public static final int EARLY_UNITS_CHANNEL = 5;
  public static final int PRODUCED_GARDENERS_CHANNEL = 50;
  public static final int PRODUCTION_GARDENERS_CHANNEL = 101;
  public static final int PRODUCED_PRODUCTION_GARDENERS_CHANNEL = 102;
  public static final int DEFENSE_START_CHANNEL = 250;
  public static final int DEFENSE_END_CHANNEL = 350;
  public static final int DEFENSE_BLOCK_WIDTH = 4;
  // Note: A value of -1 represents a null target, an artifact of when 0 could be the ID of an archon
  public static final int ATTACK_START_CHANNEL = 500;
  public static final int ATTACK_END_CHANNEL = 600;
  public static final int ATTACK_BLOCK_WIDTH = 4;
  // Note: A value of 0 (default) represents a null target
  public static final int GARDENER_TARGET_CACHE_CHANNEL = 601;
  public static final int GARDENER_TARGET_CACHE_BLOCK_WIDTH = 2;
  public static final int MAP_X_BOUNDS_CHANNEL = 900;
  public static final int MAP_Y_BOUNDS_CHANNEL = 901;
  public static final int MAP_SYMMETRY_CHANNEL = 902;
  public static final int UNITS_BIAS_PRODUCTION_CHANNEL = 1002;
  public static Direction NORTH, SOUTH, EAST, WEST;
  public static final int UNKNOWN = Integer.MIN_VALUE;
  public static int minX = UNKNOWN;
  public static int maxX = UNKNOWN;
  public static int minY = UNKNOWN;
  public static int maxY = UNKNOWN;
  public static int penultimateRound = 0;
  public static int currentRoundNum = 0;
  // Horizontal line of symmetry
  public static final int HORIZONTAL_SYMMETRY = 1;
  // Vertical line of symmetry
  public static final int VERTICAL_SYMMETRY = 2;
  public static final int ROTATIONAL_SYMMETRY = 3;
  public static final int SYMMETRY_UNKNOWN = 0;
  public static int symmetry = SYMMETRY_UNKNOWN;
  // If vertical symmetry, this is the x coordinate of the line of symmetry
  // If rotational symmetry, this is the x coordinate of the point of rotation
  public static float symmetryX = -1f;
  // If horizontal symmetry, this is the y coordinate of the line of symmetry
  // If rotational symmetry, this is the y coordinate of the point of rotation
  public static float symmetryY = -1f;

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
    penultimateRound = rc.getRoundLimit() - 1;
    currentRoundNum = rc.getRoundNum();
  }

  public static void update() {
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
          minX = Math.max(0, (int) testLocation.x);
          --lookAhead;
          testLocation = here.add(WEST, lookAhead);
        }
        if (minX != UNKNOWN) {
          int x = (minX & (0x00000FFF));
          xBounds = xBounds | 0xF0000000 | x;
          if (symmetry != SYMMETRY_UNKNOWN) {
            if (symmetry == VERTICAL_SYMMETRY || symmetry == ROTATIONAL_SYMMETRY) {
              maxX = ((int)symmetryX - minX) + (int)symmetryX;
              x = (maxX & (0x00000FFF)) << 12;
              xBounds = xBounds | 0x0F000000 | x;
            }
          }
          rc.broadcast(BroadcastUtils.MAP_X_BOUNDS_CHANNEL, xBounds);
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
          maxX = (int) testLocation.x;
          --lookAhead;
          testLocation = here.add(EAST, lookAhead);
        }
        if (maxX != UNKNOWN) {
          int x = (maxX & (0x00000FFF)) << 12;
          xBounds = xBounds | 0x0F000000 | x;
          if (symmetry != SYMMETRY_UNKNOWN) {
            if (symmetry == VERTICAL_SYMMETRY || symmetry == ROTATIONAL_SYMMETRY) {
              minX = ((int)symmetryX - maxX) + (int)symmetryX;
              x = (minX & (0x00000FFF));
              xBounds = xBounds | 0xF0000000 | x;
            }
          }
          rc.broadcast(BroadcastUtils.MAP_X_BOUNDS_CHANNEL, xBounds);
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
          minY = Math.max(0, (int) testLocation.y);
          --lookAhead;
          testLocation = here.add(SOUTH, lookAhead);
        }
        if (minY != UNKNOWN) {
          int y = (minY & (0x00000FFF));
          yBounds = yBounds | 0xF0000000 | y;
          if (symmetry != SYMMETRY_UNKNOWN) {
            if (symmetry == HORIZONTAL_SYMMETRY || symmetry == ROTATIONAL_SYMMETRY) {
              maxY = ((int)symmetryY - minY) + (int)symmetryY;
              y = (maxY & (0x00000FFF)) << 12;
              yBounds = yBounds | 0x0F000000 | y;
            }
          }
          rc.broadcast(BroadcastUtils.MAP_Y_BOUNDS_CHANNEL, yBounds);
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
          maxY = (int) testLocation.y;
          --lookAhead;
          testLocation = here.add(NORTH, lookAhead);
        }
        if (maxY != UNKNOWN) {
          int y = (maxY & (0x00000FFF)) << 12;
          yBounds = yBounds | 0x0F000000 | y;
          if (symmetry != SYMMETRY_UNKNOWN) {
            if (symmetry == HORIZONTAL_SYMMETRY || symmetry == ROTATIONAL_SYMMETRY) {
              minY = ((int)symmetryY - maxY) + (int)symmetryY;
              y = (minY & (0x00000FFF));
              yBounds = yBounds | 0xF0000000 | y;
            }
          }
          rc.broadcast(BroadcastUtils.MAP_Y_BOUNDS_CHANNEL, yBounds);
        }
      }
    }
  }

  public static void trackEnemyGardeners() throws GameActionException {
    if (Clock.getBytecodesLeft() < 1000) {
      return;
    }
    RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, them);
    int[] targets = new int[(ATTACK_END_CHANNEL - ATTACK_START_CHANNEL) / ATTACK_BLOCK_WIDTH];
    for (int channel = ATTACK_START_CHANNEL, i = 0; channel < ATTACK_END_CHANNEL; channel += ATTACK_BLOCK_WIDTH, ++i) {
      targets[i] = rc.readBroadcast(channel + 1);
    }
    outer: for (RobotInfo ri : nearbyEnemies) {
      if (Clock.getBytecodesLeft() < 200) {
        return;
      }
      if (ri.getType() == RobotType.GARDENER) {
        int id = ri.getID();
        for (int target : targets) {
          if (target == id) {
            continue outer;
          }
        }
        MapLocation targetLoc = ri.getLocation();
        int targetX = (int) targetLoc.x;
        int targetY = (int) targetLoc.y;
        rc.broadcast(GARDENER_TARGET_CACHE_CHANNEL, id);
        rc.broadcast(GARDENER_TARGET_CACHE_CHANNEL + 1, (targetX << 16) | targetY);
        return;
      }
    }
  }

  public static int readGardenerCacheX(int data) {
    return (data & 0xFFFF0000) >>> 16;
  }

  public static int readGardenerCacheY(int data) {
    return (data & 0x0000FFFF);
  }

  public static int determineMapSymmetry(MapLocation[] myArchons, MapLocation[] enemyArchons) throws GameActionException {
    // TODO rotational symmetry may take the form of horizontal or vertical symmetry
    int symmetry1 = SYMMETRY_UNKNOWN;
    outer1: for (MapLocation myArchonLoc : myArchons) {
      for (MapLocation enemyArchonLoc : enemyArchons) {
        if (MathUtils.isNear(myArchonLoc.x, enemyArchonLoc.x)) {
          symmetry1 = HORIZONTAL_SYMMETRY;
          if (symmetryY < 0) {
            symmetryY = (myArchonLoc.y + enemyArchonLoc.y) / 2;
          }
          else {
            if (!MathUtils.isNear(symmetryY, (myArchonLoc.y + enemyArchonLoc.y) / 2)) {
              symmetry1 = SYMMETRY_UNKNOWN;
              break outer1;
            }
          }
          continue outer1;
        }
      }
      symmetry1 = SYMMETRY_UNKNOWN;
      break;
    }
    int symmetry2 = SYMMETRY_UNKNOWN;
    outer2: for (MapLocation myArchonLoc : myArchons) {
      for (MapLocation enemyArchonLoc : enemyArchons) {
        if (MathUtils.isNear(myArchonLoc.y, enemyArchonLoc.y)) {
          symmetry2 = VERTICAL_SYMMETRY;
          if (symmetryX < 0) {
            symmetryX = (myArchonLoc.x + enemyArchonLoc.x) / 2;
          }
          else {
            if (!MathUtils.isNear(symmetryX, (myArchonLoc.x + enemyArchonLoc.x) / 2)) {
              symmetry2 = SYMMETRY_UNKNOWN;
              break outer2;
            }
          }
          continue outer2;
        }
      }
      symmetry2 = SYMMETRY_UNKNOWN;
      break;
    }
    if (symmetry1 == HORIZONTAL_SYMMETRY && symmetry2 == SYMMETRY_UNKNOWN) {
      symmetry = HORIZONTAL_SYMMETRY;
    }
    else if (symmetry1 == SYMMETRY_UNKNOWN && symmetry2 == VERTICAL_SYMMETRY) {
      symmetry = VERTICAL_SYMMETRY;
    }
    else if (symmetry1 == HORIZONTAL_SYMMETRY && symmetry2 == VERTICAL_SYMMETRY) {
      symmetry = ROTATIONAL_SYMMETRY;
    }
    if (symmetry == SYMMETRY_UNKNOWN) {
      // Find point of rotation of rotational symmetry
      symmetry = ROTATIONAL_SYMMETRY;
      float[] candidateXCoordinates = new float[myArchons.length];
      float[] candidateYCoordinates = new float[myArchons.length];
      MapLocation referenceArchonLoc = enemyArchons[0];
      for (int i = 0; i < myArchons.length; ++i) {
        MapLocation myArchonLoc = myArchons[i];
        candidateXCoordinates[i] = (myArchonLoc.x + referenceArchonLoc.x) / 2;
        candidateYCoordinates[i] = (myArchonLoc.y + referenceArchonLoc.y) / 2;
      }
      if (enemyArchons.length > 1) {
        // Find intersection of candidate centers of rotation
        referenceArchonLoc = enemyArchons[1];
        outer3: for (int i = 0; i < myArchons.length; ++i) {
          MapLocation myArchonLoc = myArchons[i];
          float candidateX = (myArchonLoc.x + referenceArchonLoc.x) / 2;
          float candidateY = (myArchonLoc.y + referenceArchonLoc.y) / 2;
          for (int j = 0; j < myArchons.length; ++j) {
            if (MathUtils.isNear(candidateX, candidateXCoordinates[j], 0.01f)
                && MathUtils.isNear(candidateY, candidateYCoordinates[j], 0.01f)) {
              symmetryX = candidateX;
              symmetryY = candidateY;
              break outer3;
            }
          }
        }
      }
      else {
        symmetryX = candidateXCoordinates[0];
        symmetryY = candidateYCoordinates[0];
      }
    }
    int data = (symmetry & 0b00000000000000000000000000000011) << 30;
    int x = ((int) symmetryX) & 0b00000000000000000111111111111111;
    int y = ((int) symmetryY) & 0b00000000000000000111111111111111;
    data = data | (x << 15) | y;
    rc.broadcast(MAP_SYMMETRY_CHANNEL, data);
    return symmetry;
  }

  public static void readMapSymmetry() throws GameActionException {
    int data = rc.readBroadcast(MAP_SYMMETRY_CHANNEL);
    symmetry = (data & 0b11000000000000000000000000000000) >>> 30;
    symmetryX = (data & 0b00111111111111111000000000000000) >>> 15;
    symmetryY = (data & 0b00000000000000000111111111111111);
  }
}