package net.samagames.hub.interactions;

import net.samagames.hub.Hub;
import net.samagames.hub.common.managers.AbstractManager;
import net.samagames.hub.interactions.bumper.BumperManager;
import net.samagames.hub.interactions.sonicsquid.SonicSquidManager;
import net.samagames.hub.interactions.yodels.YodelManager;
import org.bukkit.entity.Player;

import java.io.File;

public class InteractionManager extends AbstractManager
{
    private final YodelManager yodelManager;
    private final SonicSquidManager sonicSquidManager;
    private final BumperManager bumperManager;

    public InteractionManager(Hub hub)
    {
        super(hub);

        File interactionsDirectory = new File(hub.getDataFolder(), "interactions");

        if (!interactionsDirectory.exists())
            interactionsDirectory.mkdir();

        this.yodelManager = new YodelManager(hub);
        this.sonicSquidManager = new SonicSquidManager(hub);
        this.bumperManager = new BumperManager(hub);
    }

    @Override
    public void onDisable()
    {
        this.yodelManager.onDisable();
        this.sonicSquidManager.onDisable();
        this.bumperManager.onDisable();
    }

    @Override
    public void onLogin(Player player)
    {
        this.yodelManager.onLogin(player);
        this.sonicSquidManager.onLogin(player);
        this.bumperManager.onLogin(player);
    }

    @Override
    public void onLogout(Player player)
    {
        this.yodelManager.onLogout(player);
        this.sonicSquidManager.onLogout(player);
        this.bumperManager.onLogout(player);
    }

    public boolean isInteracting(Player player)
    {
        return this.yodelManager.hasPlayer(player) || this.sonicSquidManager.hasPlayer(player) || this.bumperManager.hasPlayer(player);
    }
}