package com.sample.impl.sensor.universalcontroller.helpers;

import com.alexalmanza.controller.wii.identifiers.WiiIdentifier;

import java.util.ArrayList;

public class ControllerMappingPreset {

    public ArrayList<UniversalControllerComponent> componentNames;
    public int controllerIndex;

    // Options for controller mapping
    public ControllerCyclingAction controllerCyclingAction;

}
