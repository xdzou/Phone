/*--------------------------------------------------------------------------
Copyright (c) 2010, Code Aurora Forum. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Code Aurora nor
      the names of its contributors may be used to endorse or promote
      products derived from this software without specific prior written
      permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--------------------------------------------------------------------------*/

package com.android.phone;

/* Constants used for ICC Depersonalization.*/
public class IccDepersonalizationConstants {
    //These are supported depersonalization types.
    public static final int ICC_PERSO_NOT_SUPPORTED = -1;
    public static final int ICC_SIM_NETWORK = 0;
    public static final int ICC_SIM_NETWORK_SUBSET = 1;
    public static final int ICC_SIM_CORPORATE = 2;
    public static final int ICC_SIM_SERVICE_PROVIDER = 3;
    public static final int ICC_SIM_SIM = 4;
    public static final int ICC_RUIM_NETWORK1 = 5;
    public static final int ICC_RUIM_NETWORK2 = 6;
    public static final int ICC_RUIM_HRPD = 7;
    public static final int ICC_RUIM_CORPORATE = 8;
    public static final int ICC_RUIM_SERVICE_PROVIDER = 9;
    public static final int ICC_RUIM_RUIM = 10;

    //This is how phoneApp constants relate to constants from RIL_PersoSubstate in ril.h
    public static final int[] DEPERSO_TYPES = {
        3,  //ICC_SIM_NETWORK
        4,  //ICC_SIM_NETWORK_SUBSET
        5,  //ICC_SIM_CORPORATE
        6,  //ICC_SIM_SERVICE_PROVIDER
        7,  //ICC_SIM_SIM
        13, //ICC_RUIM_NETWORK1
        14, //ICC_RUIM_NETWORK2
        15, //ICC_RUIM_HRPD
        16, //ICC_RUIM_CORPORATE
        17, //ICC_RUIM_SERVICE_PROVIDER
        18  //ICC_RUIM_RUIM
    };
}
