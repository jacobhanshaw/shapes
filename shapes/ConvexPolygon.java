package shapes;

import java.awt.*;

/**
 * A convex polygon that appears on screen and interacts with other shapes.
 * You won't use <code>ConvexPolygon</code> directly. Instead, you'll use one
 * of its subclasses, {@link Rectangle} or {@link Triangle}.
 */
public abstract class ConvexPolygon extends Shape {

  protected boolean displaysRotation;
  abstract Point[] getUnrotatedCorners();

  public ConvexPolygon() {
    super();
    setDisplaysRotation(true);
  }

  public Point[] getCorners() {
    Point[] corners = getUnrotatedCorners();
    if (
      !displaysRotation ||
      direction == null ||
      Math.abs(direction.getRadians()) < Geometry.EPSILON
    ) {
      return corners;
    }

    double angle = direction.getRadians();
    for (int i = 0; i < corners.length; i++) {
      double xOffset = corners[i].getX() - center.getX();
      double yOffset = corners[i].getY() - center.getY();
      corners[i] = new Point(
        center.getX() + xOffset * Math.cos(angle) - yOffset * Math.sin(angle),
        center.getY() + xOffset * Math.sin(angle) + yOffset * Math.cos(angle)
      );
    }

    return corners;
  }

  double maxRadius() {
    double max = 0;
    for (Point corner : getCorners()) {
      max = Math.max(max, center.distanceTo(corner));
    }
    return max;
  }

  Segment[] getSides() {
    Point[] corners = getCorners();
    Segment[] sides = new Segment[corners.length];
    for (int i = 0; i < corners.length; i++) {
      sides[i] = new Segment(corners[i], corners[(i + 1) % corners.length]);
    }
    return sides;
  }

  Point maxMovement(Point target, Segment obstacle) {
    Segment path = new Segment(getCenter(), target);
    Point maxMove = Geometry.maxMovement(this, target, obstacle);
    maxMove = Geometry.insertGap(this, path, maxMove);
    return maxMove;
  }

  Point maxMovement(Point target, Shape obstacle) {
    Segment path = new Segment(getCenter(), target);
    Point maxMove = Geometry.maxMovement(this, target, obstacle);
    maxMove = Geometry.insertGap(this, path, maxMove);
    return maxMove;
  }

  public boolean isOffscreen() {
    for (Point corner : getCorners()) {
      if (!Geometry.offscreen(corner)) {
        return false;
      }
    }
    return true;
  }

  public boolean contains(Shape shape) {
    if (this.isDestroyed() || shape.isDestroyed()) {
      return false;
    }
    if (shape instanceof ConvexPolygon) {
      Point[] corners = ((ConvexPolygon) shape).getCorners();
      for (Point corner : corners) {
        if (!this.contains(corner)) {
          return false;
        }
      }
      return true;
    } else if (shape instanceof Circle) {
      Circle circle = (Circle) shape;
      if (!this.contains(circle.getCenter())) {
        return false;
      }
      for (Segment side : getSides()) {
        double distance =
          Geometry.perpendicularThrough(side, circle.getCenter()).length();
        if (distance < circle.getRadius()) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  public boolean contains(Point p) {
    // the point is inside if all cross products have the same sign
    boolean positive = false;
    boolean first = true;
    for (Segment side : getSides()) {
      boolean pos =
        Geometry.cross(side.vector(), new Vector(side.getStart(), p)) > 0;
      if (first) {
        positive = pos;
        first = false;
        continue;
      }
      if (pos != positive) {
        return false;
      }
    }
    return true;
  }

  void renderSpeech(Graphics2D g) {
    if (!isSpeaking()) {
      return;
    }
    g.setColor(getSpeechColor());
    // upper left of the talk bubble - upper right of the polygon
    Point upperLeft = null;
    for (Point corner : getCorners()) {
      if (upperLeft == null) {
        upperLeft = corner;
        continue;
      }
      upperLeft.setX(Math.max(upperLeft.getX(), corner.getX()));
      upperLeft.setY(Math.max(upperLeft.getY(), corner.getY()));
    }
    Vector heightOffset =
      new Vector(0.0, -1.0 * g.getFontMetrics(g.getFont()).getDescent());

    Point lowerLeft = upperLeft.translation(heightOffset);
    g.drawString(
      getSpeech(),
      (int)lowerLeft.getCanvasX(),
      (int)lowerLeft.getCanvasY()
    );
  }

  void render(Graphics2D g) {
    renderSpeech(g);
    if (this.isInvisible()) {
      return;
    }

    g.setColor(getColor());
    Point[] corners = getCorners();
    int[] x = new int[corners.length];
    int[] y = new int[corners.length];
    for (int i = 0; i < corners.length; i++) {
      x[i] = (int)corners[i].getCanvasX();
      y[i] = (int)corners[i].getCanvasY();
    }
    if (this.isFilled()) {
      g.fillPolygon(x, y, corners.length);
    } else {
      g.drawPolygon(x, y, corners.length);
    }
  }

  /**
   * Set whether this shape displays the direction it is facing. When set to
   * false, calls to {@link #setDirection} will not change the appearance of
   * this shape, but they will still affect which direction this shape moves
   * when {@link #move(double)} is called.
   *
   * @param displaysRotation  true to display this shape's rotation, false to
   *                          hide it.
   */
  public void setDisplaysRotation(boolean displaysRotation) {
    this.displaysRotation = displaysRotation;
  }

  public boolean displaysRotation() {
    return displaysRotation;
  }
}
