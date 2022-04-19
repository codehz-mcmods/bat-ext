package moe.hertz.bat_ext;

import moe.hertz.bat_ext.data.BatTraderLoader;
import moe.hertz.bat_ext.entity.BatTrader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class BatExt implements ModInitializer {
  public static BatTraderLoader BAT_TRADERS = new BatTraderLoader();

  @Override
  public void onInitialize() {
    FabricDefaultAttributeRegistry.register(BatTrader.TYPE, BatTrader.createBatTraderAttributes());
    ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(BAT_TRADERS);
  }
}
