package moe.hertz.bat_ext.data;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

@Slf4j
public class BatTraderLoader extends JsonDataLoader implements IdentifiableResourceReloadListener {

  public static final Identifier ID = new Identifier("batext:traders");
  private static final Gson GSON = new GsonBuilder().create();
  @Getter
  private Map<Identifier, BatTraderData> registry = ImmutableMap.of();

  public BatTraderLoader() {
    super(GSON, "bat_traders");
  }

  @Override
  public Identifier getFabricId() {
    return ID;
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler) {
    log.info("loading trader data: {}", map.size());
    var builder = ImmutableMap.<Identifier, BatTraderData>builder();
    map.forEach((id, json) -> {
      try {
        builder.put(id, BatTraderData.read(json.getAsJsonObject()));
      } catch (Exception exception) {
        log.error("Couldn't parse bat trade {}", id, (Object) exception);
      }
    });
    this.registry = builder.build();
  }
}
