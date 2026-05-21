package com.lazify;

import com.replaymod.coffeeclient.hook.CoffeeMod;
import com.replaymod.coffeeclient.hook.event.CLInitEvent;

@CoffeeMod(name = LazifyMod.NAME, version = LazifyMod.VERSION)
public class CoffeeEntry {

    @CoffeeMod.EventHandler
    public void onInit(CLInitEvent event) {
        LazifyMod.doInit();
    }
}
