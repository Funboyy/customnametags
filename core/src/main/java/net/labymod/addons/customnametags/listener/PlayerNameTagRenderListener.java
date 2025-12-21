/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.labymod.addons.customnametags.listener;

import java.util.Map.Entry;
import java.util.Set;
import net.labymod.addons.customnametags.CustomNameTag;
import net.labymod.addons.customnametags.CustomNameTags;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.TextComponent;
import net.labymod.api.client.network.NetworkPlayerInfo;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.PlayerNameTagRenderEvent;
import net.labymod.api.event.client.render.PlayerNameTagRenderEvent.Context;
import net.labymod.api.util.Pair;

public class PlayerNameTagRenderListener {

  private final CustomNameTags addon;

  public PlayerNameTagRenderListener(CustomNameTags addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onPlayerNameTagRender(PlayerNameTagRenderEvent event) {
    NetworkPlayerInfo networkPlayerInfo = event.getPlayerInfo();
    if (networkPlayerInfo == null) {
      return;
    }

    String playerName;
    CustomNameTag customNameTag;
    if (event.context() == Context.TAB_LIST && this.addon.configuration().checkForStringInTabList()
        .get()) {
      Pair<String, CustomNameTag> pair = this.getCustomNameTag(
          this.addon.configuration().getCustomTags().entrySet(),
          event.nameTag()
      );

      if (pair == null) {
        playerName = null;
        customNameTag = null;
      } else {
        playerName = pair.getFirst();
        customNameTag = pair.getSecond();
      }
    } else {
      playerName = networkPlayerInfo.profile().getUsername();
      customNameTag = this.getCustomNameTag(playerName);
    }

    if (customNameTag == null || !customNameTag.isEnabled()) {
      return;
    }

    if (customNameTag.isReplaceScoreboard()) {
      event.setNameTag(customNameTag.displayName().copy());
    } else {
      Component newNameTag = this.addon.replaceLegacyContext(event.nameTag().copy());
      this.addon.replaceUsername(
          newNameTag,
          playerName,
          () -> customNameTag.displayName().copy()
      );
      event.setNameTag(newNameTag);
    }
  }

  private CustomNameTag getCustomNameTag(String playerName) {
    for (Entry<String, CustomNameTag> customTagEntry : this.addon.configuration().getCustomTags()
        .entrySet()) {
      CustomNameTag customNameTag = customTagEntry.getValue();
      if (customTagEntry.getKey().equalsIgnoreCase(playerName)) {
        return customNameTag;
      }
    }

    return null;
  }

  private Pair<String, CustomNameTag> getCustomNameTag(
      Set<Entry<String, CustomNameTag>> customNameTags,
      Component component
  ) {
    for (Component child : component.getChildren()) {
      Pair<String, CustomNameTag> pair = this.getCustomNameTag(
          customNameTags,
          child
      );

      if (pair != null) {
        return pair;
      }
    }

    if (!(component instanceof TextComponent textComponent)) {
      return null;
    }

    String text = textComponent.getText().toLowerCase().trim();
    for (Entry<String, CustomNameTag> customNameTag : customNameTags) {
      if (text.endsWith(customNameTag.getKey().toLowerCase())) {
        return Pair.of(customNameTag.getKey(), customNameTag.getValue());
      }
    }

    return null;
  }
}
