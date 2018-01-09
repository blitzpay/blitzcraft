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

import java.util.logging.Logger;

/**
 *
 * @author spearson
 */
public class Context {
    
    private Logger logger;
    private Lightning ln;
    
    public Context(Logger logger)
    {
        this.logger=logger;
        this.ln = new Lightning(this);      
    }
      
    public Logger getLogger()
    {
        return logger;
    }
    
    public Lightning getLightning()
    {
        return ln;
    }
    
}
