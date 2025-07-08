package me.bamberghh.firewall.config;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.Nest;
import io.wispforest.owo.util.Observable;
import me.bamberghh.firewall.config.annotation.Computed;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigInit {
    private final me.bamberghh.firewall.config.FirewallConfig config;
    private final FirewallConfigModel model;
    private final HashMap<Option.Key, Option<Object>> internalOptions = new HashMap<>();

    private ConfigInit(me.bamberghh.firewall.config.FirewallConfig config) throws IllegalAccessException {
        this.config = config;
        model = (FirewallConfigModel) FieldUtils.readField(config, "instance", true);
    };

    public static void init(me.bamberghh.firewall.config.FirewallConfig config)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        ConfigInit self = new ConfigInit(config);

        // Create the missing options for transient fields
        self.createMissingOptions();

        // Create the computed field updaters & do the initial update
        self.initComputedFields(Option.Key.ROOT, self.model);
    }

    private @Nullable Option<Object> optionForKey(Option.Key key) {
        var option = config.optionForKey(key);
        if (option != null) {
            return option;
        }
        return internalOptions.get(key);
    }

    private void createMissingOptions() throws IllegalAccessException {
        for (var keyField : config.keys.getClass().getFields()) {
            Option.Key key = (Option.Key) keyField.get(config.keys);
            if (config.optionForKey(key) != null) {
                continue;
            }
            Object backingFieldObject = model;
            for (int pathComponentI = 0; pathComponentI < key.path().length - 1; pathComponentI++) {
                var pathComponent = key.path()[pathComponentI];
                backingFieldObject = FieldUtils.readField(backingFieldObject, pathComponent);
            }
            Field backingFieldField = FieldUtils.getField(backingFieldObject.getClass(), key.path()[key.path().length - 1]);
            var backingField = new Option.BoundField<>(backingFieldObject, backingFieldField);
            var defaultValue = backingField.getValue();
            var observable = Observable.of(defaultValue);
            var option = new Option<>(
                    config.name(),
                    key,
                    defaultValue,
                    observable,
                    backingField,
                    null,
                    Option.SyncMode.NONE,
                    null);
            FieldUtils.writeField(config, String.join("_", key.path()), option, true);
            //noinspection unchecked
            internalOptions.put(key, (Option<Object>) (Option<?>) option);
        }
    }

    private Option<?> resolveOptionPath(List<String> originPath, String path) {
        var pathSplit = path.split("/");
        ArrayList<String> optionPath;
        int componentI = 0;
        if (pathSplit[0].isEmpty()) {
            optionPath = new ArrayList<>();
            componentI = 1;
        } else {
            optionPath = new ArrayList<>(originPath);
        }
        for (; componentI < pathSplit.length; componentI++) {
            var component = pathSplit[componentI];
            if (component.isEmpty() || component.equals(".")) {
                continue;
            }
            if (component.equals("..")) {
                if (!optionPath.isEmpty()) {
                    optionPath.removeLast();
                }
                continue;
            }
            optionPath.add(component);
        }
        return optionForKey(new Option.Key(optionPath));
    }

    private void initComputedFields(Option.Key parent, Object model) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        for (Field field : model.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;

            if (field.isAnnotationPresent(Nest.class)) {
                var fieldValue = field.get(model);
                if (fieldValue != null) {
                    initComputedFields(parent.child(field.getName()), fieldValue);
                } else {
                    throw new IllegalStateException("Nested config option containers must never be null");
                }
                continue;
            }
            var computed = field.getAnnotation(Computed.class);
            if (computed == null) {
                continue;
            }
            var optionKey = parent.child(field.getName());
            var option = Objects.requireNonNull(optionForKey(optionKey));
            var inputs = Arrays.stream(computed.inputs())
                    .map(input -> resolveOptionPath(Arrays.asList(parent.path()), input))
                    .toList();
            var outputClass = computed.output();
            var outputConstructor = outputClass.getDeclaredConstructor();
            outputConstructor.setAccessible(true);
            var output = outputConstructor.newInstance();
            var outputMethod = outputClass.getMethod("apply", Object[].class);
            Runnable updateOption = () -> {
                try {
                    option.set(outputMethod.invoke(output, new Object[] { inputs.stream().map(Option::value).toArray() }));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
            for (var input : inputs) {
                input.observe(_value -> updateOption.run());
            }
            updateOption.run();
        }
    }
}
