package com.botts.controller.wii.identifiers;

import net.java.games.input.Component;

public class WiiIdentifier extends Component.Identifier {

    public static final WiiIdentifier MINUS = new WiiIdentifier("Minus");
    public static final WiiIdentifier PLUS = new WiiIdentifier("Plus");
    public static final WiiIdentifier HOME = new WiiIdentifier("Home");
    public static final WiiIdentifier A = new WiiIdentifier("A");
    public static final WiiIdentifier B = new WiiIdentifier("B");
    public static final WiiIdentifier _1 = new WiiIdentifier("1");
    public static final WiiIdentifier _2 = new WiiIdentifier("2");
    public static final WiiIdentifier NONE = new WiiIdentifier("None");
    public static final WiiIdentifier POV = new WiiIdentifier("pov");
    public static final WiiIdentifier X_ACCELERATION = new WiiIdentifier("x-acceleration");
    public static final WiiIdentifier Y_ACCELERATION = new WiiIdentifier("y-acceleration");
    public static final WiiIdentifier Z_ACCELERATION = new WiiIdentifier("z-acceleration");
    protected WiiIdentifier(String name) {
        super(name);
    }
}
