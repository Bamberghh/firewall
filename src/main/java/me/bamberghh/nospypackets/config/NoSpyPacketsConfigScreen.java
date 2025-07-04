package me.bamberghh.nospypackets.config;

import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.ListOptionContainer;
import me.bamberghh.nospypackets.NoSpyPackets;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class NoSpyPacketsConfigScreen extends ConfigScreen {
    protected NoSpyPacketsConfigScreen(Identifier modelId, ConfigWrapper<?> config, @Nullable Screen parent) {
        super(modelId, config, parent);
        extraFactories.put(option -> option.clazz() == Set.class, (model, option) -> {
            var layout = new SetOptionContainer(option);
            return new OptionComponentFactory.Result(layout, layout);
        });
    }

    public static NoSpyPacketsConfigScreen create(ConfigWrapper<?> config, @Nullable Screen parent) {
        return new NoSpyPacketsConfigScreen(DEFAULT_MODEL_ID, config, parent);
    }

    public static NoSpyPacketsConfigScreen createWithCustomModel(Identifier modelId, ConfigWrapper<?> config, @Nullable Screen parent) {
        return new NoSpyPacketsConfigScreen(modelId, config, parent);
    }
}
