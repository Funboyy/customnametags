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

package net.labymod.addons.customnametags;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.labymod.addons.customnametags.listener.ChatReceiveListener;
import net.labymod.addons.customnametags.listener.ConfigVersionUpdateListener;
import net.labymod.addons.customnametags.listener.NameTagBackgroundRenderListener;
import net.labymod.addons.customnametags.listener.PlayerNameTagRenderListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.TextComponent;
import net.labymod.api.client.component.TranslatableComponent;
import net.labymod.api.client.component.format.Style;
import net.labymod.api.client.component.format.Style.Merge.Strategy;
import net.labymod.api.client.component.serializer.legacy.LegacyComponentSerializer;
import net.labymod.api.event.client.gui.screen.playerlist.PlayerListUpdateEvent;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class CustomNameTags extends LabyAddon<CustomNameTagsConfiguration> {

  private static CustomNameTags instance;

  public CustomNameTags() {
    instance = this;
  }

  public static CustomNameTags get() {
    return instance;
  }

  @Override
  protected void preConfigurationLoad() {
    this.registerListener(new ConfigVersionUpdateListener());
  }

  @Override
  protected void enable() {
    this.registerSettingCategory();
    this.configuration().removeInvalidNameTags();

    this.registerListener(new ChatReceiveListener(this));
    this.registerListener(new NameTagBackgroundRenderListener(this));
    this.registerListener(new PlayerNameTagRenderListener(this));

    if (this.wasLoadedInRuntime()) {
      this.reloadTabList();
    }
  }

  @Override
  protected Class<CustomNameTagsConfiguration> configurationClass() {
    return CustomNameTagsConfiguration.class;
  }

  public void reloadTabList() {
    this.labyAPI().eventBus().fire(new PlayerListUpdateEvent());
  }

  public boolean replaceUsername(
      Component component,
      String playerName,
      Supplier<Component> customName
  ) {
    boolean replaced = false;
    for (Component child : component.getChildren()) {
      if (this.replaceUsername(child, playerName, customName)) {
        replaced = true;
      }
    }

    if (component instanceof TranslatableComponent) {
      for (Component argument : ((TranslatableComponent) component).getArguments()) {
        if (this.replaceUsername(argument, playerName, customName)) {
          replaced = true;
        }
      }
    }

    if (component instanceof TextComponent textComponent) {
      String text = textComponent.getText();
      Style style = textComponent.style();

      int next = text.indexOf(playerName);
      if (next != -1) {
        replaced = true;
        int length = text.length();
        if (next == 0) {
          if (length == playerName.length()) {
            textComponent.text("");

            Component customNameComponent = customName.get();
            customNameComponent.style(customNameComponent.style().merge(style, Strategy.IF_ABSENT_ON_TARGET));
            component.append(0, customNameComponent);
            return true;
          }
        }

        textComponent.text("");
        int lastNameAt = 0;
        int childIndex = 0;
        for (int i = 0; i < length; i++) {
          if (i != next) {
            continue;
          }

          int nameEndsAt = i + playerName.length();

          // Replace the name multiple times in the same text component
          next = text.indexOf(playerName, nameEndsAt);

          // Skip when player name is not at the start and the character in front of the name is a valid name character
          if (i != 0) {
            char character = text.charAt(i - 1);

            if ((character >= 'A' && character <= 'Z') ||
                (character >= 'a' && character <= 'z') ||
                (character >= '0' && character <= '9') ||
                character == '_') {
              continue;
            }
          }

          // Skip when player name is not at the end and the character after the name is a valid name character
          if (nameEndsAt < length) {
            char character = text.charAt(nameEndsAt);

            if ((character >= 'A' && character <= 'Z') ||
                (character >= 'a' && character <= 'z') ||
                (character >= '0' && character <= '9') ||
                character == '_') {
              continue;
            }
          }

          if (i > lastNameAt) {
            Component spacerComponent = Component.text(text.substring(lastNameAt, i));
            spacerComponent.style(spacerComponent.style().merge(style, Strategy.IF_ABSENT_ON_TARGET));
            component.append(childIndex++, spacerComponent);
          }

          Component customNameComponent = customName.get();
          customNameComponent.style(customNameComponent.style().merge(style, Strategy.IF_ABSENT_ON_TARGET));
          component.append(childIndex++, customNameComponent);
          lastNameAt = nameEndsAt;

          // Skip unnecessary loop
          if (next == -1) {
            break;
          }
        }

        // no way to properly check for this in chat
        if (lastNameAt < length) {
          Component spacerComponent = Component.text(text.substring(lastNameAt));
          spacerComponent.style(spacerComponent.style().merge(style, Strategy.IF_ABSENT_ON_TARGET));
          component.append(childIndex, spacerComponent);
        }
      }
    }

    return replaced;
  }

  public Component replaceLegacyContext(Component component) {
    List<Component> children = new ArrayList<>();
    for (Component child : component.getChildren()) {
      children.add(this.replaceLegacyContext(child));
    }
    component.setChildren(children);

    if (component instanceof TranslatableComponent translatableComponent) {
      List<Component> arguments = new ArrayList<>();
      for (Component argument : translatableComponent.getArguments()) {
        arguments.add(this.replaceLegacyContext(argument));
      }
      translatableComponent.arguments(arguments);
    }

    if (component instanceof TextComponent textComponent) {
      String text = textComponent.getText();

      if (text.indexOf('§') != -1) {
        Style style = component.style();
        component = LegacyComponentSerializer.legacySection().deserialize(text);
        component.style(component.style().merge(style, Strategy.IF_ABSENT_ON_TARGET));
      }
    }

    return component;
  }
}
