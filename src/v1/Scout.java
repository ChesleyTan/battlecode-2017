package v1;

import battlecode.common.*;

public class Scout extends Globals {

  private final static int ROAM = 0;
  private final static int ATTACK = 1;
  private static int current_mode = ROAM;
  private static Direction direction;
  private static final int KEEPAWAY_RADIUS = 3;
  private static int squad_channel;
  
  public static void dodge(BulletInfo[] nearbyBullets, RobotInfo[] nearbyRobots) throws GameActionException {
    boolean willHit = false;
    MapLocation[] startLocs = new MapLocation[nearbyBullets.length];
    MapLocation[] endLocs = new MapLocation[nearbyBullets.length];
    int index = 0;
    for (BulletInfo b : nearbyBullets) {
      MapLocation finalLoc = b.location.add(b.dir, b.speed);
      startLocs[index] = b.location;
      endLocs[index] = finalLoc;
      index++;
      /*
      float dist = (float)(Math.sqrt(Math.pow(here.x - finalLoc.x, 2) + Math.pow(here.y - finalLoc.y, 2)));
      if(dist < RobotType.SCOUT.bodyRadius){
        willHit = true;
      }*/
      if (RobotPlayer.willCollideWithMe(b)) {
        willHit = true;
      }
    }
    float sumX = 0;
    float sumY = 0;
    if(willHit){
      for(int i = 0; i < index; i++){
        float x0 = startLocs[i].x;
        float y0 = startLocs[i].y;
        float x1 = endLocs[i].x;
        float y1 = endLocs[i].y;
        float a = x1 - x0;
        float b = y0 - y1;
        float c = x0 * y1 - y0 * x1;
        float distance = (float)(Math.abs(a* here.x + b* here.y + c)
            / Math.sqrt(Math.pow(a,2) + Math.pow(b, 2)));
        float x2 = (float)((b * (b * here.x - a * here.y) - a * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        float y2 = (float)((a * (a * here.y - b * here.x) - b * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        Direction away = here.directionTo(new MapLocation(x2, y2)).opposite();
        float weighted = (RobotType.SCOUT.bulletSightRadius - distance) 
            / RobotType.SCOUT.bulletSightRadius * RobotType.SCOUT.strideRadius;
        sumX += away.getDeltaX(weighted);
        sumY += away.getDeltaY(weighted);
      }
    }
    for (RobotInfo r: nearbyRobots){
      if(r.getType() == RobotType.LUMBERJACK){
        Direction their_direction = here.directionTo(r.location).opposite();
        float their_distance = (RobotType.SCOUT.sensorRadius - here.distanceTo(r.location))/RobotType.SCOUT.sensorRadius * RobotType.SCOUT.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
        index ++;
      }
    }
    float finaldist = (float)Math.sqrt(Math.pow(sumX, 2) + Math.pow(sumY, 2));
    if(finaldist <= RobotType.SCOUT.strideRadius){
      MapLocation destination = new MapLocation(here.x + sumX , here.y + sumY);
      if(rc.canMove(destination) && !rc.hasMoved()){
        rc.move(destination);
      }
    }
    else{
      Direction finalDir = new Direction(sumX, sumY);
      if(rc.canMove(finalDir) && !rc.hasMoved()){
        rc.move(finalDir);
      }
    }
  }
  
  public static void findSquad() throws GameActionException{
    int i = ATTACK_START_CHANNEL;
    while (i < 900){
      int squad_count = rc.readBroadcast(i);
      System.out.println(squad_count);
      if (squad_count < 5){
        squad_channel = i;
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i = i + 4;
    }
  }

  public static void alert() throws GameActionException {
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(KEEPAWAY_RADIUS, them);
    if (nearbyBullets != null && nearbyBullets.length != 0 || nearbyRobots != null && nearbyRobots.length != 0){
      dodge(nearbyBullets, nearbyRobots);
    }
    if (nearbyRobots == null || nearbyRobots.length == 0){
      return;
    }
    else{
      if (rc.getRoundNum() < 100){
        for (RobotInfo enemy : nearbyRobots){
          if(enemy.getType() != RobotType.ARCHON){
            rc.broadcast(squad_channel + 1, enemy.ID);
            rc.broadcast(squad_channel + 2, (int)(enemy.location.x));
            rc.broadcast(squad_channel + 3, (int)(enemy.location.y));
            MapLocation center = enemy.location;
            direction = here.directionTo(center);
            if (rc.canFireSingleShot() && clearShot(enemy.location)){
              rc.fireSingleShot(here.directionTo(center));
            }
            current_mode = ATTACK;
          }
          else{
            return;
          }
        }
      }
      else{
        RobotInfo enemy = nearbyRobots[0];
        rc.broadcast(squad_channel + 1, enemy.ID);
        rc.broadcast(squad_channel + 2, (int)(enemy.location.x));
        rc.broadcast(squad_channel + 3, (int)(enemy.location.y));
        MapLocation center = enemy.location;
        direction = here.directionTo(center);
        if (rc.canFireSingleShot() && clearShot(enemy.location)){
          rc.fireSingleShot(here.directionTo(center));
        }
        current_mode = ATTACK;
      }
    }
  }
  
  /*
   * Returns True if I have a clear shot at the person, false otherwise;
   */
  public static boolean clearShot(MapLocation target){
    Direction targetDir = here.directionTo(target);
    float distanceTarget = here.distanceTo(target);
    RobotInfo[] friendlies = rc.senseNearbyRobots(distanceTarget, us);
    MapLocation outerEdge = here.add(targetDir, RobotType.SCOUT.bodyRadius);
    for (RobotInfo r : friendlies){
      if (RobotPlayer.willCollideWithTargetLocation(outerEdge, targetDir, r.location, r.getRadius())){
        return false;
      }
    }
    TreeInfo[] trees = rc.senseNearbyTrees(distanceTarget);
    for (TreeInfo t : trees){
      if (RobotPlayer.willCollideWithTargetLocation(outerEdge, targetDir, t.location, t.getRadius())){
        return false;
      }
    }
    return true;
  }
  
  public static void engage(int target) throws GameActionException {
    RobotInfo targetRobot = rc.senseRobot(target);
    rc.broadcast(squad_channel + 2, (int) targetRobot.location.x);
    rc.broadcast(squad_channel + 3, (int) targetRobot.location.y);
    direction = here.directionTo(targetRobot.location);
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius, them);
    if (nearbyBullets != null && nearbyBullets.length != 0 || nearbyRobots != null && nearbyRobots.length != 0){
      dodge(nearbyBullets, nearbyRobots);
    }
    //System.out.println(target);
    if (!rc.hasMoved()){
      float absolute_dist = (float) Math.sqrt(Math.pow(here.x - targetRobot.location.x, 2)
          + Math.pow(here.y - targetRobot.location.y, 2));
      if (absolute_dist > KEEPAWAY_RADIUS + RobotType.SCOUT.strideRadius) {
        if (rc.canMove(direction)) {
          rc.move(direction);
        }
      }
      else {
        Direction rotated20 = direction.opposite().rotateLeftDegrees(20);
        MapLocation newLoc = targetRobot.location.add(rotated20, KEEPAWAY_RADIUS - 2);
        if (rc.canMove(newLoc)) {
          rc.move(newLoc);
        }
        else {
          rotated20 = direction.opposite().rotateRightDegrees(20);
          newLoc = targetRobot.location.add(rotated20, KEEPAWAY_RADIUS - 2);
          if (rc.canMove(newLoc)) {
            rc.move(newLoc);
          }
        }
      }
    }
    if (rc.canFireSingleShot() && clearShot(targetRobot.location)) {
      rc.fireSingleShot(direction);
    }
  }  
	public static void loop() throws GameActionException {
	  try{
	    Globals.update();
	    if (rc.getRoundNum() < 100){
	      squad_channel = 100;
	      MapLocation[] enemies = rc.getInitialArchonLocations(them);
	      MapLocation first = enemies[0];
	      direction = here.directionTo(first);
	    }
	    else{
	      findSquad();
	      direction = new Direction((float)(Math.random() * 2 * Math.PI));
	    }
  	  while(true){
  	    Globals.update();
    	  if(current_mode == ROAM){
    	    //rc.setIndicatorDot(here, 0, 0, 255);
    	    int target = rc.readBroadcast(squad_channel + 1);
    	    if (target != 0){
    	      current_mode = ATTACK;
    	      int xLoc = rc.readBroadcast(squad_channel + 2);
    	      int yLoc = rc.readBroadcast(squad_channel + 3);
    	      Direction target_direction = here.directionTo(new MapLocation(xLoc, yLoc));
    	      direction = target_direction;
    	      BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    	      RobotInfo[] nearbyRobots = rc.senseNearbyRobots(KEEPAWAY_RADIUS, them);
    	      if (nearbyBullets != null && nearbyBullets.length != 0 || nearbyRobots != null && nearbyRobots.length != 0){
    	        dodge(nearbyBullets, nearbyRobots);
    	      }
    	      else if(rc.canMove(direction) && !rc.hasMoved()){
    	        rc.move(direction);
    	      }
    	    }
    	    else{
    	      alert();
    	      if (!rc.hasMoved()){
      	      if (rc.canMove(direction)){
      	        rc.move(direction);
      	      }
      	      else if (!rc.onTheMap(here.add(direction, RobotType.SCOUT.strideRadius)) || rc.senseNearbyRobots(2.5f, us) != null){
      	        Direction newDir = direction.rotateRightDegrees(10);
      	        while(!rc.canMove(newDir)){
      	          newDir = newDir.rotateRightDegrees(10);
      	        }
                direction = newDir;
      	        rc.move(direction);
      	        //System.out.println(direction.getAngleDegrees());
      	      }
    	      }
    	    }
    	  }
    	  else{
    	    //rc.setIndicatorDot(here, 0, 255, 0);
    	    int target = rc.readBroadcast(squad_channel + 1);
    	    if(rc.canSenseRobot(target)){
    	      while(rc.canSenseRobot(target)){
    	        Globals.update();
    	        engage(target);
    	        Clock.yield();
    	      }
    	      rc.broadcast(squad_channel + 1, 0);
            current_mode = ROAM;
            direction = new Direction((float)(Math.random() * 2 * Math.PI));
            if (!rc.hasMoved() && rc.canMove(direction)){
              rc.move(direction);
            }
    	    }
    	    else{
    	      if (rc.readBroadcast(squad_channel + 1) != 0){
      	      int xLoc = rc.readBroadcast(squad_channel + 2);
              int yLoc = rc.readBroadcast(squad_channel + 3);
              Direction target_direction = here.directionTo(new MapLocation(xLoc, yLoc));
              if(rc.canMove(target_direction)){
                rc.move(target_direction);
              }
    	      }
    	      else{
    	        current_mode = ROAM;
    	        direction = new Direction((float)(Math.random() * 2 * Math.PI));
    	        if (!rc.hasMoved() && rc.canMove(direction)){
                rc.move(direction);
              }
              //System.out.println(direction.getAngleDegrees());
            }
          }
        }
    	  if(rc.getHealth() < 3f){
    	    int squad_count = rc.readBroadcast(squad_channel);
    	    rc.broadcast(squad_channel, squad_count - 1);
    	  }
        Clock.yield();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}