package net.iann.vanillareenchanted.screen;

import net.iann.vanillareenchanted.VanillaReenchanted;
import net.iann.vanillareenchanted.menu.ResearchTableMenu;
import net.iann.vanillareenchanted.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {

    // -------------------------------------------------
    // GUI SIZE
    // Must match the size of research_table.png.
    // -------------------------------------------------

    private static final int GUI_WIDTH = 278;
    private static final int GUI_HEIGHT = 278;

    // -------------------------------------------------
    // MAIN BACKGROUND TEXTURE
    // -------------------------------------------------

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/research_table.png"
            );

    // -------------------------------------------------
    // ENCHANTMENT LIST SETTINGS
    // -------------------------------------------------

    private static final int ENCHANTMENTS_PER_PAGE = 6;

    // Position of the first enchantment row inside the GUI texture.
    private static final int ENCHANT_ROW_X = 33;
    private static final int ENCHANT_ROW_Y = 63;

    // Size of one row texture.
    private static final int ENCHANT_ROW_WIDTH = 97;
    private static final int ENCHANT_ROW_HEIGHT = 13;
    private static final int ENCHANT_ROW_SPACING = 15;


    // Text offsets inside one enchantment row.
    private static final int ENCHANT_LEVEL_TEXT_X = 4;
    private static final int ENCHANT_NAME_TEXT_X = 15;
    private static final int ENCHANT_TEXT_Y = 3;
    private static final int ENCHANT_NAME_RIGHT_PADDING = 2;

    private static final int ENCHANT_NAME_MAX_WIDTH =
            ENCHANT_ROW_WIDTH
                    - ENCHANT_NAME_TEXT_X
                    - ENCHANT_NAME_RIGHT_PADDING;

    private static final int TEXT_COLOR = 0xFFC79D7D;

    private static final ResourceLocation ENCHANT_ROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/enchantment_list_text_box.png"
            );

    private static final ResourceLocation ENCHANT_ROW_HOVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/enchantment_list_text_box_outline.png"
            );

    // -------------------------------------------------
    // SELECTED ENCHANTMENT ARROW
    // -------------------------------------------------

    private static final int SELECT_ARROW_WIDTH = 4;
    private static final int SELECT_ARROW_HEIGHT = 8;

    // Offset from the enchantment row.
    private static final int SELECT_ARROW_X_OFFSET = -5;
    private static final int SELECT_ARROW_Y_OFFSET = 3;

    private static final ResourceLocation SELECT_ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/select_arrow.png"
            );

    // -------------------------------------------------
    // PAGE ARROWS
    // -------------------------------------------------

    private static final int PAGE_PREVIOUS_X = 55;
    private static final int PAGE_NEXT_X = PAGE_PREVIOUS_X + 35;
    private static final int PAGE_Y = 156;

    private static final int PAGE_ARROW_SIZE = 11;

    // Offset from the previous arrow to the page number.
    private static final int PAGE_TEXT_X_OFFSET = 14;
    private static final int PAGE_TEXT_Y_OFFSET = 2;

    private static final ResourceLocation ACTIVE_LEFT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/active_left_arrow.png"
            );

    private static final ResourceLocation ACTIVE_RIGHT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/active_right_arrow.png"
            );

    private static final ResourceLocation INACTIVE_LEFT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/inactive_left_arrow.png"
            );

    private static final ResourceLocation INACTIVE_RIGHT_ARROW =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/inactive_right_arrow.png"
            );

    private static final ResourceLocation PAGE_ARROW_HOVER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    VanillaReenchanted.MODID,
                    "textures/gui/buttons/arrow_outline.png"
            );

    // -------------------------------------------------
    // SCREEN STATE
    // These values change while the player uses the menu.
    // -------------------------------------------------

    private int enchantmentPage = 0;

    // Null means the player has not selected an enchantment yet.
    private ResourceLocation selectedEnchantmentId = null;

    public ResearchTableScreen(
            ResearchTableMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    // Draws the large book/inventory background image.
    @Override
    protected void renderBg(
            GuiGraphics guiGraphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        guiGraphics.blit(
                BACKGROUND_TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                GUI_WIDTH,
                GUI_HEIGHT,
                GUI_WIDTH,
                GUI_HEIGHT
        );
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Draw slots, items, and the base menu.
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw our custom enchantment rows and page buttons.
        renderEnchantmentList(guiGraphics, mouseX, mouseY);

        // Draw normal Minecraft item tooltips.
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // We do not have space for vanilla's default "Research Table" and "Inventory" labels.
    @Override
    protected void renderLabels(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
    }

    // -------------------------------------------------
    // ENCHANTMENT LIST RENDERING
    // -------------------------------------------------

    private void renderEnchantmentList(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();

        int totalPages = getTotalPages(enchantments.size());
        clampCurrentPage(totalPages);

        int firstIndex = this.enchantmentPage * ENCHANTMENTS_PER_PAGE;
        int lastIndex = Math.min(firstIndex + ENCHANTMENTS_PER_PAGE, enchantments.size());

        int rowX = this.leftPos + ENCHANT_ROW_X;

        for (int enchantmentIndex = firstIndex; enchantmentIndex < lastIndex; enchantmentIndex++) {
            Holder<Enchantment> enchantmentHolder = enchantments.get(enchantmentIndex);

            ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

            if (enchantmentId == null) {
                continue;
            }

            int rowIndexOnPage = enchantmentIndex - firstIndex;
            int rowY = this.topPos
                    + ENCHANT_ROW_Y
                    + rowIndexOnPage * ENCHANT_ROW_SPACING;

            boolean isHovered = isMouseOver(
                    mouseX,
                    mouseY,
                    rowX,
                    rowY,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT
            );

            boolean isSelected = enchantmentId.equals(this.selectedEnchantmentId);

            renderEnchantmentRowBackground(
                    guiGraphics,
                    rowX,
                    rowY,
                    isHovered,
                    isSelected
            );

            int displayedLevel = getDisplayedEnchantmentLevel(enchantmentHolder);

            renderEnchantmentRowText(
                    guiGraphics,
                    rowX,
                    rowY,
                    enchantmentHolder,
                    displayedLevel
            );
        }

        renderPageControls(guiGraphics, totalPages, mouseX, mouseY);
    }

    private void renderEnchantmentRowBackground(
            GuiGraphics guiGraphics,
            int rowX,
            int rowY,
            boolean isHovered,
            boolean isSelected
    ) {
        // Normal paper row.
        guiGraphics.blit(
                ENCHANT_ROW_TEXTURE,
                rowX,
                rowY,
                0,
                0,
                ENCHANT_ROW_WIDTH,
                ENCHANT_ROW_HEIGHT,
                ENCHANT_ROW_WIDTH,
                ENCHANT_ROW_HEIGHT
        );

        // Hover border appears over the normal row.
        if (isHovered) {
            guiGraphics.blit(
                    ENCHANT_ROW_HOVER_TEXTURE,
                    rowX,
                    rowY,
                    0,
                    0,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT
            );
        }

        // Selection arrow appears just left of the selected row.
        if (isSelected) {
            guiGraphics.blit(
                    SELECT_ARROW_TEXTURE,
                    rowX + SELECT_ARROW_X_OFFSET,
                    rowY + SELECT_ARROW_Y_OFFSET,
                    0,
                    0,
                    SELECT_ARROW_WIDTH,
                    SELECT_ARROW_HEIGHT,
                    SELECT_ARROW_WIDTH,
                    SELECT_ARROW_HEIGHT
            );
        }
    }

    private void renderEnchantmentRowText(
            GuiGraphics guiGraphics,
            int rowX,
            int rowY,
            Holder<Enchantment> enchantmentHolder,
            int level
    ) {
        Component levelText = Component.literal(String.valueOf(level));

        Component fullEnchantmentName = Component.literal(
                enchantmentHolder.value().description().getString()
        );

        Component enchantmentName = shortenTextToFit(
                fullEnchantmentName,
                ENCHANT_NAME_MAX_WIDTH
        );

        guiGraphics.drawString(
                this.font,
                levelText,
                rowX + ENCHANT_LEVEL_TEXT_X,
                rowY + ENCHANT_TEXT_Y,
                TEXT_COLOR,
                false
        );

        guiGraphics.drawString(
                this.font,
                enchantmentName,
                rowX + ENCHANT_NAME_TEXT_X,
                rowY + ENCHANT_TEXT_Y,
                TEXT_COLOR,
                false
        );
    }

    // When no item is inserted: show player research level.
    // When an item is inserted: show the enchantment level on that item.
    private int getDisplayedEnchantmentLevel(
            Holder<Enchantment> enchantmentHolder
    ) {
        ItemStack researchItem = this.menu.getResearchItem();

        if (!researchItem.isEmpty()) {
            return researchItem.getEnchantmentLevel(enchantmentHolder);
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return 0;
        }

        ResourceLocation enchantmentId = getEnchantmentId(enchantmentHolder);

        if (enchantmentId == null) {
            return 0;
        }

        return minecraft.player
                .getData(ModAttachments.PLAYER_KNOWLEDGE)
                .getLevel(enchantmentId);
    }

    // -------------------------------------------------
    // PAGE BUTTONS
    // -------------------------------------------------

    private void renderPageControls(
            GuiGraphics guiGraphics,
            int totalPages,
            int mouseX,
            int mouseY
    ) {
        if (totalPages <= 1) {
            return;
        }

        int previousArrowX = this.leftPos + PAGE_PREVIOUS_X;
        int nextArrowX = this.leftPos + PAGE_NEXT_X;
        int arrowY = this.topPos + PAGE_Y;

        boolean canGoPrevious = this.enchantmentPage > 0;
        boolean canGoNext = this.enchantmentPage < totalPages - 1;

        boolean previousHovered = isMouseOver(
                mouseX,
                mouseY,
                previousArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        );

        boolean nextHovered = isMouseOver(
                mouseX,
                mouseY,
                nextArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        );

        renderPageArrow(
                guiGraphics,
                previousArrowX,
                arrowY,
                canGoPrevious ? ACTIVE_LEFT_ARROW : INACTIVE_LEFT_ARROW,
                canGoPrevious && previousHovered
        );

        renderPageArrow(
                guiGraphics,
                nextArrowX,
                arrowY,
                canGoNext ? ACTIVE_RIGHT_ARROW : INACTIVE_RIGHT_ARROW,
                canGoNext && nextHovered
        );

        Component pageText = Component.literal(
                (this.enchantmentPage + 1) + "/" + totalPages
        );

        guiGraphics.drawString(
                this.font,
                pageText,
                previousArrowX + PAGE_TEXT_X_OFFSET,
                arrowY + PAGE_TEXT_Y_OFFSET,
                TEXT_COLOR,
                false
        );
    }

    private void renderPageArrow(
            GuiGraphics guiGraphics,
            int x,
            int y,
            ResourceLocation arrowTexture,
            boolean showHoverOutline
    ) {
        guiGraphics.blit(
                arrowTexture,
                x,
                y,
                0,
                0,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        );

        if (showHoverOutline) {
            guiGraphics.blit(
                    PAGE_ARROW_HOVER_TEXTURE,
                    x,
                    y,
                    0,
                    0,
                    PAGE_ARROW_SIZE,
                    PAGE_ARROW_SIZE,
                    PAGE_ARROW_SIZE,
                    PAGE_ARROW_SIZE
            );
        }
    }

    // -------------------------------------------------
    // CLICK HANDLING
    // -------------------------------------------------

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        // Only left mouse clicks interact with our UI controls.
        if (button == 0) {
            if (handlePageButtonClick(mouseX, mouseY)) {
                return true;
            }

            if (handleEnchantmentRowClick(mouseX, mouseY)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePageButtonClick(
            double mouseX,
            double mouseY
    ) {
        int totalPages = getTotalPages(
                this.menu.getVisibleEnchantments().size()
        );

        if (totalPages <= 1) {
            return false;
        }

        int previousArrowX = this.leftPos + PAGE_PREVIOUS_X;
        int nextArrowX = this.leftPos + PAGE_NEXT_X;
        int arrowY = this.topPos + PAGE_Y;

        if (isMouseOver(
                mouseX,
                mouseY,
                previousArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        )) {
            if (this.enchantmentPage > 0) {
                this.enchantmentPage--;
                playButtonClickSound();
            }

            return true;
        }

        if (isMouseOver(
                mouseX,
                mouseY,
                nextArrowX,
                arrowY,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE
        )) {
            if (this.enchantmentPage < totalPages - 1) {
                this.enchantmentPage++;
                playButtonClickSound();
            }

            return true;
        }

        return false;
    }

    private boolean handleEnchantmentRowClick(
            double mouseX,
            double mouseY
    ) {
        List<Holder<Enchantment>> enchantments = this.menu.getVisibleEnchantments();

        int firstIndex = this.enchantmentPage * ENCHANTMENTS_PER_PAGE;
        int lastIndex = Math.min(firstIndex + ENCHANTMENTS_PER_PAGE, enchantments.size());

        int rowX = this.leftPos + ENCHANT_ROW_X;

        for (int enchantmentIndex = firstIndex; enchantmentIndex < lastIndex; enchantmentIndex++) {
            int rowIndexOnPage = enchantmentIndex - firstIndex;

            int rowY = this.topPos
                    + ENCHANT_ROW_Y
                    + rowIndexOnPage * ENCHANT_ROW_SPACING;

            if (!isMouseOver(
                    mouseX,
                    mouseY,
                    rowX,
                    rowY,
                    ENCHANT_ROW_WIDTH,
                    ENCHANT_ROW_HEIGHT
            )) {
                continue;
            }

            ResourceLocation enchantmentId = getEnchantmentId(
                    enchantments.get(enchantmentIndex)
            );

            if (enchantmentId != null) {
                this.selectedEnchantmentId = enchantmentId;
                playButtonClickSound();
            }

            return true;
        }

        return false;
    }

    // -------------------------------------------------
    // SMALL UTILITY METHODS
    // -------------------------------------------------

    private void clampCurrentPage(int totalPages) {
        if (this.enchantmentPage >= totalPages) {
            this.enchantmentPage = Math.max(0, totalPages - 1);
        }
    }

    private int getTotalPages(int enchantmentCount) {
        if (enchantmentCount <= 0) {
            return 1;
        }

        return (int) Math.ceil(
                enchantmentCount / (double) ENCHANTMENTS_PER_PAGE
        );
    }

    private ResourceLocation getEnchantmentId(
            Holder<Enchantment> enchantmentHolder
    ) {
        return enchantmentHolder.unwrapKey()
                .map(key -> key.location())
                .orElse(null);
    }

    private String formatEnchantName(String enchantmentPath) {
        String[] words = enchantmentPath.split("_");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            formattedName.append(
                    Character.toUpperCase(word.charAt(0))
            );

            formattedName.append(word.substring(1));
            formattedName.append(" ");
        }

        return formattedName.toString().trim();
    }

    private void playButtonClickSound() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            minecraft.player.playSound(
                    SoundEvents.UI_BUTTON_CLICK.value(),
                    1.0F,
                    1.0F
            );
        }
    }

    private boolean isMouseOver(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }
    private Component shortenTextToFit(Component text, int maxWidth) {
        String fullText = text.getString();

        // It already fits, so keep it unchanged.
        if (this.font.width(fullText) <= maxWidth) {
            return Component.literal(fullText);
        }

        String dots = ".";
        int availableWidth = maxWidth - this.font.width(dots);

        String shortenedText = fullText;

        while (!shortenedText.isEmpty()
                && this.font.width(shortenedText) > availableWidth) {

            shortenedText = shortenedText.substring(
                    0,
                    shortenedText.length() - 1
            );
        }

        return Component.literal(shortenedText + dots);
    }
}