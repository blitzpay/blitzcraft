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

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 *
 * @author spearson
 */
public class PlayerState {
     
    private static final HashMap<UUID,PlayerState> stateMap = new HashMap();
    
    private final Context ctx;
    
    private PlayerState(Context ctx)
    {
        this.ctx=ctx;
    }
    
    public static void updatePlayerStates()
    {
        for(PlayerState playerState : stateMap.values()){
            
            if(playerState.displayPayRecv)
            {
                if(playerState.isPaid() && (System.currentTimeMillis() - playerState.paidTime ) > 2000 )
                {
                    playerState.displayPayRecv = false;
                    playerState.mapUpToDate = false;
                }
            }
        }
    }
    
    public static PlayerState getPlayerState(Player player,Context ctx)
    { 
        PlayerState playerState = stateMap.get(player.getUniqueId());
        if(playerState == null)
        {
            playerState = new PlayerState(ctx);
            playerState.playerId = player.getUniqueId();
            stateMap.put(player.getUniqueId(), playerState);
        }
        
        playerState.isPaid();
        
        return playerState;        
    }
    
    public void sendLoginPayment(PaymentRequest req)
    {
        currentPayment = req;       
        paid = false;
        mapUpToDate = false;
        payReqAsMsg = false;
    }
    
    public boolean isDisplayPaymentRecieved()
    {
        return displayPayRecv;
    }
    
    public boolean isPaid()
    {
        if(!paid && currentPayment != null)
        {
            boolean reqPaid = ctx.getLightning().isPaid(currentPayment);
            if(reqPaid)
            {
                paid = true;
                paidTime = System.currentTimeMillis();
                displayPayRecv = true;
                mapUpToDate = false;
                currentPayment = null;
            } 
        }
        
        return paid;
    }
    
    public void setError()
    {
        error=true;
        mapUpToDate = false;
    }
    
    public void clearError()
    {
        error = false;
        mapUpToDate = false;
    }
    
    public boolean isError() {
        return error;
    }
    
    private UUID playerId;
    private PaymentRequest currentPayment;
    private boolean paid;
    private long paidTime;
    public boolean mapUpToDate;
    public boolean error;
    public boolean displayPayRecv;
    public boolean payReqAsMsg;
       
    public long getPaidTime() {
        return paidTime;
    }

    PaymentRequest getCurrentPayment() {
        return currentPayment;
    }
    
}
