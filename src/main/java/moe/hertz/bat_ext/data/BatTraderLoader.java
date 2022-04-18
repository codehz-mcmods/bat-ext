package moe.hertz.bat_ext.data;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

@Slf4j
public class BatTraderLoader extends JsonDataLoader {
  private static final Gson GSON = new GsonBuilder().create();
  @Getter
  private Map<Identifier, BatTraderData> registry = ImmutableMap.of();

  public BatTraderLoader() {
    super(GSON, "bat_trades");
  }

  @Override
  protected void apply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler) {
    var builder = ImmutableMap.<Identifier, BatTraderData>builder();
    map.forEach((id, json) -> {
      try {
        builder.put(id, GSON.fromJson(json, BatTraderData.class));
      } catch (Exception exception) {
        log.error("Couldn't parse bat trade {}", id, (Object) exception);
      }
    });
    this.registry = builder.build();
  }
}
