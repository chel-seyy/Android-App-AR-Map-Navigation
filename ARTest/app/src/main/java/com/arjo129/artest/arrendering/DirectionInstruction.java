package com.arjo129.artest.arrendering;

import java.io.Serializable;

public class DirectionInstruction implements Serializable {
    public float direction; //Store the compass angle of which way to go here
    public float distance; //Store the distance to travel till you execute this instruction
    public DirectionInstruction(float dist, float dir){
        distance = dist;
        direction = dir;
    }
}
