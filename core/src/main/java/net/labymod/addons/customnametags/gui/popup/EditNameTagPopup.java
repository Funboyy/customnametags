package net.labymod.addons.customnametags.gui.popup;

import net.labymod.addons.customnametags.CustomNameTag;
import net.labymod.addons.customnametags.CustomNameTagsConfiguration;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.serializer.legacy.LegacyComponentSerializer;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget.State;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.FlexibleContentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.popup.SimpleAdvancedPopup;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.client.render.font.TextColorStripper;
import net.labymod.api.event.client.gui.screen.playerlist.PlayerListUpdateEvent;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Link("manage.lss")
public class EditNameTagPopup extends SimpleAdvancedPopup {
  private static final Pattern NAME_PATTERN = Pattern.compile("\\w{0,16}");
  private static final TextColorStripper TEXT_COLOR_STRIPPER = Laby.references()
      .textColorStripper();

  private String lastUserName;
  private final SimplePopupButton confirmButton;

  private TextFieldWidget mcNameInput;
  private TextFieldWidget customNameInput;
  private CheckBoxWidget enabledCheckBox;
  private CheckBoxWidget replaceCheckBox;

  public EditNameTagPopup(@NotNull CustomNameTag nameTag, @NotNull CustomNameTagsConfiguration config, @NotNull Consumer<CustomNameTag> onDataChange) {
    DivWidget inputContainer = new DivWidget();
    inputContainer.addId("input-container");

    ComponentWidget customNamePreview = ComponentWidget.component(nameTag.displayName());
    customNamePreview.addId("custom-preview");
    inputContainer.addChild(customNamePreview);

    FlexibleContentWidget inputWidget = new FlexibleContentWidget();
    inputWidget.addId("input-list");

    //Build areas of popup
    this.buildOriginalInputArea(inputWidget, nameTag);
    this.buildCustomNameInputArea(inputWidget, customNamePreview, nameTag);
    this.buildCheckboxArea(inputWidget, nameTag);

    /////////////////////////////////////////////// Popup logic

    inputContainer.addChild(inputWidget);

    super.title = Component.translatable("customnametags.gui.manage." + (nameTag.getOriginalName().isEmpty() ? "add" : "edit"));
    super.widgetFunction = container -> {
      container.addChild(inputContainer);
    };

    super.buttons = new ArrayList<>();
    this.confirmButton = SimplePopupButton.create(
        Component.translatable("labymod.ui.button.done"),
        ignored -> {
          nameTag.setEnabled(this.enabledCheckBox.state() == State.CHECKED);
          nameTag.setReplaceScoreboard(this.replaceCheckBox.state() == State.CHECKED);
          nameTag.setOriginalName(mcNameInput.getText());
          nameTag.setCustomName(customNameInput.getText());
          Map<String, CustomNameTag> customTags = config.getCustomTags();
          customTags.remove(nameTag.getOriginalName()); //Remove for the case the username was changed (and therefore the key)
          customTags.put(nameTag.getOriginalName(), nameTag);
          onDataChange.accept(nameTag);
          Laby.fireEvent(new PlayerListUpdateEvent()); //Make sure the changes are displayed immediately
        }
    );
    super.buttons.add(confirmButton);
    super.buttons.add(SimplePopupButton.cancel());

    super.displayInOverlay();
  }

  private void buildOriginalInputArea(@NotNull FlexibleContentWidget parent, @NotNull CustomNameTag nameTag) {
    IconWidget icon = new IconWidget(
        this.getPlayerHead(nameTag.getOriginalName())
    );

    this.mcNameInput = this.buildInputArea(
        parent,
        "customnametags.gui.manage.name",
        icon,
        nameTag.getOriginalName(),
        newValue -> {
          icon.icon().set(this.getPlayerHead(newValue));
        }
    );
  }

  private void buildCustomNameInputArea(@NotNull FlexibleContentWidget parent, @NotNull ComponentWidget customNamePreview, @NotNull CustomNameTag nameTag) {
    this.customNameInput = this.buildInputArea(
        parent,
        "customnametags.gui.manage.custom.name",
        new DivWidget(), //Placeholder
        nameTag.getCustomName(),
        newValue -> {
          customNamePreview.setComponent(
              LegacyComponentSerializer.legacyAmpersand().deserialize(newValue)
          );
        }
    );
  }

  private @NotNull TextFieldWidget buildInputArea(
      @NotNull FlexibleContentWidget parent,
      @NotNull String translationKey,
      @NotNull Widget iconWidget,
      @NotNull String defaultValue,
      @NotNull Consumer<String> changeConsumer
  ) {
    ComponentWidget label = ComponentWidget.i18n(translationKey);
    label.addId("label-name");
    parent.addContent(label);

    HorizontalListWidget list = new HorizontalListWidget();
    list.addId("input-name-list");

    iconWidget.addId("input-avatar");
    list.addEntry(iconWidget);

    TextFieldWidget input = new TextFieldWidget();
    input.maximalLength(64);
    input.setText(defaultValue);
    input.updateListener(newValue -> {
      this.updateConfirmButtonState();

      changeConsumer.accept(newValue);
    });

    list.addEntry(input);
    parent.addContent(list);

    return input;
  }

  private void buildCheckboxArea(@NotNull FlexibleContentWidget parent, @NotNull CustomNameTag nameTag) {
    FlexibleContentWidget checkBoxList = new FlexibleContentWidget();
    checkBoxList.addId("checkbox-list");

    this.enabledCheckBox = this.buildSingleCheckboxes(checkBoxList, "customnametags.gui.manage.enabled.name", nameTag.isEnabled());
    this.replaceCheckBox = this.buildSingleCheckboxes(checkBoxList, "customnametags.gui.manage.replace.name", nameTag.isReplaceScoreboard());

    parent.addContent(checkBoxList);
  }

  private @NotNull CheckBoxWidget buildSingleCheckboxes(@NotNull FlexibleContentWidget parent, @NotNull String translationKey, boolean defaultValue) {
    FlexibleContentWidget div = new FlexibleContentWidget();
    div.addId("checkbox-div");

    ComponentWidget text = ComponentWidget.i18n(translationKey);
    text.addId("checkbox-name");

    CheckBoxWidget checkBox = new CheckBoxWidget();
    checkBox.addId("checkbox-item");
    checkBox.setState(defaultValue ? State.CHECKED : State.UNCHECKED);

    div.addContent(checkBox);
    div.addContent(text);

    parent.addContent(div);

    return checkBox;
  }

  private void updateConfirmButtonState() {
    this.confirmButton.enabled(
        !this.mcNameInput.getText().isBlank() && !this.customNameInput.getText().isBlank()
    );
  }

  private @NotNull Icon getPlayerHead(@NotNull String userName) {
    return Icon.head(userName.isEmpty() ? "MHF_Question" : userName);
  }
}
