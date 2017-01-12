package v1;

import battlecode.common.*;

public class Scout extends Globals{
	
  private static final int attack_start_channel = 500;
  private static final int defense_start_channel = 250;
  private static final int target_channel = 451;
  private String[] Modes = {"ROAM", "ATTACK"};
  private static String current_mode = "ROAM";
  private static Direction direction;
  private static final int keepaway_radius = 5;
  
  static boolean willCollideWithMe(BulletInfo bullet) {
    MapLocation myLocation = here;

    // Get relevant bullet information
    Direction propagationDirection = bullet.dir;
    MapLocation bulletLocation = bullet.location;

    // Calculate bullet relations to this robot
    Direction directionToRobot = bulletLocation.directionTo(myLocation);
    float distToRobot = bulletLocation.distanceTo(myLocation);
    float theta = propagationDirection.radiansBetween(directionToRobot);

    // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
    if (Math.abs(theta) > Math.PI / 2) {
      return false;
    }

    // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
    // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
    // This corresponds to the smallest radius circle centered at our location that would intersect with the
    // line that is the path of the bullet.
    float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

    return (perpendicularDist <= myType.bodyRadius);
  }
  
  public static void dodge(BulletInfo[] nearbyBullets) throws GameActionException {
    boolean willHit = false;
    MapLocation[] startLocs = new MapLocation[nearbyBullets.length];
    MapLocation[] endLocs = new MapLocation[nearbyBullets.length];
    int index = 0;
    for(BulletInfo b: nearbyBullets){
      MapLocation finalLoc = b.location.add(b.dir, b.speed);
      startLocs[index] = b.location;
      endLocs[index] = finalLoc;
      index++;
      /*
      float dist = (float)(Math.sqrt(Math.pow(here.x - finalLoc.x, 2) + Math.pow(here.y - finalLoc.y, 2)));
      if(dist < RobotType.SCOUT.bodyRadius){
        willHit = true;
      }*/
      if (willCollideWithMe(b)){
        System.out.println("true");
        willHit = true;
      }
    }
    if (willHit == false){
      //Don't move
      return;
    }
    float sumX = 0;
    float sumY = 0;
    for(int i = 0; i < index; i++){
      float x0 = startLocs[i].x;
      float y0 = startLocs[i].y;
      float x1 = endLocs[i].x;
      float y1 = endLocs[i].y;
      float a = x1 - x0;
      float b = y0 - y1;
      float c = x0 * y1 - y0 * x1;
      float distance = (float)(Math.abs(a* here.x + b* here.y + c)/Math.sqrt(Math.pow(a,2) + Math.pow(b, 2)));
      float x2 = (float)((b * (b * here.x - a * here.y) - a * c)/(Math.pow(a, 2) + Math.pow(b, 2)));
      float y2 = (float)((a * (a * here.y - b * here.x) - b * c)/(Math.pow(a, 2) + Math.pow(b, 2)));
      Direction away = here.directionTo(new MapLocation(x2, y2)).opposite();
      float weighted = (RobotType.SCOUT.bulletSightRadius - distance) / RobotType.SCOUT.bulletSightRadius * RobotType.SCOUT.strideRadius;
      sumX += away.getDeltaX(weighted);
      sumY += away.getDeltaY(weighted);
    }
    MapLocation destination = new MapLocation(here.x + sumX / index, here.y + sumY / index);
    if(rc.canMove(destination) && !rc.hasMoved()){
      rc.move(destination);
    }
  }
  
