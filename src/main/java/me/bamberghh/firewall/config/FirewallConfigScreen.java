package me.bamberghh.firewall.config;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import me.bamberghh.firewall.util.SimpleStringFilter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FirewallConfigScreen extends ConfigScreen {
    protected FirewallConfigScreen(Identifier modelId, ConfigWrapper<?> config, @Nullable Screen parent) {
        super(modelId, config, parent);
        extraFactories.put(option -> option.clazz() == Set.class, (model, option) -> {
            var layout = new SetOptionContainer(option);
            return new OptionComponentFactory.Result<>(layout, layout);
        });
        extraFactories.put(option -> option.clazz() == SimpleStringFilter.class, (model, option) -> {
            var layout = new StringFilterContainer(model, (Option<SimpleStringFilter>) (Object) option);
            return new OptionComponentFactory.Result<>(layout, layout);
        });
    }

    public static FirewallConfigScreen create(ConfigWrapper<?> config, @Nullable Screen parent) {
        return new FirewallConfigScreen(DEFAULT_MODEL_ID, config, parent);
    }

    public static FirewallConfigScreen createWithCustomModel(Identifier modelId, ConfigWrapper<?> config, @Nullable Screen parent) {
        return new FirewallConfigScreen(modelId, config, parent);
    }
}
