package moe.hertz.bat_ext;

import moe.hertz.bat_ext.entity.BatTrader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class BatExt implements ModInitializer {
  @Override
  public void onInitialize() {
    FabricDefaultAttributeRegistry.register(BatTrader.TYPE, BatTrader.createBatAttributes());
  }
}
