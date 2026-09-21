package com.personal.messageforwarder.diagnostics;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;

final class AppUi {
    static final int INK=Color.rgb(36,50,61), MUTED=Color.rgb(102,119,130), PRIMARY=Color.rgb(53,122,120);
    static int dp(Activity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    static TextView text(Activity a,String value,float size,int color){
        TextView v=new TextView(a);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(0,1.12f);return v;
    }
    static TextView title(Activity a,String value){TextView v=text(a,value,29,INK);v.setTypeface(null,android.graphics.Typeface.BOLD);return v;}
    static Button button(Activity a,String value,boolean primary){
        Button b=new Button(a);b.setText(value);b.setTextSize(16);b.setAllCaps(false);b.setMinHeight(dp(a,54));b.setTextColor(primary?Color.WHITE:INK);
        b.setBackgroundTintList(ColorStateList.valueOf(primary?PRIMARY:Color.rgb(231,239,237)));return b;
    }
    static GradientDrawable panel(int color,int stroke,float radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);if(stroke!=Color.TRANSPARENT)g.setStroke(1,stroke);return g;
    }
    static LinearLayout page(Activity a){
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);int p=dp(a,22);root.setPadding(p,p,p,dp(a,36));
        root.setBackgroundColor(Color.rgb(245,248,247));root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(p+b.left,p+b.top,p+b.right,dp(a,36)+b.bottom);return i;});return root;
    }
    static void gap(Activity a,LinearLayout parent,int size){Space s=new Space(a);parent.addView(s,new LinearLayout.LayoutParams(1,dp(a,size)));}
    static void showScrollable(Activity a,LinearLayout root){ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(root);a.setContentView(scroll);}
}
