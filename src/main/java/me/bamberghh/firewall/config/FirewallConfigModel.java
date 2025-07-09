package me.bamberghh.firewall.config;

import io.wispforest.endec.Endec;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Nest;
import me.bamberghh.firewall.config.annotation.Computed;
import me.bamberghh.firewall.util.AndStringFilter;
import me.bamberghh.firewall.util.SimpleStringFilter;
import me.bamberghh.firewall.util.StringFilter;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Config(name = "firewall-config", wrapperName = "FirewallConfig", defaultHook = true)
public class FirewallConfigModel {
    private static class MergeFilters implements Function<Object[], Object> {
        @Override
        public Object apply(Object[] objects) {
            return AndStringFilter.of(Arrays.copyOf(objects, objects.length, StringFilter[].class));
        }
    }
    private static class MergeFiltersWithBool implements Function<Object[], Object> {
        @Override
        public Object apply(Object[] objects) {
            assert objects.length == 5;
            var comm = (StringFilter) objects[0];
            var sendOrRecv = (StringFilter) objects[1];
            var useOther = (boolean) objects[2];
            var otherComm = (StringFilter) objects[3];
            var otherSendOrRecv = (StringFilter) objects[4];
            if (useOther) {
                return AndStringFilter.of(comm, sendOrRecv, otherComm, otherSendOrRecv);
            } else {
                return AndStringFilter.of(comm, sendOrRecv);
            }
        }
    }
    public static class SendRecvStringFilter {
        public SimpleStringFilter comm = SimpleStringFilter.blacklist();
        public SimpleStringFilter send = SimpleStringFilter.blacklist();
        public SimpleStringFilter recv = SimpleStringFilter.blacklist();

        @Computed(inputs = {"comm", "send"}, output = MergeFilters.class)
        public transient StringFilter sendMerged = null;
        @Computed(inputs = {"comm", "recv"}, output = MergeFilters.class)
        public transient StringFilter recvMerged = null;

        public SendRecvStringFilter() {}

        public SendRecvStringFilter(SimpleStringFilter comm, SimpleStringFilter send, SimpleStringFilter recv) {
            this.comm = comm;
            this.send = send;
            this.recv = recv;
        }
    }
    public static class CustomPayloadIdentifiers {
        public boolean useFromRegister = false;
        public SimpleStringFilter comm = SimpleStringFilter.blacklist();
        public SimpleStringFilter send = SimpleStringFilter.blacklist();
        public SimpleStringFilter recv = SimpleStringFilter.blacklist();

        @Computed(inputs = {"comm", "send", "useFromRegister", "/registerIdentifiers/comm", "/registerIdentifiers/send"}, output = MergeFiltersWithBool.class)
        public transient StringFilter sendMerged = null;
        @Computed(inputs = {"comm", "recv", "useFromRegister", "/registerIdentifiers/comm", "/registerIdentifiers/recv"}, output = MergeFiltersWithBool.class)
        public transient StringFilter recvMerged = null;
    }
    public static class RegisterIdentifiers {
        public boolean useFromCustomPayload = true;
        public SimpleStringFilter comm = SimpleStringFilter.blacklist();
        public SimpleStringFilter send = SimpleStringFilter.blacklist();
        public SimpleStringFilter recv = SimpleStringFilter.blacklist();
        public boolean sendEmptyChannelLists = false;
        public boolean recvEmptyChannelLists = true;

        @Computed(inputs = {"comm", "send", "useFromCustomPayload", "/customPayloadIdentifiers/comm", "/customPayloadIdentifiers/send"}, output = MergeFiltersWithBool.class)
        public transient StringFilter sendMerged = null;
        @Computed(inputs = {"comm", "recv", "useFromCustomPayload", "/customPayloadIdentifiers/comm", "/customPayloadIdentifiers/recv"}, output = MergeFiltersWithBool.class)
        public transient StringFilter recvMerged = null;
    }
    @Nest public SendRecvStringFilter packetIdentifiers = new SendRecvStringFilter();
    @Nest public CustomPayloadIdentifiers customPayloadIdentifiers = new CustomPayloadIdentifiers();
    @Nest public RegisterIdentifiers registerIdentifiers = new RegisterIdentifiers();

    public boolean shouldOverwriteBrand = false;
    public String brandOverwriteValue = "vanilla";

    @Nest public SendRecvStringFilter loggedPacketIdentifiers = new SendRecvStringFilter(
            SimpleStringFilter.whitelist(),
            SimpleStringFilter.blacklist(),
            SimpleStringFilter.blacklist());
    @Nest public SendRecvStringFilter loggedCustomPayloadIdentifiers = new SendRecvStringFilter(
            SimpleStringFilter.whitelist(),
            SimpleStringFilter.blacklist(),
            SimpleStringFilter.blacklist());

    public static void builderConsumer(ConfigWrapper.SerializationBuilder builder) {
        builder.addEndec(Pattern.class, Endec.STRING.xmap(
                Pattern::compile,
                Pattern::pattern));
    }
}
