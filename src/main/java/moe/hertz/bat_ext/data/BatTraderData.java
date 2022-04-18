package moe.hertz.bat_ext.data;

import java.util.List;

import com.google.gson.JsonElement;

public record BatTraderData(
    List<Offer> offers) {
  public static record Offer(
      ItemTemplate input1,
      ItemTemplate input2,
      ItemTemplate output,
      int maxUses) {
  }

  public static record ItemTemplate(
      String item,
      int count,
      JsonElement nbt) {
  }
}