  public static void alert() throws GameActionException {
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    if (nearbyBullets != null && nearbyBullets.length != 0){
      dodge(nearbyBullets);
    }
    RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius, them);
    if (enemies == null || enemies.length == 0){
      return;
    }
    else{
      RobotInfo enemy = enemies[0];
      rc.broadcast(target_channel, enemy.ID);
      rc.broadcast(target_channel + 1, (int)(enemy.location.x));
      rc.broadcast(target_channel + 2, (int)(enemy.location.y));
      MapLocation center = enemy.location;
      direction = here.directionTo(center);
      if (rc.canFireSingleShot()){
        rc.fireSingleShot(here.directionTo(center));
      }
      current_mode = "ATTACK";
    }
  }
  
  public static void engage(int target) throws GameActionException{
    RobotInfo targetRobot = rc.senseRobot(target);
    rc.broadcast(target_channel + 1, (int)targetRobot.location.x);
    rc.broadcast(target_channel + 2, (int)targetRobot.location.y);
    direction = here.directionTo(targetRobot.location);
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    if (nearbyBullets != null && nearbyBullets.length != 0){
      dodge(nearbyBullets);
    }
    //System.out.println(target);
    else{
      float absolute_dist = (float)Math.sqrt(Math.pow(here.x - targetRobot.location.x, 2) + Math.pow(here.y - targetRobot.location.y, 2));
      if (absolute_dist > keepaway_radius + RobotType.SCOUT.strideRadius){
        if(rc.canMove(direction)){
          rc.move(direction);
        }
      }
      else{
        Direction rotated20 = direction.opposite().rotateLeftDegrees(20);
        MapLocation newLoc = targetRobot.location.add(rotated20, keepaway_radius);
        if (rc.canMove(newLoc)){
          rc.move(newLoc);
        }
        else{
          rotated20 = direction.opposite().rotateRightDegrees(20);
          newLoc = targetRobot.location.add(rotated20, keepaway_radius);
          if (rc.canMove(newLoc)){
            rc.move(newLoc);
          }
        }
      }
    }
    if (rc.canFireSingleShot()){
      rc.fireSingleShot(direction);
    }
  }
  
	public static void loop() throws GameActionException {
	  try{
	    Globals.update();
	    if (rc.getRoundNum() < 100){
	      MapLocation[] enemies = rc.getInitialArchonLocations(them);
	      MapLocation first = enemies[0];
	      direction = here.directionTo(first);
	    }
	    else{
	      direction = new Direction((float)(Math.random() * 2 * Math.PI));
	    }
  	  while(true){
  	    Globals.update();
    	  if(current_mode == "ROAM"){
    	    int target = rc.readBroadcast(target_channel);
    	    if (target != 0){
    	      current_mode = "ATTACK";
    	      int xLoc = rc.readBroadcast(target_channel + 1);
    	      int yLoc = rc.readBroadcast(target_channel + 2);
    	      Direction target_direction = here.directionTo(new MapLocation(xLoc, yLoc));
    	      direction = target_direction;
    	      BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    	      if (nearbyBullets != null && nearbyBullets.length != 0){
    	        dodge(nearbyBullets);
    	      }
    	      else if(rc.canMove(direction) && !rc.hasMoved()){
    	        rc.move(direction);
    	      }
    	    }
    	    else{
    	      alert();
    	      if (!rc.hasMoved() && rc.canMove(direction)){
    	        rc.move(direction);
    	      }
    	      else if (!rc.onTheMap(here.add(direction, RobotType.SCOUT.strideRadius)) || rc.senseNearbyRobots(2.5f, us) != null){
    	        Direction newDir = direction.rotateRightDegrees(45);
    	        direction = newDir;
    	        if (!rc.hasMoved() && rc.canMove(direction)){
    	          rc.move(direction);
    	        }
    	        //System.out.println(direction.getAngleDegrees());
    	      }
    	    }
    	  }
    	  else{
    	    int target = rc.readBroadcast(target_channel);
    	    if(rc.canSenseRobot(target)){
    	      while(rc.canSenseRobot(target)){
    	        Globals.update();
    	        engage(target);
    	        Clock.yield();
    	      }
    	      rc.broadcast(target_channel, 0);
            current_mode = "ROAM";
            direction = new Direction((float)(Math.random() * 2 * Math.PI));
            if (!rc.hasMoved() && rc.canMove(direction)){
              rc.move(direction);
            }
    	    }
    	    else{
    	      if (rc.readBroadcast(target_channel) != 0){
      	      int xLoc = rc.readBroadcast(target_channel + 1);
              int yLoc = rc.readBroadcast(target_channel + 2);
              Direction target_direction = here.directionTo(new MapLocation(xLoc, yLoc));
              if(rc.canMove(target_direction)){
                rc.move(target_direction);
              }
    	      }
    	      else{
    	        current_mode = "ROAM";
    	        direction = new Direction((float)(Math.random() * 2 * Math.PI));
    	        if (!rc.hasMoved() && rc.canMove(direction)){
                rc.move(direction);
              }
    	      }
    	    }
    	  }
  	    Clock.yield();
  	  }
	  }catch(Exception e){
	    e.printStackTrace();
	  }
	}
}