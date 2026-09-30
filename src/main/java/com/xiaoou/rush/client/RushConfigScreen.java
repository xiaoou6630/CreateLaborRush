package com.xiaoou.rush.client;

import com.xiaoou.rush.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * 手写的图形化配置界面（零依赖，不依赖 Cloth Config 等第三方库）。
 *
 * <p>点"完成"会把改动写入本地并同步给服务端（联机时由服务端校验管理员权限）。
 * ESC 关闭则放弃修改。
 */
public class RushConfigScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int WIDGET_W = 160;
    private static final int MARGIN = 40;
    private static final int LABEL_COLOR = 0xE0E0E0;
    private static final int HEADER_COLOR = 0xFFD700;

    private final Screen parent;

    // 编辑中的配置值（点"完成"才写回）
    private boolean rebellionEnabled;
    private boolean canDestroyDevices;
    private double destroyChance;
    private double destroyRatioMin;
    private double destroyRatioMax;
    private double rebellionChance;
    private double destroyIntensity;
    private int rebellionTriggerTime;
    private int rebellionDuration;
    private int rebellionRadius;
    private int destroyCooldown;

    private final List<Row> rows = new ArrayList<>();
    private int scrollRow;
    private int contentTop;
    private int contentBottom;
    private int visibleRows = 1;

    public RushConfigScreen(Screen parent) {
        super(Component.translatable("createlaborrush.config.title"));
        this.parent = parent;

        Config.Values v = ConfigSyncClient.displayValues();
        this.destroyChance = v.destroyChance();
        this.destroyRatioMin = v.destroyRatioMin();
        this.destroyRatioMax = v.destroyRatioMax();
        this.rebellionEnabled = v.rebellionEnabled();
        this.rebellionTriggerTime = v.rebellionTriggerTime();
        this.rebellionChance = v.rebellionChance();
        this.rebellionDuration = v.rebellionDuration();
        this.rebellionRadius = v.rebellionRadius();
        this.canDestroyDevices = v.canDestroyDevices();
        this.destroyCooldown = v.destroyCooldown();
        this.destroyIntensity = v.destroyIntensity();

        buildRows();
    }

    // ===== 行定义 =====

    private Row option(String key, WidgetFactory factory) {
        return new Row(
            Component.translatable("createlaborrush.config." + key),
            Component.translatable("createlaborrush.config." + key + ".tooltip"),
            factory
        );
    }

    private void buildRows() {
        rows.clear();

        rows.add(Row.header("work"));
        rows.add(option("destroyChance", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            destroyChance, 0.0, 1.0, 0.01, 2, value -> destroyChance = value)));
        rows.add(option("destroyRatioMin", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            destroyRatioMin, 0.0, 1.0, 0.01, 2, value -> destroyRatioMin = value)));
        rows.add(option("destroyRatioMax", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            destroyRatioMax, 0.0, 1.0, 0.01, 2, value -> destroyRatioMax = value)));

        rows.add(Row.header("rebellion"));
        rows.add(option("enableRebellion", (x, y, w, h) -> toggle(x, y, w, h,
            rebellionEnabled, value -> rebellionEnabled = value)));
        rows.add(option("rebellionTriggerTime", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            rebellionTriggerTime, 10, 3600, 1, 0, value -> rebellionTriggerTime = (int) Math.round(value))));
        rows.add(option("rebellionChance", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            rebellionChance, 0.0, 1.0, 0.01, 2, value -> rebellionChance = value)));
        rows.add(option("rebellionDuration", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            rebellionDuration, 5, 600, 1, 0, value -> rebellionDuration = (int) Math.round(value))));
        rows.add(option("rebellionRadius", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            rebellionRadius, 1, 32, 1, 0, value -> rebellionRadius = (int) Math.round(value))));
        rows.add(option("canDestroyDevices", (x, y, w, h) -> toggle(x, y, w, h,
            canDestroyDevices, value -> canDestroyDevices = value)));
        rows.add(option("destroyCooldown", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            destroyCooldown, 1, 60, 1, 0, value -> destroyCooldown = (int) Math.round(value))));
        rows.add(option("destroyIntensity", (x, y, w, h) -> new ValueSlider(x, y, w, h,
            destroyIntensity, 0.0, 1.0, 0.01, 2, value -> destroyIntensity = value)));
    }

    // ===== 布局 =====

    @Override
    protected void init() {
        contentTop = 32;
        contentBottom = height - 40;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        visibleRows = Math.max(1, (contentBottom - contentTop) / ROW_H);
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        scrollRow = Mth.clamp(scrollRow, 0, maxScroll);

        int widgetX = width - MARGIN - WIDGET_W;
        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scrollRow + i;
            if (rowIndex >= rows.size()) break;
            Row row = rows.get(rowIndex);
            if (row.factory() == null) continue;
            addRenderableWidget(row.factory().create(widgetX, contentTop + i * ROW_H + 1, WIDGET_W, 20));
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
            .bounds(width / 2 - 100, height - 27, 200, 20).build());
    }

    // ===== 渲染 =====

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        int widgetX = width - MARGIN - WIDGET_W;
        for (int i = 0; i < visibleRows; i++) {
            int rowIndex = scrollRow + i;
            if (rowIndex >= rows.size()) break;
            Row row = rows.get(rowIndex);
            int rowY = contentTop + i * ROW_H;

            if (row.factory() == null) {
                guiGraphics.drawString(font, row.label(), MARGIN, rowY + 6, HEADER_COLOR, false);
                continue;
            }

            guiGraphics.drawString(font, row.label(), MARGIN, rowY + 6, LABEL_COLOR, false);
            if (mouseY >= rowY && mouseY < rowY + ROW_H && mouseX < widgetX) {
                guiGraphics.renderTooltip(font, font.split(row.tooltip(), 200), mouseX, mouseY);
            }
        }

        if (rows.size() > visibleRows) {
            Component hint = Component.translatable("createlaborrush.config.scroll",
                scrollRow + 1, Math.min(rows.size(), scrollRow + visibleRows), rows.size());
            guiGraphics.drawCenteredString(font, hint, width / 2, contentBottom + 2, 0x909090);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        if (maxScroll <= 0) return false;

        int next = Mth.clamp(scrollRow - (int) Math.signum(delta), 0, maxScroll);
        if (next == scrollRow) return false;
        scrollRow = next;
        rebuild();
        return true;
    }

    @Override
    public void onClose() {
        // ESC 关闭：放弃本次修改
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void saveAndClose() {
        ConfigSyncClient.applyFromScreen(new Config.Values(
            destroyChance, destroyRatioMin, destroyRatioMax,
            rebellionEnabled, rebellionTriggerTime, rebellionChance,
            rebellionDuration, rebellionRadius, canDestroyDevices,
            destroyCooldown, destroyIntensity
        ));
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private AbstractWidget toggle(int x, int y, int width, int height, boolean current, Consumer<Boolean> setter) {
        final boolean[] state = {current};
        return Button.builder(onOff(current), b -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            b.setMessage(onOff(state[0]));
        }).bounds(x, y, width, height).build();
    }

    // ===== 内部类型 =====

    private record Row(Component label, Component tooltip, WidgetFactory factory) {
        static Row header(String section) {
            return new Row(Component.translatable("createlaborrush.config.section." + section), null, null);
        }
    }

    @FunctionalInterface
    private interface WidgetFactory {
        AbstractWidget create(int x, int y, int width, int height);
    }

    /** 数值配置用的滑条：0~1 归一化存值，按 step 取整后回调 */
    private class ValueSlider extends AbstractSliderButton {

        private final double min;
        private final double max;
        private final double step;
        private final int decimals;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int width, int height, double value,
                    double min, double max, double step, int decimals, DoubleConsumer onChange) {
            super(x, y, width, height, Component.empty(), Mth.clamp((value - min) / (max - min), 0.0, 1.0));
            this.min = min;
            this.max = max;
            this.step = step;
            this.decimals = decimals;
            this.onChange = onChange;
            updateMessage();
        }

        private double currentValue() {
            double raw = min + (max - min) * this.value;
            if (step > 0) raw = Math.round(raw / step) * step;
            return Mth.clamp(raw, min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.format(Locale.ROOT, "%." + decimals + "f", currentValue())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(currentValue());
        }
    }
}