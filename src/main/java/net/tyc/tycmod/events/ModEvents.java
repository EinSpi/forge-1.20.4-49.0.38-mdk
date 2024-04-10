package net.tyc.tycmod.events;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tyc.tycmod.TycMod;
import net.tyc.tycmod.entity.custom.AircraftEntity;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = TycMod.MOD_ID)
public class ModEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
@SubscribeEvent
public static void RideAircraft(PlayerInteractEvent.EntityInteract event)
{
    event.getEntity().startRiding(event.getTarget());
}

}
