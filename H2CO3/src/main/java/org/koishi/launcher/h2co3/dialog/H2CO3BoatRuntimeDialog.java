package org.koishi.launcher.h2co3.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.koishi.launcher.h2co3.R;
import org.koishi.launcher.h2co3.adapter.BaseRecycleAdapter;
import org.koishi.launcher.h2co3.core.H2CO3Game;
import org.koishi.launcher.h2co3.core.H2CO3Tools;
import org.koishi.launcher.h2co3.core.utils.data.DbDao;
import org.koishi.launcher.h2co3.resources.component.H2CO3CardView;
import org.koishi.launcher.h2co3.resources.component.dialog.H2CO3CustomViewDialog;
import org.koishi.launcher.h2co3.ui.fragment.directory.DirectoryFragment;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class H2CO3BoatRuntimeDialog extends H2CO3CustomViewDialog implements View.OnClickListener{

    public Context context;

    H2CO3CardView button_jre_8,button_jre_17;
    public H2CO3BoatRuntimeDialog(Context context) {
        super(context);
        this.context = context;
        setTitle(org.koishi.launcher.h2co3.resources.R.string.title_runtime);
        this.setCustomView(R.layout.custom_dialog_runtime);
        initViews();

        // Custom button behavior without dismiss
    }

    public void initViews() {
        button_jre_8 = findViewById(R.id.button_jre_8);
        button_jre_8.setOnClickListener(this);
        button_jre_17 = findViewById(R.id.button_jre_17);
        button_jre_17.setOnClickListener(this);

        if (H2CO3Game.getJavaPath().equals(H2CO3Tools.JAVA_8_PATH)){
            button_jre_8.setStrokeWidth(13);
            button_jre_17.setStrokeWidth(0);
        } else if (H2CO3Game.getJavaPath().equals(H2CO3Tools.JAVA_17_PATH)) {
            button_jre_17.setStrokeWidth(13);
            button_jre_8.setStrokeWidth(0);
        }else {
            H2CO3Game.setJavaPath(H2CO3Tools.JAVA_17_PATH);
            button_jre_17.setStrokeWidth(13);
            button_jre_8.setStrokeWidth(0);
        }
    }

    /** Show the dialog, refreshes the adapter data before showing it */
    @SuppressLint("NotifyDataSetChanged") //only used to completely refresh the list, it is necessary
    @Override
    public AlertDialog show() {
        return super.show();
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v == button_jre_8){
            H2CO3Game.setJavaPath(H2CO3Tools.JAVA_8_PATH);
            button_jre_8.setStrokeWidth(13);
            button_jre_17.setStrokeWidth(0);
        } else if (v == button_jre_17){
            H2CO3Game.setJavaPath(H2CO3Tools.JAVA_17_PATH);
            button_jre_17.setStrokeWidth(13);
            button_jre_8.setStrokeWidth(0);
        }
    }
}