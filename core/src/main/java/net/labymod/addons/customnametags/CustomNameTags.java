package net.labymod.addons.customnametags;

import com.google.inject.Singleton;
import net.labymod.addons.customnametags.listener.ChatReceiveListener;
import net.labymod.addons.customnametags.listener.PlayerNameTagRenderListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonListener;

@AddonListener
@Singleton
public class CustomNameTags extends LabyAddon<CustomNameTagsConfiguration> {

  @Override
  protected void enable() {
    this.registerSettingCategory();

    this.registerListener(ChatReceiveListener.class);
    this.registerListener(PlayerNameTagRenderListener.class);
  }

  @Override
  protected Class<CustomNameTagsConfiguration> configurationClass() {
    return CustomNameTagsConfiguration.class;
  }
}