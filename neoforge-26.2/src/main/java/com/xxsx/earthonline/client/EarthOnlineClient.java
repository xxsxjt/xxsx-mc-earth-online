package com.xxsx.earthonline.client;

import com.xxsx.earthonline.EarthOnline;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@OnlyIn(Dist.CLIENT)
public final class EarthOnlineClient {
    private EarthOnlineClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EarthOnlineClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(EarthOnline.PROCESSING_MACHINE_MENU.get(), ProcessingMachineScreen::new);
    }

    public static void openNotebook() {
        Minecraft.getInstance().gui.setScreen(new FieldGeologyNotebookScreen());
    }
}
