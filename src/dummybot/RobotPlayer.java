package dummybot;

import battlecode.common.*;
import utils.MData;
import utils.TargetingUtils;
import v1.Globals;

import javax.swing.*;

/**
 * Dummy RobotPlayer that should be used for simple testing purposes only
 */
public strictfp class RobotPlayer {
  static RobotController rc;

  @SuppressWarnings("unused")
  public static void run(RobotController rc) throws GameActionException {
    try{
      Globals.init(rc);
      switch(rc.getType()){
        case ARCHON:
          //Run Archon
          break;
        case GARDENER:
          //Run Gardener
          break;
        case SCOUT:
          //Run Scout
          break;
        case SOLDIER:
          //Run Soldier
          break;
        case TANK:
          //Run Tank
          break;
      }
      int count = 0;
      System.out.println("North: " + Direction.getNorth().getAngleDegrees());
      System.out.println("South: " + Direction.getSouth().getAngleDegrees());
      System.out.println("East: " + Direction.getEast().getAngleDegrees());
      System.out.println("West: " + Direction.getWest().getAngleDegrees());

      MData mdata1 = null;
      MData mdata2 = null;
      MapLocation predicted;
      MapLocation locLast = null;
      MapLocation locCurr = null;

      RobotInfo target = null;
      while (true) {
        RobotInfo[] robots = rc.senseNearbyRobots();
        if (robots.length != 0) {
          target = robots[0];
          locCurr = target.getLocation();
        }
        if (target != null) {
          locLast = locCurr;
          locCurr = target.getLocation();
          Direction dir = locLast.directionTo(locCurr);
          float speed = locLast.distanceTo(locCurr);
          MData mdata= new MData(locCurr,dir,rc.getRoundNum(),speed);
          MapLocation firingSoln = TargetingUtils.getLinearTargetPoint(mdata, rc.getLocation(), rc.getType().bulletSpeed);
          rc.fireSingleShot(rc.getLocation().directionTo(firingSoln));
        }
        count++;
        Clock.yield();
      }
    } catch (Exception e) {
      e.printStackTrace(System.out);
      System.out.println();
      System.out.println();
      System.out.println();

    }

  }
}
