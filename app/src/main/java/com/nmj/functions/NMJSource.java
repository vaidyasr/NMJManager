/*
 * Copyright (C) 2014 Michell Bak
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

package com.nmj.functions;

import android.content.Context;

public class NMJSource {

    private String IP_ADDRESS, PORT, DISPLAY_NAME;

    public NMJSource(Context context, String ip_address, String port, String display_name) {

        // Set up movie fields based on constructor
        IP_ADDRESS = ip_address;
        PORT = port;
        DISPLAY_NAME = display_name;
    }

    public String getMachine() {
        return IP_ADDRESS;
    }

    public void setMachine(String machine) {
        this.IP_ADDRESS = machine;
    }

    public String getPort() {
        return PORT;
    }

    public void setPort(String port) {
        this.PORT = port;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public void setDisplayName(String displayName) {
        this.DISPLAY_NAME = displayName;
    }
}