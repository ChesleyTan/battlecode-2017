package utils;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

/**
 * This class represents the movement data of an object or particle. The class contains information about the object's position AND velocity.
 * The class also provides relevant functions that pertain to the movement of said object or particle.
 *
 * @author TheButlah
 */
public class MData implements Cloneable{

  private float time;
  private MapLocation location;
  private Direction direction;
  private float speed;

  public MData(MapLocation loc, Direction dir, float time, float speed) {
    this.time = time;
    this.speed = speed;
    this.location = loc;
    this.direction = dir;
  }

  public MData(float x, float y, float angle, float time, float speed ) {
    this(new MapLocation(x,y), new Direction(angle), time, speed);
  }

  /**
   * Function to linearly predict the location of the object in the future or past represented by this MData object.
   * @param delta The time elapsed. Should be in terms of the same units as the speed was specified in.
   * In other words, if the speed is in meters/second, delta should be in seconds.
   * A negative value will predict positions in the past.
   * @return The predicted position of the object, not accounting for collisions with walls or other objects.
   */
  public MapLocation predictPositionLinear(double delta) {
    double predictedX = this.getX() + this.getSpeed()*delta*Math.sin(this.getAngle());
    double predictedY = this.getY() + this.getSpeed()*delta*Math.cos(this.getAngle());
    return new MapLocation((float)predictedX,(float)predictedY);
  }

  /**
   * Uses linear projection to predict location with a given time. Will be fully accurate as long as the robot maintains that exact velocity.
   * Also takes walls into account.
   * @param delta he time elapsed. Should be in terms of the same units as the speed was specified in.
   * In other words, if the speed is in meters/second, delta should be in seconds.
   * A negative value will predict positions in the past.
   * @return The predicted location, not accounting for collisions other than walls
   */
  public MapLocation predictPositionLinearBounded(double delta, double upperboundX, double lowerboundX, double upperboundY, double lowerboundY) {

    double predictedX = this.getX() + this.getSpeed()*delta*Math.sin(this.getAngle());
    double predictedY = this.getY() + this.getSpeed()*delta*Math.cos(this.getAngle());
    double dx = predictedX - this.getX();
    double dy = predictedY - this.getY();

    //Scale down vector if x component is out of bounds
    if (predictedX > upperboundX || predictedX < lowerboundX) {
      double diffX = (predictedX>upperboundX) ? predictedX - (upperboundX) : -predictedX + lowerboundX;
      double scale = 1-Math.abs(diffX/dx);
      //Fix the weird behavior that occurs when this component's velocity is zero
      if (MathUtils.isNear(dx,0d)) scale = 1;
      dx = dx*(scale);
      dy = dy*(scale);
      predictedX = this.getX() + dx;
      predictedY = this.getY() + dy;
    }
    //scale down new vector if y component is out of bounds
    if (predictedY > upperboundY || predictedY < lowerboundY) {
      double diffY = (predictedY>upperboundY) ? predictedY - (upperboundY) : -predictedY + lowerboundY;
      double scale = 1-Math.abs(diffY/dy);
      //Fix the weird behavior that occurs when this component's velocity is zero
      if (MathUtils.isNear(dy, 0)) scale = 1;
      dx = dx*(scale);
      dy = dy*(scale);
      predictedX = this.getX() + dx;
      predictedY = this.getY() + dy;
    }
    //Just in case...
    if (Double.isNaN(predictedX) || Double.isNaN(predictedY)) {
      System.out.println("PREDICTED POS IS NaN in MData.java! Resorting to non-bounded prediction!\n" +
                         "X: " + predictedX + ", Y: " + predictedY);
      return predictPositionLinear(delta);
    }

    //finally! that was a lot harder than expected
    return new MapLocation((float)predictedX, (float)predictedY);
  }

  public float getX() {
    return location.x;
  }

  public float getY() {
    return location.y;
  }

  public float getTime() {
    return time;
  }

  public float getAngle() {
    return direction.getAngleDegrees();
  }

  public Direction getDirection() {
    return direction;
  }

  public float getSpeed() {
    return speed;
  }

  public MapLocation getLocation() {
    return location;
  }



  public void setLocation(MapLocation newLocation) {
    this.location = newLocation;
  }

  public void setTime(float time) {
    this.time = time;
  }

  public void setDirection(Direction newDirection) {
    this.direction = newDirection;
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  @Override
  public MData clone() {
    return new MData(location, direction, time, speed);
  }
}