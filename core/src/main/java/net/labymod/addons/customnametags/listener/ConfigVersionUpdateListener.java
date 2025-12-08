package net.labymod.addons.customnametags.listener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.labymod.config.ConfigurationVersionUpdateEvent;
import net.labymod.api.util.logging.Logging;
import org.jetbrains.annotations.NotNull;
import java.util.Map.Entry;

public class ConfigVersionUpdateListener {
  private Logging logger = null;

  @Subscribe
  public void onConfigVersionUpdate(ConfigurationVersionUpdateEvent event) {
    if (this.logger == null) {
      this.logger = Logging.getLogger();
    }
    this.logger.info("Config update detected, current version: " + event.getUsedVersion() + ", required version: " + event.getIntendedVersion());

    JsonObject object = event.getJsonObject();

    this.mapVersion1ToVersion2(object); //Currently only version 1 to version 2 exists

    event.setJsonObject(object);
  }

  private void mapVersion1ToVersion2(@NotNull JsonObject jsonObject) {
    JsonElement customTags = jsonObject.get("customTags");
    if (customTags == null || customTags.isJsonNull()) {
      this.logger.warn("Could not find customTags in config.json, updating of config to version 2 failed.");
      return;
    }

    for (Entry<String, JsonElement> entry : customTags.getAsJsonObject().entrySet()) {
      JsonElement customTag = entry.getValue();
      if (customTag.isJsonNull() || !customTag.isJsonObject()) {
        this.logger.warn("Invalid customTag in config.json, updating of config to version 2 failed for entry " + entry.getKey() + ".");
        continue;
      }
      customTag.getAsJsonObject().addProperty("originalName", entry.getKey());
    }
    this.logger.info("Updated config to version 2.");
  }
}
