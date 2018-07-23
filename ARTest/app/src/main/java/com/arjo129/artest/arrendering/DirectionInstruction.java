package com.arjo129.artest.arrendering;

import com.arjo129.artest.places.Connector;

import java.io.Serializable;

public class DirectionInstruction implements Serializable {
    public float direction; //Store the compass angle of which way to go here
    public float distance; //Store the distance to travel till you execute this instruction
    public boolean isConnector;
    public Connector connector_type;
    public boolean goingUp;
    public DirectionInstruction(float dist, float dir){
        distance = dist;
        direction = dir;
    }
}
