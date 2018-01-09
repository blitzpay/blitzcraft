/*
 * Copyright 2018 blitzpay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blitzpay.blitzcraft;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.*;
import org.bukkit.plugin.java.*;

/**
 *
 * @author spearson
 */
public class Main extends JavaPlugin implements Listener,Runnable{
    
    private static final short INVOICE_MAP = 0;
     
    public static Connection con; 
    
    private Context ctx;
    private Lightning ln;
    
    @Override
    public void onEnable() {
        
       this.ctx = new Context(getLogger());
       this.ln = ctx.getLightning();
        
       MapView map = Bukkit.getMap(INVOICE_MAP);
       for(MapRenderer mr : map.getRenderers())
       {
           map.removeRenderer(mr);
       }
       map.addRenderer(new InvoiceRenderer(ctx));
       
       
       
       getServer().getPluginManager().registerEvents(this, this);
       
       getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 5*20l, 5*20l);
       
       /*
        try
        {

            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:blitzcraft.db");
            Statement stmnt = con.createStatement();
            stmnt.execute("CREATE TABLE IF NOT EXISTS PLOTOWNERSHIP (location TEXT,owner TEXT);");
        }
        catch(Exception e)
        {
             getLogger().log(Level.SEVERE,"ERROR" , e);       
        }
       */
    }
   
    @Override
    public void run()
    {
        PlayerState.updatePlayerStates();
    }
    
    @Override
    public void onDisable() {
       
    }
    
    @EventHandler
    public void InventoryEvent(InventoryClickEvent event) {
      
        HumanEntity player = event.getWhoClicked();
        
        
        InventoryAction act = event.getAction();
              
        if( act == InventoryAction.DROP_ALL_CURSOR ||
            act == InventoryAction.DROP_ALL_SLOT ||
            act == InventoryAction.DROP_ONE_CURSOR ||
            act == InventoryAction.DROP_ONE_SLOT
        )
        {
            ItemStack drop = event.getCursor();
            
            if( drop.getType() == Material.MAP && drop.getDurability() == INVOICE_MAP )
            {
                event.setCancelled(true);            
            }
        } 
        else if( act == InventoryAction.PLACE_ALL ||
                act == InventoryAction.PLACE_ONE ||
                act == InventoryAction.PLACE_SOME )
        {
            Inventory clickedInventory = event.getClickedInventory();
            if(clickedInventory.getType() != InventoryType.PLAYER)
            {
                ItemStack drop = event.getCursor();          
                if( drop.getType() == Material.MAP && drop.getDurability() == INVOICE_MAP )
                {
                    event.setCancelled(true);            
                }
            }
        }
    }  
    
    @EventHandler
    public void PlayerDropItemEvent(PlayerDropItemEvent event) {
        ItemStack drop = event.getItemDrop().getItemStack();

        if( drop.getType() == Material.MAP && drop.getDurability() == INVOICE_MAP )
        {
            event.setCancelled(true);            
        }
    }
    
    public void updatePlayerState(Player player)
    {
    }
      
    @EventHandler
    public void PlayerMoveEvent(PlayerMoveEvent event) {
        
        PlayerState playerState = PlayerState.getPlayerState(event.getPlayer(),ctx);
        
        if( !playerState.isPaid()  )
        {
            boolean foundInvoice = false;
            
            for(ItemStack item : event.getPlayer().getInventory().getContents())
            { 
                if( item != null && item.getType() == Material.MAP && item.getDurability() == INVOICE_MAP )
                {
                    foundInvoice=true;
                    break;
                }
            }
            
            if(!foundInvoice)
            {
                //getLogger().info("Allocated new Invoice Map");
                Inventory i = event.getPlayer().getInventory();                
                i.addItem(new ItemStack(Material.MAP, 1, INVOICE_MAP));
            }
            
            
            Location from = event.getFrom();
            Location to = event.getTo();
            if(
                    from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ()
            )
            {
                event.setCancelled(true);            
            }
            
            PaymentRequest pr = playerState.getCurrentPayment();
            if(pr != null && !playerState.payReqAsMsg)
            {  
                String message = "Send Payment";
                String url = "lightning:" + pr.Request;
                   
                String cmd = "tellraw "+event.getPlayer().getName()+" {\"text\":\"Click to Send Payment\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"http://:"+pr.Request+"\"}}";
                
                Bukkit.getServer().dispatchCommand(
                    Bukkit.getConsoleSender(),
                    cmd);
                
                playerState.payReqAsMsg = true;
            }
        }
        
        updatePlayerState(event.getPlayer());
            
    }
    
    @EventHandler
    public void PlayerInteractEvent(PlayerInteractEvent event) {
    }
    
    @EventHandler
    public void PlayerLoginEvent(PlayerLoginEvent event) {
        
        PlayerState playerState = PlayerState.getPlayerState(event.getPlayer(),ctx);
        try
        {
            playerState.clearError();
           
            PaymentRequest payReq = ln.createPaymentRequest("BlitzCraft Login(" + event.getPlayer().getName() + ")" , 100 );
            
            playerState.sendLoginPayment(payReq);
            
            updatePlayerState(event.getPlayer());
        }
        catch(Exception e)
        {
            playerState.setError();
            getLogger().log(Level.SEVERE,"ERROR" , e);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender,
            Command command,
            String label,
            String[] args) {
        if (command.getName().equalsIgnoreCase("mycommand")) {
            sender.sendMessage("You ran /mycommand!");
            return true;
        }
        return false;
    }
    
}
