package net.samagames.hub.gui.achievements;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.samagames.api.SamaGamesAPI;
import net.samagames.api.achievements.Achievement;
import net.samagames.api.achievements.AchievementCategory;
import net.samagames.api.achievements.AchievementProgress;
import net.samagames.api.achievements.IncrementationAchievement;
import net.samagames.hub.Hub;
import net.samagames.hub.gui.AbstractGui;
import net.samagames.hub.gui.profile.GuiProfile;
import net.samagames.hub.utils.SimilarityUtils;
import net.samagames.tools.ItemUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiAchievements extends AbstractGui
{
    private static Table<Integer, Integer, List<Triple<Integer, Integer, Boolean>>> CACHE;

    private static final ItemStack LOCKED_HEAD = ItemUtils.getCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDhjNTNiY2U4YWU1OGRjNjkyNDkzNDgxOTA5YjcwZTExYWI3ZTk0MjJkOWQ4NzYzNTEyM2QwNzZjNzEzM2UifX19");
    private static final ItemStack UNLOCKED_BLUE_HEAD = ItemUtils.getCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI2ZTM0NjI4N2EyMWRiZmNhNWI1OGMxNDJkOGQ1NzEyYmRjODRmNWI3NWQ0MzE0ZWQyYTgzYjIyMmVmZmEifX19");
    private static final ItemStack UNLOCKED_GOLD_HEAD = ItemUtils.getCustomHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWM3ZjdiNzJmYzNlNzMzODI4ZmNjY2MwY2E4Mjc4YWNhMjYzM2FhMzNhMjMxYzkzYTY4MmQxNGFjNTRhYTBjNCJ9fX0=");
    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("EEEE d MMMM yyyy à HH:mm", Locale.FRENCH);

    private AchievementCategory category;
    private int page;

    public GuiAchievements(Hub hub, AchievementCategory category, int page)
    {
        super(hub);

        this.category = category;
        this.page = page;
    }

    @Override
    public void display(Player player)
    {
        this.inventory = this.hub.getServer().createInventory(null, 54, "Objectifs" + (this.category != null ? " (Page " + (this.page + 1) + ")" : ""));

        int[] baseSlots = {10, 11, 12, 13, 14, 15, 16};
        int lines = 0;
        int slot = 0;

        for (AchievementCategory category : SamaGamesAPI.get().getAchievementManager().getAchievementsCategories())
        {
            if (category.getParent() == this.category)
            {
                ItemStack itemStack = category.getIcon().clone();

                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName(ChatColor.GOLD + ChatColor.translateAlternateColorCodes('&', category.getDisplayName()));

                List<String> lore = new ArrayList<>();
                lore.add("");

                for (String line : category.getDescription())
                    lore.add(ChatColor.GRAY + line);

                itemMeta.setLore(lore);
                itemStack.setItemMeta(itemMeta);

                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                this.setSlotData(itemStack, (baseSlots[slot] + (lines * 9)), "category_" + category.getID());

                slot++;

                if (slot == 7)
                {
                    slot = 0;
                    lines++;
                }
            }
        }

        if (this.category != null && CACHE.containsRow(this.category.getID()))
        {
            for (Triple<Integer, Integer, Boolean> achievementPair : CACHE.get(this.category.getID(), this.page))
            {
                Achievement achievement = SamaGamesAPI.get().getAchievementManager().getAchievementByID(achievementPair.getMiddle());

                boolean unlocked = achievement.isUnlocked(player.getUniqueId());

                ItemStack itemStack = (unlocked ? (achievementPair.getRight() ? UNLOCKED_GOLD_HEAD : UNLOCKED_BLUE_HEAD) : LOCKED_HEAD).clone();

                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName((unlocked ? ChatColor.GREEN : ChatColor.RED) + ChatColor.translateAlternateColorCodes('&', achievement.getDisplayName()));

                List<String> lore = new ArrayList<>();
                lore.add("");

                for (String line : achievement.getDescription())
                    lore.add(ChatColor.GRAY + line);

                lore.add("");

                AchievementProgress progress = achievement.getProgress(player.getUniqueId());

                if (unlocked)
                {
                    Date unlockDate = new Date();
                    unlockDate.setTime(progress.getUnlockTime().getTime());

                    lore.add(ChatColor.DARK_GRAY + "Vous avez débloqué cet objectif");
                    lore.add(ChatColor.DARK_GRAY + "le : " + ChatColor.GRAY + WordUtils.capitalize(DATE_FORMATTER.format(unlockDate)) + ChatColor.DARK_GRAY + ".");
                }
                else if (!(achievement instanceof IncrementationAchievement))
                {
                    lore.add(ChatColor.DARK_GRAY + "Cet objectif n'est pas encore");
                    lore.add(ChatColor.DARK_GRAY + "débloqué.");
                }
                else
                {
                    int target = (progress == null ? ((IncrementationAchievement) achievement).getObjective() : (((IncrementationAchievement) achievement).getObjective() - progress.getProgress()));

                    lore.add(ChatColor.DARK_GRAY + "Vous devez effectuer cette action");
                    lore.add(ChatColor.DARK_GRAY + "encore " + ChatColor.GRAY + String.valueOf(target) + ChatColor.DARK_GRAY + " fois pour débloquer");
                    lore.add(ChatColor.DARK_GRAY + "cet objectif.");
                }

                itemMeta.setLore(lore);
                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                itemStack.setItemMeta(itemMeta);

                this.setSlotData(itemStack, achievementPair.getLeft(), "none");
            }
        }

        if (this.category != null && this.page > 0)
            this.setSlotData(ChatColor.YELLOW + "« Page " + (this.page), Material.PAPER, this.inventory.getSize() - 9, null, "page_back");

        if (this.category != null && CACHE.contains(this.category.getID(), this.page + 1))
            this.setSlotData(ChatColor.YELLOW + "Page " + (this.page + 2) + " »", Material.PAPER, this.inventory.getSize() - 1, null, "page_next");

        this.setSlotData(AbstractGui.getBackIcon(), 49, "back");

        player.openInventory(this.inventory);
    }

    @Override
    public void onClick(Player player, ItemStack stack, String action)
    {
        if (action.startsWith("category_"))
        {
            int id = Integer.parseInt(action.substring(9));
            this.hub.getGuiManager().openGui(player, new GuiAchievements(this.hub, SamaGamesAPI.get().getAchievementManager().getAchievementCategoryByID(id), 0));
        }
        else if (action.equals("page_back"))
        {
            this.hub.getGuiManager().openGui(player, new GuiAchievements(this.hub, this.category, this.page - 1));
        }
        else if (action.equals("page_next"))
        {
            this.hub.getGuiManager().openGui(player, new GuiAchievements(this.hub, this.category, this.page + 1));
        }
        else if (action.equals("back"))
        {
            this.hub.getGuiManager().openGui(player, this.category == null ? new GuiProfile(this.hub) : new GuiAchievements(this.hub, this.category.getParent(), 0));
        }
    }

    public static void createCache()
    {
        CACHE = HashBasedTable.create();
        int[] slots = { 36, 37, 38, 39, 40, 41, 42, 43, 44 };

        Map<Integer, List<Integer>> achievementParents = new HashMap<>();

        SamaGamesAPI.get().getAchievementManager().getAchievements().forEach(achievement ->
        {
            if (!achievementParents.containsKey(achievement.getParentCategoryID().getID()))
                achievementParents.put(achievement.getParentCategoryID().getID(), new ArrayList<>());

            achievementParents.get(achievement.getParentCategoryID().getID()).add(achievement.getID());
        });

        for (int categoryId : achievementParents.keySet())
        {
            System.out.println("New category (" + categoryId + ")");

            List<List<Integer>> families = new ArrayList<>();
            CopyOnWriteArrayList<Integer> remaining = new CopyOnWriteArrayList<>(achievementParents.get(categoryId));

            int page = 0;
            int slotIndex = 0;
            int slot = slots[slotIndex];

            for (int achievementId : remaining)
            {
                if (!remaining.contains(achievementId))
                    continue;

                System.out.println("> Listing in the first loop. Achievement " + achievementId);

                Achievement achievement = SamaGamesAPI.get().getAchievementManager().getAchievementByID(achievementId);
                String concatenated = Arrays.toString(achievement.getDescription());
                String cleared = concatenated.replaceAll("[^A-Za-z]+", "");

                ArrayList<Integer> family = new ArrayList<>();
                family.add(achievementId);

                for (int testAchievementId = achievementId - 2; testAchievementId < achievementId + 5; testAchievementId++)
                {
                    if (testAchievementId == achievementId)
                        continue;

                    System.out.println(">> Listing in the second loop. Achievement " + testAchievementId);

                    Achievement remainingAchievement = SamaGamesAPI.get().getAchievementManager().getAchievementByID(testAchievementId);

                    if (remainingAchievement instanceof IncrementationAchievement)
                    {
                        String remainingAchievementConcatenated = Arrays.toString(remainingAchievement.getDescription());
                        String remainingAchievementCleared = remainingAchievementConcatenated.replaceAll("[^A-Za-z]+", "");

                        if (SimilarityUtils.similarity(cleared, remainingAchievementCleared) > 0.8D)
                        {
                            System.out.println(">>> Description equals! Adding " + testAchievementId + " into the family.");

                            family.add(testAchievementId);
                            remaining.remove(new Integer(testAchievementId));
                        }
                    }
                }

                remaining.remove(new Integer(achievementId));
                families.add(family);
            }

            System.out.println("Sorting families...");

            Collections.sort(families, (o1, o2) ->
            {
                if ((((List) o1).size() > 1 && ((List) o2).size() > 1) || (((List) o1).size() == ((List) o2).size()))
                    return 0;
                else if (((List) o1).size() > ((List) o2).size())
                    return -1;
                else
                    return 1;
            });

            List<Pair<List<Integer>, Boolean>> columns = new ArrayList<>();
            List<Integer> independentActualColumn = new ArrayList<>();
            boolean wasBig = false;

            System.out.println("Creating columns...");

            for (List<Integer> family : families)
            {
                System.out.println("> New family: " + Arrays.toString(family.toArray()));

                if (family.size() > 1)
                {
                    System.out.println(">> This family is a big one");

                    Collections.sort(family, (o1, o2) ->
                    {
                        String o1Concatenated = Arrays.toString(SamaGamesAPI.get().getAchievementManager().getAchievementByID((Integer) o1).getDescription());
                        String o2Concatenated = Arrays.toString(SamaGamesAPI.get().getAchievementManager().getAchievementByID((Integer) o2).getDescription());

                        if (Integer.parseInt(o1Concatenated.replaceAll("[\\D]", "")) < Integer.parseInt(o2Concatenated.replaceAll("[\\D]", "")))
                            return -1;
                        else
                            return 1;
                    });

                    wasBig = true;
                    columns.add(Pair.of(family, true));
                }
                else
                {
                    if (wasBig)
                    {
                        System.out.println(">> Adding a white column to separate the big families from the independents.");

                        columns.add(Pair.of(null, null));
                        wasBig = false;
                    }

                    System.out.println(">> This family is an independent one");

                    independentActualColumn.add(family.get(0));

                    if (independentActualColumn.size() == 5)
                    {
                        System.out.println(">>> This independent family has reached 5 members, flushing...");

                        columns.add(Pair.of(independentActualColumn, false));
                        independentActualColumn = new ArrayList<>();
                    }
                }
            }

            if (!independentActualColumn.isEmpty())
                columns.add(Pair.of(independentActualColumn, false));

            System.out.println("Listing columns");

            for (Pair<List<Integer>, Boolean> columnPair : columns)
                System.out.print("> " + (columnPair.getLeft() != null ? Arrays.toString(columnPair.getLeft().toArray()) + (columnPair.getRight() ? " B" : "") : ""));

            for (Pair<List<Integer>, Boolean> columnPair : columns)
            {
                System.out.println("New column");

                if (columnPair.getLeft() == null && columnPair.getRight() == null)
                {
                    System.out.println("> This is a separation column");

                    if (slotIndex == 0 || slotIndex == slots.length - 1)
                        continue;
                }
                else
                {
                    for (int achievementId : columnPair.getLeft())
                    {
                        if (!CACHE.contains(categoryId, page))
                            CACHE.put(categoryId, page, new ArrayList<>());

                        CACHE.get(categoryId, page).add(Triple.of(slot, achievementId, columnPair.getRight()));

                        System.out.println("> Setted achievement " + achievementId + " at the slot " + slot + " on the page " + page);

                        slot -= 9;
                    }
                }

                slotIndex++;

                if (slotIndex == slots.length)
                {
                    slotIndex = 0;
                    page++;
                }

                slot = slots[slotIndex];
            }
        }
    }
}
