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
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author spearson
 */
public class Lightning {
    
    private final Context ctx;
    
    public Lightning(Context ctx)
    {
        this.ctx=ctx;
    }
    
    private static final boolean ENABLED = true;
    
    private static BufferedImage getRequestQR(String paymentRequest) throws Exception
    {
        com.google.zxing.Writer writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(paymentRequest, com.google.zxing.BarcodeFormat.QR_CODE, 57, 57);

        //generate an image from the byte matrix
        int width = matrix.getWidth(); 
        int height = matrix.getHeight(); 

        //create buffered image to draw to
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) { 
         for (int x = 0; x < width; x++) {        
          image.setRGB(x, y, (matrix.get(x, y) ? 0 : 0xFFFFFF ));
         }
        }

        return image;
    }
    
    //        params.add(10000000);
    //params.add("BlitzCraft login (" + player.getName() + ")");
    
    
    private static String readString(InputStream inputStream) throws Exception
    {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, "UTF-8");
        for (; ; ) {
            int rsz = in.read(buffer, 0, buffer.length);
            if (rsz < 0)
                break;
            out.append(buffer, 0, rsz);
        }
        return out.toString();
    }
   
    
    public PaymentRequest createPaymentRequest(String description,int amount) throws Exception
    {
        if(!ENABLED)
        {
            PaymentRequest payReq = new PaymentRequest();
            payReq.Request = "BOB";
            payReq.Hash    = "BOB";
            payReq.RequestQR = getRequestQR(payReq.Request);
            return payReq;
        }
               
        try
        {
            String stdin = "EMPTY";
            String stderr = "EMPTY";            
            
            Process lncliProcess = Runtime.getRuntime().exec(new String[]{
                "lncli",
                "--no-macaroons",
                "addinvoice",
                "--value",
                Integer.toString(amount),
                "--memo",
                description
            });

            stdin = readString(lncliProcess.getInputStream());
            stderr = readString(lncliProcess.getErrorStream());   

            int exitCode = lncliProcess.waitFor();
            if(exitCode == 0)
            {
                JSONParser p =new JSONParser();
                JSONObject res = (JSONObject)p.parse(stdin);  

                PaymentRequest payReq = new PaymentRequest();
                payReq.Request = (String)res.get("pay_req");
                payReq.Hash    = (String)res.get("r_hash");
                payReq.RequestQR = getRequestQR(payReq.Request); 
                
                ctx.getLogger().info("CreatePayment " + description + " " + amount + " invoice " + payReq.Request + " Hash" + payReq.Hash);

                return payReq;
            }
            else
            {
                ctx.getLogger().severe("AddInvoice Error stdin>" + stdin);
                ctx.getLogger().severe("AddInvoice Error stderr>" + stderr);
                throw new Exception("AddInvoice failed with exits code" + exitCode);
            }
        }
        catch(Exception e)
        {
            ctx.getLogger().log(Level.SEVERE,"Add Invoice Failed",e);
            throw e;
        }
    }
    
    public boolean isPaid(PaymentRequest payReq)
    {
        if(!ENABLED)
        {
            return true;
        }
        else if(payReq == null)
        {
            return true;
        }
        else
        {
            String stdin = "EMPTY";
            String stderr = "EMPTY";
            try
            {

                
                Process lncliProcess = Runtime.getRuntime().exec(new String[]{
                    "lncli",
                    "--no-macaroons",
                    "lookupinvoice",
                    payReq.Hash
                });
                
                stdin = readString(lncliProcess.getInputStream());
                stderr = readString(lncliProcess.getErrorStream()); 
                
                int exitCode = lncliProcess.waitFor();
                if(exitCode == 0)
                {                
                    JSONParser p =new JSONParser();
                    JSONObject res = (JSONObject)p.parse(stdin);  
                
                    boolean settled = (boolean)res.get("settled");
                    
                    if(settled)
                    {
                        ctx.getLogger().info("IsPaid " + payReq.Hash + " = " + settled);
                    }
                    
                    return settled;
                }
                else
                {
                    throw new Exception("IsPaid failed with exit code" + exitCode);                    
                }
            }
            catch(Exception e)
            {
                ctx.getLogger().severe("IsPaid Error stdin>" + stdin);
                ctx.getLogger().severe("IsPaid Error stderr>" + stderr);                
                ctx.getLogger().log(Level.SEVERE,"IsPaid " + payReq.Hash + " Failed",e);
                return false;
            }

            
        }
    }
    
}
