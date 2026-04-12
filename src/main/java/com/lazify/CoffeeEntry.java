package com.lazify;

import io.github.moulberry.notenoughupdates.coffeeclient.hook.CoffeeMod;
import io.github.moulberry.notenoughupdates.coffeeclient.hook.event.CLInitEvent;

@CoffeeMod(name = LazifyMod.NAME, version = LazifyMod.VERSION)
public class CoffeeEntry {

    @CoffeeMod.EventHandler
    public void onInit(CLInitEvent event) {
        LazifyMod.doInit();
    }
}
