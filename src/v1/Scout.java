package v1;

import battlecode.common.*;

public class Scout extends Globals{
	
  private static final int attack_start_channel = 500;
  private static final int defense_start_channel = 500;
  
  public static void evade() throws GameActionException {
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    boolean willHit = false;
    for(BulletInfo b: nearbyBullets){
      MapLocation finalLoc = b.location.add(b.dir, b.speed);
      float dist = (float)(Math.sqrt(Math.pow(here.x - finalLoc.x, 2) + Math.pow(here.y - finalLoc.y, 2)));
      
    }
  }
  
  
	public static void loop() throws GameActionException {
	}
}