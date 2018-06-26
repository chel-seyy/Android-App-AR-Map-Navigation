package com.arjo129.artest.arrendering;

public class DirectionInstruction {
    public float direction; //Store the compass angle of which way to go here
    public float distance; //Store the distance to travel till you execute this instruction
    DirectionInstruction(float dist, float dir){
        distance = dist;
        direction = dir;
    }
}
