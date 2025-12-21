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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.labymod.addons.customnametags.CustomNameTag;
import net.labymod.addons.customnametags.CustomNameTags;
import net.labymod.addons.customnametags.gui.popup.EditNameTagPopup;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.serializer.legacy.LegacyComponentSerializer;
import net.labymod.api.client.gui.mouse.MutableMouse;
import net.labymod.api.client.gui.screen.LabyScreen;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Activity;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.key.InputType;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.key.MouseButton;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget.State;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.FlexibleContentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.popup.SimpleAdvancedPopup;
import net.labymod.api.client.gui.screen.widget.widgets.popup.SimpleAdvancedPopup.SimplePopupButton;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.client.render.font.TextColorStripper;
import net.labymod.api.event.client.gui.screen.playerlist.PlayerListUpdateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoActivity
@Link("manage.lss")
@Link("overview.lss")
public class NameTagActivity extends Activity {

  private static final Pattern NAME_PATTERN = Pattern.compile("\\w{0,16}");
  private static final TextColorStripper TEXT_COLOR_STRIPPER = Laby.references()
      .textColorStripper();

  private final CustomNameTags addon;
  private final VerticalListWidget<NameTagWidget> nameTagList;
  private final Map<String, NameTagWidget> nameTagWidgets;

  private CustomNameTag selectedNameTag;

  private ButtonWidget removeButton;
  private ButtonWidget editButton;

  private FlexibleContentWidget inputWidget;
  private String lastUserName;
  private String lastCustomName;

  private Action action;
  private boolean updateRequired;

  public NameTagActivity() {
    this.addon = CustomNameTags.get();

    this.nameTagWidgets = new HashMap<>();
    this.loadNameTags();

    this.nameTagList = new VerticalListWidget<>();
    this.nameTagList.addId("name-tag-list");
    this.nameTagList.setSelectCallback(nameTagWidget -> {
      NameTagWidget selectedNameTag = this.nameTagList.session().getSelectedEntry();
      if (selectedNameTag == null
          || selectedNameTag.getCustomTag() != nameTagWidget.getCustomTag()) {
        this.editButton.setEnabled(true);
        this.removeButton.setEnabled(true);
      }
    });

    this.nameTagList.setDoubleClickCallback(nameTagWidget -> this.performAction(Action.EDIT));
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    FlexibleContentWidget container = new FlexibleContentWidget();
    container.addId("name-tag-container");
    for (NameTagWidget nameTagWidget : this.nameTagWidgets.values()) {
      this.nameTagList.addChild(nameTagWidget);
    }

    container.addFlexibleContent(new ScrollWidget(this.nameTagList));
    NameTagWidget selectedEntry = this.nameTagList.session().getSelectedEntry();
    if (selectedEntry != null) {
      this.selectedNameTag = selectedEntry.getCustomTag();
    } else {
      this.selectedNameTag = null;
    }
    HorizontalListWidget menu = new HorizontalListWidget();
    menu.addId("overview-button-menu");

    menu.addEntry(ButtonWidget.i18n("labymod.ui.button.add", () -> this.performAction(Action.ADD)));

    this.editButton = ButtonWidget.i18n("labymod.ui.button.edit",
        () -> this.performAction(Action.EDIT));
    this.editButton.setEnabled(this.selectedNameTag != null);
    menu.addEntry(this.editButton);

    this.removeButton = ButtonWidget.i18n("labymod.ui.button.remove",
        () -> this.performAction(Action.REMOVE));
    this.removeButton.setEnabled(this.selectedNameTag != null);
    menu.addEntry(this.removeButton);

    container.addContent(menu);
    this.document().addChild(container);
  }

  private String getStrippedText(String text) {
    text = text.trim();
    if (text.isEmpty()) {
      return text;
    }

    return TEXT_COLOR_STRIPPER.stripColorCodes(text, '&');
  }

  @Override
  public boolean mouseClicked(MutableMouse mouse, MouseButton mouseButton) {
    try {
      if (this.action != null) {
        return this.inputWidget.mouseClicked(mouse, mouseButton);
      }

      return super.mouseClicked(mouse, mouseButton);
    } finally {
      NameTagWidget selectedEntry = this.nameTagList.session().getSelectedEntry();
      if (selectedEntry != null) {
        this.selectedNameTag = selectedEntry.getCustomTag();
      } else {
        this.selectedNameTag = null;
      }
      this.removeButton.setEnabled(this.selectedNameTag != null);
      this.editButton.setEnabled(this.selectedNameTag != null);
    }
  }

  @Override
  public void reload() {
    this.loadNameTags();
    super.reload();
  }

  private void loadNameTags() {
    this.nameTagWidgets.clear();
    this.addon.configuration().getCustomTags().forEach((userName, customTag) -> {
      this.nameTagWidgets.put(userName, new NameTagWidget(customTag));
    });
  }

  private void performAction(@NotNull Action action) {
    switch (action) {
      case ADD -> {
        new EditNameTagPopup(CustomNameTag.ofDefault(), this.addon.configuration(), customNameTag -> {
          this.selectedNameTag = customNameTag;
          this.reload();
        });
      }
      case EDIT -> {
        new EditNameTagPopup(
            this.nameTagList.listSession().getSelectedEntry().getCustomTag(),
            this.addon.configuration(),
            ignored -> this.reload()
        );
      }
      case REMOVE -> {
        SimpleAdvancedPopup.builder()
            .title(Component.translatable("customnametags.gui.manage.remove.title"))
            .description(Component.translatable("customnametags.gui.manage.remove.description").argument(Component.text(this.selectedNameTag.getOriginalName())))
            .addButton(SimplePopupButton.confirm(simplePopupButton -> {
              this.addon.configuration().getCustomTags().remove(this.selectedNameTag.getOriginalName());
              this.reload();
              Laby.fireEvent(new PlayerListUpdateEvent()); //Make sure the changes are displayed immediately
            }))
            .addButton(SimplePopupButton.cancel())
            .build()
            .displayInOverlay();
      }
    }
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    if (this.updateRequired) {
      this.addon.reloadTabList();
    }
  }

  private enum Action {
    ADD, EDIT, REMOVE
  }
}
