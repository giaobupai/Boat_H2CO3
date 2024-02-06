package org.koishi.launcher.h2co3.control.input.log;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

import org.koishi.launcher.h2co3.control.controller.Controller;
import org.koishi.launcher.h2co3.control.input.Input;
import org.koishi.launcher.h2co3.launcher.H2CO3LauncherLoader;
import org.koishi.launcher.h2co3.core.H2CO3Tools;
import org.koishi.launcher.h2co3.core.login.utils.DisplayUtils;
import org.koishi.launcher.h2co3.core.utils.file.FileTools;
import org.koishi.launcher.h2co3.resources.component.LineTextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class DebugInfo implements Input, View.OnClickListener {
    private Context mContext;
    private Controller mController;
    private boolean isEnabled;

    private LogView mLogView;
    private boolean firstWrite = true;
    private boolean isWrite = true;

    @Override
    public boolean load(Context context, Controller controller) {
        this.mContext = context;
        this.mController = controller;

        mLogView = new LogView(mContext);
        mLogView.setLayoutParams(new ViewGroup.LayoutParams(mController.getConfig().getScreenWidth() - DisplayUtils.getPxFromDp(mContext, 10), mController.getConfig().getScreenHeight() - DisplayUtils.getPxFromDp(mContext, 30)));
        mController.addView(mLogView);
        mLogView.setX(0);
        mLogView.setY(mController.getConfig().getScreenHeight() - mLogView.getLayoutParams().height);

        if (H2CO3LauncherLoader.logReceiver == null || H2CO3LauncherLoader.logReceiver.getLogs() == null) {
            H2CO3LauncherLoader.LogReceiver mReceiver = new H2CO3LauncherLoader.LogReceiver() {
                final StringBuilder stringBuilder = new StringBuilder();

                @Override
                public void pushLog(String log) {
                    mLogView.appendLog(log);
                    stringBuilder.append(log);
                    writeLog(log);
                }

                @Override
                public String getLogs() {
                    return stringBuilder.toString();
                }
            };
            H2CO3LauncherLoader.logReceiver = new WeakReference<>(mReceiver).get();
        }

        return true;
    }

    @Override
    public boolean unload() {
        ViewGroup vg = (ViewGroup) mLogView.getParent();
        vg.removeView(mLogView);
        H2CO3LauncherLoader.logReceiver = null;
        return true;
    }

    @Override
    public void setGrabCursor(boolean isGrabbed) {

    }

    @Override
    public void runConfigure() {

    }

    @Override
    public void saveConfig() {

    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        mLogView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPaused() {

    }

    @Override
    public void onResumed() {

    }

    @Override
    public Controller getController() {
        return mController;
    }

    @Override
    public void onClick(View v) {
    }

    private void writeLog(String log) {
        if (!isWrite)
            return;
        File logFile = new File(H2CO3Tools.LOG_DIR + "/client_output.txt");
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile()) {
                    isWrite = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (firstWrite) {
            FileTools.writeData(logFile.getAbsolutePath(), log);
            firstWrite = false;
        } else {
            FileTools.addStringLineToFile(log, logFile);
        }
    }

    public class LogView extends NestedScrollView {

        private final TextView mTextView;

        public LogView(@NonNull Context context) {
            super(context);
            this.setBackground(getViewBackground());
            this.mTextView = new LineTextView(context);

            mTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(mTextView);
            mTextView.setTextColor(Color.WHITE);
            mTextView.setTextIsSelectable(true);
            mTextView.setTextSize(DisplayUtils.getPxFromSp(mContext, 3.2F));
            mTextView.setLineSpacing(0, 1f);
        }

        public void appendLog(String str) {
            this.post(() -> {
                if (mTextView != null) {
                    mTextView.append(str);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> toBottom(this, mTextView), 50);
                }
            });
        }

        private void toBottom(final NestedScrollView scrollView, final View view) {
            int offset = view.getHeight() - scrollView.getHeight();
            if (offset < 0) {
                offset = 0;
            }
            scrollView.scrollTo(0, offset);
        }

        private LayerDrawable getViewBackground() {
            int radiusSize = 0;
            int mainColor = Color.parseColor("#7f5B5B5B");

            float[] outerR = new float[]{radiusSize, radiusSize, radiusSize, radiusSize, radiusSize, radiusSize, radiusSize, radiusSize};
            RoundRectShape rectShape = new RoundRectShape(outerR, null, null);
            ShapeDrawable shapeDrawable = new ShapeDrawable();
            shapeDrawable.setShape(rectShape);
            shapeDrawable.getPaint().setStyle(Paint.Style.FILL);
            shapeDrawable.getPaint().setColor(mainColor);

            Drawable[] layers = new Drawable[]{shapeDrawable};

            return new LayerDrawable(layers);
        }
    }
}