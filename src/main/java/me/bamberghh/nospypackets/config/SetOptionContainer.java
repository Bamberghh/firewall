package me.bamberghh.nospypackets.config;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.config.ui.component.OptionValueProvider;
import io.wispforest.owo.config.ui.component.SearchAnchorComponent;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.UISounds;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import me.bamberghh.nospypackets.util.IndexHashSet;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

// Copied from io.wispforest.owo.config.ui.component.ListOptionContainer
public class SetOptionContainer<T> extends CollapsibleContainer implements OptionValueProvider {

    protected final Option<Set<T>> backingOption;
    protected final ArrayList<T> backingList;
    protected boolean backingSetDirty = true;
    protected final IndexHashSet<T> backingSet = new IndexHashSet<>();

    protected final ButtonWidget resetButton;

    @SuppressWarnings("unchecked")
    public SetOptionContainer(Option<Set<T>> option) {
        super(
                Sizing.fill(100), Sizing.content(),
                Text.translatable("text.config." + option.configName() + ".option." + option.key().asString()),
                option.backingField().field().isAnnotationPresent(Expanded.class)
        );

        this.backingOption = option;
        this.backingList = new ArrayList<>(option.value());

        this.padding(this.padding.get().add(0, 5, 0, 0));

        this.titleLayout.horizontalSizing(Sizing.fill(100));
        this.titleLayout.verticalSizing(Sizing.fixed(30));
        this.titleLayout.verticalAlignment(VerticalAlignment.CENTER);

        if (!option.detached()) {
            this.titleLayout.child(Components.label(Text.translatable("text.owo.config.list.add_entry").formatted(Formatting.GRAY)).<LabelComponent>configure(label -> {
                label.cursorStyle(CursorStyle.HAND);

                label.mouseEnter().subscribe(() -> label.text(label.text().copy().styled(style -> style.withColor(Formatting.YELLOW))));
                label.mouseLeave().subscribe(() -> label.text(label.text().copy().styled(style -> style.withColor(Formatting.GRAY))));
                label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    UISounds.playInteractionSound();
                    this.backingList.add((T) "");
                    backingSetMarkDirty();

                    if (!this.expanded) this.toggleExpansion();
                    this.refreshOptions();

                    var lastEntry = (ParentComponent) this.collapsibleChildren.getLast();
                    assert this.focusHandler() != null;
                    this.focusHandler().focus(
                            lastEntry.children().getLast(),
                            FocusSource.MOUSE_CLICK
                    );

                    return true;
                });
            }));
        }

        this.resetButton = Components.button(Text.literal("⇄"), (ButtonComponent button) -> {
            this.backingList.clear();
            this.backingList.addAll(option.defaultValue());
            backingSetMarkDirty();

            this.refreshOptions();
            button.active = false;
        });
        this.resetButton.margins(Insets.right(10));
        this.resetButton.positioning(Positioning.relative(100, 50));
        this.titleLayout.child(resetButton);
        this.refreshResetButton();

        this.refreshOptions();

        this.titleLayout.child(new SearchAnchorComponent(
                this.titleLayout,
                option.key(),
                () -> I18n.translate("text.config." + option.configName() + ".option." + option.key().asString()),
                () -> this.backingList.stream().map(Objects::toString).collect(Collectors.joining())
        ));
    }

    protected void backingSetMarkDirty() {
        this.backingSetDirty = true;
    }

    protected IndexHashSet<T> backingSet() {
        if (!this.backingSetDirty) {
            return this.backingSet;
        }
        this.backingSet.clear();
        this.backingSet.addAll(this.backingList);
        this.backingSetDirty = false;
        return this.backingSet;
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    protected void refreshOptions() {
        this.collapsibleChildren.clear();

        var setType = ReflectionUtils.getTypeArgument(this.backingOption.backingField().field().getGenericType(), 0);
        for (int currentIndex = 0; currentIndex < this.backingList.size(); currentIndex++) {
            int optionIndex = currentIndex;

            var container = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            container.verticalAlignment(VerticalAlignment.CENTER);

            final var label = Components.label(TextOps.withFormatting("- ", Formatting.GRAY));
            label.margins(Insets.left(10));
            if (!this.backingOption.detached()) {
                label.cursorStyle(CursorStyle.HAND);
                label.mouseEnter().subscribe(() -> label.text(TextOps.withFormatting("x ", Formatting.GRAY)));
                label.mouseLeave().subscribe(() -> label.text(TextOps.withFormatting("- ", Formatting.GRAY)));
                label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    this.backingList.remove(optionIndex);
                    backingSetMarkDirty();
                    this.refreshResetButton();
                    this.refreshOptions();
                    UISounds.playInteractionSound();

                    return true;
                });
            }
            container.child(label);

            final var box = new ConfigTextBox();
            box.setText(this.backingList.get(optionIndex).toString());
            box.setCursorToStart(false);
            box.setDrawsBackground(false);
            box.margins(Insets.vertical(2));
            box.horizontalSizing(Sizing.fill(95));
            box.verticalSizing(Sizing.fixed(8));

            if (!this.backingOption.detached()) {
                box.onChanged().subscribe(s -> {
                    if (!box.isValid()) return;

                    this.backingList.set(optionIndex, (T) box.parsedValue());
                    backingSetMarkDirty();
                    this.refreshResetButton();
                });
            } else {
                box.active = false;
            }

            if (NumberReflection.isNumberType(setType)) {
                box.configureForNumber((Class<? extends Number>) setType);
            }

            container.child(box);
            this.collapsibleChildren.add(container);
        }

        this.contentLayout.<FlowLayout>configure(layout -> {
            layout.clearChildren();
            if (this.expanded) layout.children(this.collapsibleChildren);
        });
        this.refreshResetButton();
    }

    protected void refreshResetButton() {
        this.resetButton.active = !this.backingOption.detached() && !backingSet().equals(this.backingOption.defaultValue());
    }

    @Override
    public boolean shouldDrawTooltip(double mouseX, double mouseY) {
        return ((mouseY - this.y) <= this.titleLayout.height()) && super.shouldDrawTooltip(mouseX, mouseY);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Object parsedValue() {
        return backingSet();
    }
}
