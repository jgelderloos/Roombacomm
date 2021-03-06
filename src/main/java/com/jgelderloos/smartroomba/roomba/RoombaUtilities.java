/*
 *  SmartRoomba - RoombaUtilities
 *
 *  Copyright (c) 2018 Jon Gelderloos
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General
 *  Public License along with this library; if not, write to the
 *  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA  02111-1307  USA
 *
 */

package com.jgelderloos.smartroomba.roomba;

import com.jgelderloos.smartroomba.roomba.RoombaConstants.SensorPacketGroup;
import com.jgelderloos.smartroomba.roomba.RoombaConstants.Direction;
import com.jgelderloos.smartroomba.roomba.RoombaConstants.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

public class RoombaUtilities {
    private static final Logger LOGGER = LogManager.getLogger(RoombaUtilities.class);
    private Map<SensorPacketGroup,Integer> sensorPackSizeMap;

    public RoombaUtilities() {
        sensorPackSizeMap = new HashMap<>();
        sensorPackSizeMap.put(SensorPacketGroup.P0, 26);
        sensorPackSizeMap.put(SensorPacketGroup.P1, 10);
        sensorPackSizeMap.put(SensorPacketGroup.P2, 6);
        sensorPackSizeMap.put(SensorPacketGroup.P3, 10);
        sensorPackSizeMap.put(SensorPacketGroup.P4, 14);
        sensorPackSizeMap.put(SensorPacketGroup.P5, 12);
        sensorPackSizeMap.put(SensorPacketGroup.P6, 52);
        // The spec says pack 100 is 80 in length but 90 bytes are returned
        sensorPackSizeMap.put(SensorPacketGroup.P100, 93);
        sensorPackSizeMap.put(SensorPacketGroup.P101, 28);
        sensorPackSizeMap.put(SensorPacketGroup.P106, 12);
        sensorPackSizeMap.put(SensorPacketGroup.P107, 9);
    }

    public void sleep(int millis, String whileString) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            LOGGER.info("Error sleeping while {}.", whileString, e);
            Thread.currentThread().interrupt();
        }
    }
    // TODO: null handling
    public int getSensorPacketSize(SensorPacketGroup packet) {
        return sensorPackSizeMap.get(packet);
    }

    public double getMilimetersFromEncoderCounts(int encoderCounts) {
        return encoderCounts * (Math.PI * RoombaConstants.MILLIMETERS_PER_WHEEL_TURN / RoombaConstants.ENCODER_COUNTS_PER_WHEEL_TURN);
    }

    // For now this is calculated on a forward clockwise turn
    public double getRadiansFromWheelDistance(double leftWheelDistance, double rightWheelDistance) {
        return (rightWheelDistance - leftWheelDistance) / RoombaConstants.WHEELBASE;
    }

    public double getTurnRadians(double arcRadians, double straightDistance, double radius) {
        return Math.PI - arcRadians - (Math.asin(radius * (Math.sin(arcRadians) / straightDistance)));
    }

    public double getRadius(double radians, double arcDistance) {
        return arcDistance / radians;
    }

    public double getArcDistance(double radians, double radius) {
        return radius * radians;
    }

    public double getStraightDistance(double radians, double radius) {
        return Math.sqrt((2 * Math.pow(radius, 2)) - (2 * Math.pow(radius, 2) * Math.cos(radians)));
    }

    // TODO: all the Math methods use radians not degrees
    public double getNearSideLength(double angle, double hypotenuse) {
        return hypotenuse * Math.cos(angle);
    }

    public double getFarSideLength(double angle, double hypotenuse) {
        return hypotenuse * Math.sin(angle);
    }

    public Point2D.Double getPointOnCircle(double radians, double radius, RoombaConstants.Side side, RoombaConstants.Direction direction) {
        double radiansToUse = radians;
        if ((side == Side.RIGHT && direction == Direction.FORWARDS) || (side == Side.LEFT && direction == Direction.BACKWARDS)) {
            radiansToUse += Math.PI;
        }
        return new Point2D.Double(getLength(radiansToUse, radius), getHeight(radiansToUse, radius));
    }

    public double getHeight(double radians, double radius) {
        return radius * Math.sin(radians);
    }

    public double getLength(double radians, double radius) {
        return radius * Math.cos(radians);
    }

    public int getChangeInEncoderCounts(int lastCount, int currentCount) {
        int count = currentCount - lastCount;
        int testBackwardsRollCount = currentCount - (lastCount + RoombaConstants.MAX_ENCODER_COUNT);
        int testForwardsRollCount = (currentCount + RoombaConstants.MAX_ENCODER_COUNT) - lastCount;

        // Detect a rollover by seeing the sign will flip, and that the original count is very close to the max value that the
        // encoder bytes can hold.
        if (!isSameSign(count, testForwardsRollCount) && Math.abs(count) > RoombaConstants.MAX_ENCODER_COUNT - 10000) {
            return testForwardsRollCount;
        } else if (!isSameSign(count, testBackwardsRollCount) && Math.abs(count) > RoombaConstants.MAX_ENCODER_COUNT - 10000) {
            return testBackwardsRollCount;
        }
        return count;
    }

    public boolean isSameSign(int first, int second) {
        return (first ^ second) >= 0;
    }
}
