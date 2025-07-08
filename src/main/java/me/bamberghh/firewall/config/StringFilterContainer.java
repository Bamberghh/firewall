package me.bamberghh.firewall.config;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Expanded;
import io.wispforest.owo.config.ui.component.OptionValueProvider;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.util.Observable;
import me.bamberghh.firewall.Firewall;
import me.bamberghh.firewall.util.IndexHashSet;
import me.bamberghh.firewall.util.SimpleStringFilter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringFilterContainer extends CollapsibleContainer implements OptionValueProvider {
    public static class KindButton extends ButtonComponent implements OptionValueProvider {
        @Nullable protected Option<SimpleStringFilter> backingOption = null;
        @Nullable protected Enum<?>[] backingValues = null;
        protected int selectedIndex = 0;

        protected boolean wasRightClicked = false;

        public KindButton() {
            super(Text.empty(), button -> {});
            this.verticalSizing(Sizing.fixed(20));
            this.horizontalSizing(Sizing.expand());
            this.updateMessage();
        }

        @Override
        public boolean onMouseDown(double mouseX, double mouseY, int button) {
            this.wasRightClicked = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            return super.onMouseDown(mouseX, mouseY, button);
        }

        @Override
        public void onPress() {
            if (this.wasRightClicked || Screen.hasShiftDown()) {
                this.selectedIndex--;
                if (this.selectedIndex < 0) this.selectedIndex += this.backingValues.length;
            } else {
                this.selectedIndex++;
                if (this.selectedIndex > this.backingValues.length - 1) this.selectedIndex -= this.backingValues.length;
            }

            this.updateMessage();

            super.onPress();
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT || super.isValidClickButton(button);
        }

        protected void updateMessage() {
            if (this.backingOption == null) return;

            var enumName = StringUtils.uncapitalize(this.backingValues.getClass().componentType().getSimpleName());
            assert this.backingValues[this.selectedIndex] != null;
            var valueName = this.backingValues[this.selectedIndex].name().toLowerCase(Locale.ROOT);

            this.setMessage(Text.translatable("text.config." + this.backingOption.configName() + ".stringfilter.kind." + valueName));
        }

        public KindButton init(Option<SimpleStringFilter> option, int selectedIndex) {
            this.backingOption = option;
            this.backingValues = SimpleStringFilter.Kind.class.getEnumConstants();
            this.selectedIndex = selectedIndex;

            this.updateMessage();

            return this;
        }

        public KindButton select(int index) {
            this.selectedIndex = index;
            this.updateMessage();

            return this;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        public SimpleStringFilter.Kind value() {
            return (SimpleStringFilter.Kind) this.backingValues[this.selectedIndex];
        }

        @Override
        public Object parsedValue() {
            return value();
        }
    }

    protected KindButton kindButton;
    protected SetOptionContainer<String> subValueContainer;
    protected TextBoxComponent regexTextBoxComponent;

    public StringFilterContainer(UIModel model, Option<SimpleStringFilter> option) {
        super(
                Sizing.fill(100), Sizing.content(),
                Text.translatable("text.config." + option.configName() + ".option." + option.key().asString()),
                option.backingField().field().isAnnotationPresent(Expanded.class)
        );

        var filter = option.value();

        kindButton = new KindButton().init(option, filter.kind.ordinal());
        kindButton.onPress(buttonComponent -> {
            this.collapsibleChildren.remove(1);
            if (kindButton.value() == SimpleStringFilter.Kind.Regex) {
                this.collapsibleChildren.add(regexTextBoxComponent);
            } else {
                this.collapsibleChildren.add(subValueContainer);
            }
            this.contentLayout.<FlowLayout>configure(layout -> {
                layout.clearChildren();
                if (this.expanded) layout.children(this.collapsibleChildren);
            });
        });

        var subValueObservable = Observable.of((Set<String>) filter.list);

        Field subValueObservableField;
        try {
            subValueObservableField = (new Object() {
                @Expanded
                Set<String> value;
            }).getClass().getDeclaredField("value");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        // This is horrible but whatever
        subValueContainer = new SetOptionContainer<>(new Option<>(
                "",
                new Option.Key(""),
                new IndexHashSet<>(),
                subValueObservable,
                new Option.BoundField<>(subValueObservable, subValueObservableField),
                null,
                Option.SyncMode.NONE,
                null));

        subValueContainer
                .titleLayout()
                .children()
                .getFirst()
                .<LabelComponent>configure(
                        label -> label.text(Text.translatable(String.format("text.config.%s.stringfilter.list", Firewall.CONFIG.name()))));

        regexTextBoxComponent = Components.textBox(Sizing.expand());
        regexTextBoxComponent.setMaxLength(Integer.MAX_VALUE);
        regexTextBoxComponent.text(filter.regex.pattern());

        this.collapsibleChildren.add(kindButton);
        if (kindButton.value() == SimpleStringFilter.Kind.Regex) {
            this.collapsibleChildren.add(regexTextBoxComponent);
        } else {
            this.collapsibleChildren.add(subValueContainer);
        }
        this.contentLayout.<FlowLayout>configure(layout -> {
            layout.clearChildren();
            if (this.expanded) layout.children(this.collapsibleChildren);
        });
    }

    @Override
    public boolean shouldDrawTooltip(double mouseX, double mouseY) {
        return ((mouseY - this.y) <= this.titleLayout.height());
    }

    @Override
    public boolean isValid() {
        try {
            Pattern.compile(regexTextBoxComponent.getText());
        } catch (PatternSyntaxException e) {
            Firewall.LOGGER.info("firewall: bad regex {}: {}", regexTextBoxComponent.getText(), e);
            return false;
        }
        return true;
    }

    @Override
    public Object parsedValue() {
        var filter = new SimpleStringFilter();
        filter.kind = kindButton.value();
        filter.list = subValueContainer.value();
        filter.regex = Pattern.compile(regexTextBoxComponent.getText());
        return filter;
    }
}
