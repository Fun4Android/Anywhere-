package com.absinthe.anywhere_.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.absinthe.anywhere_.services.OverlayService;
import com.absinthe.anywhere_.ui.main.MainActivity;
import com.absinthe.anywhere_.utils.CommandUtils;
import com.absinthe.anywhere_.utils.UiUtils;
import com.absinthe.anywhere_.utils.manager.Logger;
import com.absinthe.anywhere_.viewbuilder.entity.OverlayBuilder;

public class OverlayView extends LinearLayout {

    private final Context mContext;
    private final WindowManager mWindowManager;
    private OverlayBuilder mBuilder;
    private final int mTouchSlop;

    private WindowManager.LayoutParams mLayoutParams;

    private String mCommand;
    private String mPkgName;

    private boolean isClick;
    private long mStartTime = 0;
    private long mEndTime = 0;

    private Runnable removeWindowTask = new Runnable() {
        @Override
        public void run() {
            mBuilder.ivIcon.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            mContext.startService(
                    new Intent(mContext, OverlayService.class)
                            .putExtra(OverlayService.COMMAND, OverlayService.COMMAND_CLOSE)
            );
        }
    };

    public OverlayView(Context context) {
        super(context);
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        initView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mBuilder = new OverlayBuilder(mContext, this);

        mBuilder.ivIcon.setOnClickListener(v -> {
            Logger.d("Overlay window clicked!");

            CommandUtils.execCmd(MainActivity.getInstance(), mCommand);
        });

        mBuilder.ivIcon.setOnTouchListener(new OnTouchListener() {

            private float lastX; //上一次位置的X.Y坐标
            private float lastY;
            private float nowX;  //当前移动位置的X.Y坐标
            private float nowY;
            private float tranX; //悬浮窗移动位置的相对值
            private float tranY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mLayoutParams = (WindowManager.LayoutParams) OverlayView.this.getLayoutParams();
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 获取按下时的X，Y坐标
                        lastX = motionEvent.getRawX();
                        lastY = motionEvent.getRawY();
                        Logger.d("MotionEvent.ACTION_DOWN last:", lastX, lastY);

                        isClick = false;
                        mStartTime = System.currentTimeMillis();
                        postDelayed(removeWindowTask, 1000);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        isClick = true;

                        // 获取移动时的X，Y坐标
                        nowX = motionEvent.getRawX();
                        nowY = motionEvent.getRawY();
                        Logger.d("MotionEvent.ACTION_MOVE now:", nowX, nowY);

                        // 计算XY坐标偏移量
                        tranX = nowX - lastX;
                        tranY = nowY - lastY;
                        Logger.d("MotionEvent.ACTION_MOVE tran:", tranX, tranY);

                        if (tranX * tranX + tranY * tranY > mTouchSlop * mTouchSlop) {
                            removeCallbacks(removeWindowTask);
                        }

                        // 移动悬浮窗
                        mLayoutParams.x -= tranX;
                        mLayoutParams.y += tranY;
                        //更新悬浮窗位置
                        mWindowManager.updateViewLayout(OverlayView.this, mLayoutParams);
                        //记录当前坐标作为下一次计算的上一次移动的位置坐标
                        lastX = nowX;
                        lastY = nowY;

                        break;
                    case MotionEvent.ACTION_UP:
                        mEndTime = System.currentTimeMillis();
                        Logger.d("Touch period =", (mEndTime - mStartTime));

                        isClick = (mEndTime - mStartTime) > 0.2 * 1000L;
                        removeCallbacks(removeWindowTask);
                        break;
                }
                return isClick;
            }
        });
    }

    public String getCommand() {
        return mCommand;
    }

    public void setCommand(String mCommand) {
        this.mCommand = mCommand;
    }

    public String getPkgName() {
        return mPkgName;
    }

    public void setPkgName(String mPkgName) {
        this.mPkgName = mPkgName;
        mBuilder.ivIcon.setImageDrawable(UiUtils.getAppIconByPackageName(mContext, mPkgName));
    }
}

