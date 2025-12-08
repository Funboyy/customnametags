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

package net.labymod.addons.customnametags.gui.activity;

import net.labymod.addons.customnametags.CustomNameTag;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.widget.SimpleWidget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;

@AutoWidget
public class NameTagWidget extends SimpleWidget {

  private CustomNameTag customNameTag;

  public NameTagWidget(CustomNameTag customNameTag) {
    this.customNameTag = customNameTag;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    if(this.customNameTag.isEnabled()) {
      this.removeId("disabled");
    } else {
      this.addId("disabled");
    }

    IconWidget iconWidget = new IconWidget(this.getIconWidget(this.customNameTag.getOriginalName()));
    iconWidget.addId("avatar");
    this.addChild(iconWidget);

    ComponentWidget nameWidget = ComponentWidget.component(Component.text(this.customNameTag.getOriginalName()));
    nameWidget.addId("name");
    this.addChild(nameWidget);

    ComponentWidget customNameWidget = ComponentWidget.component(this.customNameTag.displayName());
    customNameWidget.addId("custom-name");
    this.addChild(customNameWidget);
  }

  public Icon getIconWidget(String userName) {
    return Icon.head(userName == null || userName.isBlank() ? "MHF_Question" : userName);
  }

  public String getUserName() {
    return this.customNameTag.getOriginalName();
  }

  public CustomNameTag getCustomTag() {
    return this.customNameTag;
  }

  public void setCustomTag(CustomNameTag customNameTag) {
    this.customNameTag = customNameTag;
  }
}
