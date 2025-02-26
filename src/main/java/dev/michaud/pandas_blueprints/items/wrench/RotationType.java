package dev.michaud.pandas_blueprints.items.wrench;

import net.minecraft.util.math.Direction.Axis;

public enum RotationType {
  HORIZONTAL_CLOCKWISE(true, true, Axis.Y),
  HORIZONTAL_COUNTERCLOCKWISE(true, false, Axis.Y),
  VERTICAL_CLOCKWISE_X(false, true, Axis.X),
  VERTICAL_CLOCKWISE_Z(false, true, Axis.Z),
  VERTICAL_COUNTERCLOCKWISE_X(false, false, Axis.X),
  VERTICAL_COUNTERCLOCKWISE_Z(false, false, Axis.Z);

  private final boolean horizontal;
  private final boolean clockwise;
  private final Axis acrossAxis;

  RotationType(boolean horizontal, boolean clockwise, Axis acrossAxis) {
    this.horizontal = horizontal;
    this.clockwise = clockwise;
    this.acrossAxis = acrossAxis;
  }

  public RotationType toHorizontal() {
    return switch (this) {
      case HORIZONTAL_CLOCKWISE, HORIZONTAL_COUNTERCLOCKWISE -> this;
      case VERTICAL_CLOCKWISE_X, VERTICAL_CLOCKWISE_Z -> HORIZONTAL_CLOCKWISE;
      case VERTICAL_COUNTERCLOCKWISE_X, VERTICAL_COUNTERCLOCKWISE_Z -> HORIZONTAL_COUNTERCLOCKWISE;
    };
  }

  public RotationType toVertical(boolean xAxis) {
    return switch (this) {
      case HORIZONTAL_CLOCKWISE -> xAxis ? VERTICAL_CLOCKWISE_X : VERTICAL_CLOCKWISE_Z;
      case HORIZONTAL_COUNTERCLOCKWISE -> xAxis ? VERTICAL_COUNTERCLOCKWISE_X : VERTICAL_COUNTERCLOCKWISE_Z;
      case VERTICAL_CLOCKWISE_X -> xAxis ? this : VERTICAL_CLOCKWISE_Z;
      case VERTICAL_CLOCKWISE_Z -> xAxis ? VERTICAL_CLOCKWISE_X : this;
      case VERTICAL_COUNTERCLOCKWISE_X -> xAxis ? this : VERTICAL_COUNTERCLOCKWISE_Z;
      case VERTICAL_COUNTERCLOCKWISE_Z -> xAxis ? VERTICAL_COUNTERCLOCKWISE_X : this;
    };
  }

  public static RotationType of(Axis axis, boolean clockwise) {
    return switch (axis) {
      case X -> clockwise ? VERTICAL_CLOCKWISE_X : VERTICAL_COUNTERCLOCKWISE_X;
      case Y -> clockwise ? HORIZONTAL_CLOCKWISE : HORIZONTAL_COUNTERCLOCKWISE;
      case Z -> clockwise ? VERTICAL_CLOCKWISE_Z : VERTICAL_COUNTERCLOCKWISE_Z;
    };
  }

  public static RotationType ofHorizontal(boolean clockwise) {
    return clockwise ? HORIZONTAL_CLOCKWISE : HORIZONTAL_COUNTERCLOCKWISE;
  }

  public static RotationType ofVertical(boolean clockwise, boolean xAxis) {
    if (clockwise) {
      return xAxis ? VERTICAL_CLOCKWISE_X : VERTICAL_CLOCKWISE_Z;
    } else {
      return xAxis ? VERTICAL_COUNTERCLOCKWISE_X : VERTICAL_COUNTERCLOCKWISE_Z;
    }
  }

  public boolean isHorizontal() {
    return horizontal;
  }

  public boolean isVertical() {
    return !horizontal;
  }

  public boolean isClockwise() {
    return clockwise;
  }

  public boolean isCounterClockwise() {
    return !clockwise;
  }

  public Axis getAcrossAxis() {
    return acrossAxis;
  }
}