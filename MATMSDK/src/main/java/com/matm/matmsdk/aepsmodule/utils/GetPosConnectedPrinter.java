package com.matm.matmsdk.aepsmodule.utils;

import com.matm.matmsdk.btprinter.AEMPrinter;

public class GetPosConnectedPrinter {

    public GetPosConnectedPrinter(AEMPrinter aemPrinter) {
        this.aemPrinter = aemPrinter;
    }

    public AEMPrinter getAemPrinter() {
        return aemPrinter;
    }

    public void setAemPrinter(AEMPrinter aemPrinter) {
        this.aemPrinter = aemPrinter;
    }

    public static  AEMPrinter aemPrinter;

}
