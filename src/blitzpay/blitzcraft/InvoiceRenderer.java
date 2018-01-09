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

import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author spearson
 */
public class InvoiceRenderer extends MapRenderer {

    private final Context ctx;
    
    private static BufferedImage PAYMENT_RECEIVED;
    private static BufferedImage SEND_PAYMENT;            
    private static BufferedImage ERROR;
    private static BufferedImage IDLE;     
    
    public InvoiceRenderer(Context ctx) {
        super(true);
        this.ctx = ctx;
    }
    
    @Override
    public void render(MapView mv, MapCanvas mc, Player player) {      
       
        PlayerState playerState = PlayerState.getPlayerState(player,ctx);
        try
        {
            if( PAYMENT_RECEIVED == null)
            {
                URL url = getClass().getResource("/paymentreceived.png");
                PAYMENT_RECEIVED = ImageIO.read(url);
            }
            
            if( SEND_PAYMENT == null)
            {
                URL url = getClass().getResource("/sendpayment.png");
                SEND_PAYMENT = ImageIO.read(url);
            }
            
            if(ERROR == null)
            {
                URL url = getClass().getResource("/error.png");
                ERROR = ImageIO.read(url);
            }
            
            if(IDLE == null)
            {
                URL url = getClass().getResource("/paymentreceived.png");
                IDLE = ImageIO.read(url);
            }
            
            if(!playerState.mapUpToDate)
            {
                if(playerState.isError())
                {
                    ctx.getLogger().info("Updating Map to Player State Error");
                    mc.drawImage(0, 0, ERROR);                    
                }
                if(playerState.isDisplayPaymentRecieved() )
                {
                    mc.drawImage(0, 0, PAYMENT_RECEIVED);
                }
                else if (playerState.getCurrentPayment() != null)
                {
                    mc.drawImage(0, 0, SEND_PAYMENT); 
                    
                    BufferedImage invoiceQr = playerState.getCurrentPayment().RequestQR;
                    
                    mc.drawImage(35, 64, invoiceQr);  
                    
                }
                else
                {
                    mc.drawImage(0, 0, IDLE);
                }
                
                playerState.mapUpToDate = true;
            }
        }
        catch(Exception e)
        {
            ctx.getLogger().log(Level.SEVERE,"error during invoice map render",e);
            playerState.setError();
            mc.drawImage(0, 0, ERROR);          
        }
    }
    
}
