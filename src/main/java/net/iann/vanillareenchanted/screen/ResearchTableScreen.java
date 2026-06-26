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
    private static final int ENCHANTMENTS_PER_PAGE = 9;

    private static final int ENCHANT_LIST_X = 34;
    private static final int ENCHANT_LIST_Y = 63;
    private static final int ENCHANT_LINE_HEIGHT = 10;

    private static final int PREV_PAGE_X = 105;
    private static final int NEXT_PAGE_X = 145;
    private static final int PAGE_BUTTON_Y = 152;

    private int enchantmentPage = 0;

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

        int totalPages = getTotalPages(enchantments.size());

        if (this.enchantmentPage >= totalPages) {
            this.enchantmentPage = Math.max(0, totalPages - 1);
        }

        int startIndex = this.enchantmentPage * ENCHANTMENTS_PER_PAGE;
        int endIndex = Math.min(startIndex + ENCHANTMENTS_PER_PAGE, enchantments.size());

        int startX = this.leftPos + ENCHANT_LIST_X;
        int startY = this.topPos + ENCHANT_LIST_Y;

        for (int i = startIndex; i < endIndex; i++) {
            Holder<Enchantment> enchantmentHolder = enchantments.get(i);

            String name = enchantmentHolder.unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse("unknown");

            Component text = Component.literal(formatEnchantName(name))
                    .withStyle(ChatFormatting.DARK_GRAY);

            int lineIndex = i - startIndex;

            guiGraphics.drawString(
                    this.font,
                    text,
                    startX,
                    startY + lineIndex * ENCHANT_LINE_HEIGHT,
                    0x3F2A14,
                    false
            );
        }

        renderPageControls(guiGraphics, totalPages);
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
    private void renderPageControls(GuiGraphics guiGraphics, int totalPages) {
        if (totalPages <= 1) {
            return;
        }

        int y = this.topPos + PAGE_BUTTON_Y;

        Component prev = Component.literal("<");
        Component next = Component.literal(">");
        Component pageText = Component.literal((this.enchantmentPage + 1) + " / " + totalPages);

        guiGraphics.drawString(
                this.font,
                prev,
                this.leftPos + PREV_PAGE_X,
                y,
                0x3F2A14,
                false
        );

        guiGraphics.drawString(
                this.font,
                pageText,
                this.leftPos + 119,
                y,
                0x3F2A14,
                false
        );

        guiGraphics.drawString(
                this.font,
                next,
                this.leftPos + NEXT_PAGE_X,
                y,
                0x3F2A14,
                false
        );
    }
    private int getTotalPages(int enchantmentCount) {
        if (enchantmentCount == 0) {
            return 1;
        }

        return (int) Math.ceil(enchantmentCount / (double) ENCHANTMENTS_PER_PAGE);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handlePageButtonClick(mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    private boolean handlePageButtonClick(double mouseX, double mouseY) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();
        int totalPages = getTotalPages(enchantments.size());

        if (totalPages <= 1) {
            return false;
        }

        int prevX = this.leftPos + PREV_PAGE_X;
        int nextX = this.leftPos + NEXT_PAGE_X;
        int y = this.topPos + PAGE_BUTTON_Y;

        if (isMouseOver(mouseX, mouseY, prevX, y, 10, 10)) {
            if (this.enchantmentPage > 0) {
                this.enchantmentPage--;
            }
            return true;
        }

        if (isMouseOver(mouseX, mouseY, nextX, y, 10, 10)) {
            if (this.enchantmentPage < totalPages - 1) {
                this.enchantmentPage++;
            }
            return true;
        }

        return false;
    }
    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }
}