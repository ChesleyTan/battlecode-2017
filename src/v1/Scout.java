package v1;

import battlecode.common.*;

public class Scout extends Globals{
	
  private static final int attack_start_channel = 500;
  private static final int defense_start_channel = 250;
  private static final int target_channel = 451;
  private enum Mode {ROAM, ATTACK};
  private static Mode current_mode = Mode.ROAM;
  private static Direction direction;
  
  public static void dodge() throws GameActionException {
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    if (nearbyBullets == null || nearbyBullets.length == 0){
      return;
    }
    boolean willHit = false;
    MapLocation[] startLocs = new MapLocation[nearbyBullets.length];
    MapLocation[] endLocs = new MapLocation[nearbyBullets.length];
    int index = 0;
    for(BulletInfo b: nearbyBullets){
      MapLocation finalLoc = b.location.add(b.dir, b.speed);
      startLocs[index] = b.location;
      endLocs[index] = finalLoc;
      index++;
      float dist = (float)(Math.sqrt(Math.pow(here.x - finalLoc.x, 2) + Math.pow(here.y - finalLoc.y, 2)));
      if(dist < RobotType.SCOUT.bodyRadius){
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
      float weighted = distance / RobotType.SCOUT.bulletSightRadius * RobotType.SCOUT.strideRadius;
      sumX += away.getDeltaX(weighted);
      sumY += away.getDeltaY(weighted);
    }
    MapLocation destination = new MapLocation(here.x + sumX / index, here.y + sumY / index);
    if(rc.canMove(destination) && !rc.hasMoved()){
      rc.move(destination);
    }
  }
  
  public static void alert() throws GameActionException {
    RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadius, them);
    if (enemies == null || enemies.length == 0){
      return;
    }
    else{
      RobotInfo enemy = enemies[0];
      rc.broadcast(target_channel, 1);
      rc.broadcast(target_channel + 1, (int)(enemy.location.x));
      rc.broadcast(target_channel + 2, (int)(enemy.location.y));
      dodge();
      MapLocation center = enemy.location;
      rc.fireSingleShot(here.directionTo(center));
      current_mode = Mode.ATTACK;
    }
  }
  
  
	public static void loop() throws GameActionException {
	  while(true){
	    Globals.update();
  	  if(current_mode == Mode.ROAM){
  	    int target = rc.readBroadcast(target_channel);
  	    if (target != 0){
  	      current_mode = Mode.ATTACK;
  	      int xLoc = rc.readBroadcast(target_channel + 1);
  	      int yLoc = rc.readBroadcast(target_channel + 2);
  	      Direction target_direction = here.directionTo(new MapLocation(xLoc, yLoc));
  	      direction = target_direction;
  	      dodge();
  	      if(rc.canMove(direction) && !rc.hasMoved()){
  	        rc.move(direction);
  	      }
  	      Clock.yield();
  	    }
  	    else{
  	      alert();
  	    }
  	  }
  	  
	  }
	}
}