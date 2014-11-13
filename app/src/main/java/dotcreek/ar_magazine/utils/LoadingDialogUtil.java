/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package dotcreek.ar_magazine.utils;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.RelativeLayout;

import dotcreek.ar_magazine.R;


public final class LoadingDialogUtil extends Handler{

    // Constants for Hiding/Showing Loading dialog
    public static final int HIDE_LOADING_DIALOG = 0;
    public static final int SHOW_LOADING_DIALOG = 1;

    private View vLoadingDialogContainer;

    public LoadingDialogUtil(RelativeLayout layout){

        vLoadingDialogContainer = layout.findViewById(R.id.loading_indicator);

    }

    public void handleMessage(Message message){
        
        if (message.what == SHOW_LOADING_DIALOG){

            vLoadingDialogContainer.setVisibility(View.VISIBLE);
            
        }

        else if (message.what == HIDE_LOADING_DIALOG){

            vLoadingDialogContainer.setVisibility(View.GONE);
        }
    }
    
}
