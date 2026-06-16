package net.iann.vanillareenchanted.screen;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/research_table.png"
            );

    public ResearchTableScreen(ResearchTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = 278;  // change to your PNG width
        this.imageHeight = 278; // change to your PNG height

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
                TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                this.imageWidth,
                this.imageHeight,
                this.imageWidth,
                this.imageHeight
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderEnchantmentList(guiGraphics);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Hide default title and inventory labels
    }
    private void renderEnchantmentList(GuiGraphics guiGraphics) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();

        int startX = this.leftPos + 34;
        int startY = this.topPos + 63;

        int lineHeight = 10;
        int maxVisible = 9;

        for (int i = 0; i < enchantments.size() && i < maxVisible; i++) {
            Holder<Enchantment> enchantmentHolder = enchantments.get(i);

            String name = enchantmentHolder.unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse("unknown");

            Component text = Component.literal(formatEnchantName(name))
                    .withStyle(ChatFormatting.DARK_GRAY);

            guiGraphics.drawString(
                    this.font,
                    text,
                    startX,
                    startY + i * lineHeight,
                    0x3F2A14,
                    false
            );
        }
    }
    private String formatEnchantName(String path) {
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }

        return result.toString().trim();
    }
}