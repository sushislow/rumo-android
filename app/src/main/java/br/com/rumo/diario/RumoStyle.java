package br.com.rumo.diario;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One palette for the native PIN/account surfaces and the local diary. */
public final class RumoStyle {
    public final Activity activity;
    public final int bg, panel, text, muted, accent, onAccent, border;
    public final boolean light;

    public RumoStyle(Activity activity, RumoVault vault) {
        this.activity = activity;
        String theme = "oceano";
        try { theme = new org.json.JSONObject(vault.get("rumo_preferencias_v1")).optString("theme", "oceano"); }
        catch (Exception ignored) { }
        String[] colors;
        switch (theme) {
            case "violeta": colors = new String[]{"#171326","#211c33","#faf5ff","#c0b5d4","#d8b4fe","#20132f","#514363"}; break;
            case "floresta": colors = new String[]{"#0b201c","#133029","#f1fff8","#b2d0c5","#9af0a9","#10291a","#366052"}; break;
            case "areia": colors = new String[]{"#f8f5ec","#fffdfa","#252920","#5c554a","#14694c","#ffffff","#b5ab97"}; break;
            case "aurora": colors = new String[]{"#211624","#302035","#fff4fc","#d2b7cd","#f5b7dd","#311327","#654663"}; break;
            case "grafite": colors = new String[]{"#14171c","#20252d","#f3f5f8","#bac3cf","#d1d9e6","#20252d","#525e70"}; break;
            case "ceu": colors = new String[]{"#eef5fc","#ffffff","#152d43","#496176","#235c91","#ffffff","#a6bfd5"}; break;
            case "terracota": colors = new String[]{"#faf2ed","#fffcfa","#38251e","#705548","#9d462e","#ffffff","#c6ada0"}; break;
            default: colors = new String[]{"#0b1421","#142333","#f0f8fa","#a8becb","#78e1bb","#0b2920","#355064"};
        }
        bg=Color.parseColor(colors[0]); panel=Color.parseColor(colors[1]); text=Color.parseColor(colors[2]);
        muted=Color.parseColor(colors[3]); accent=Color.parseColor(colors[4]); onAccent=Color.parseColor(colors[5]); border=Color.parseColor(colors[6]);
        light = theme.equals("areia") || theme.equals("ceu") || theme.equals("terracota");
    }
    public int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    public GradientDrawable box(int color, int stroke) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(18));
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(1),stroke); return shape;
    }
    public LinearLayout column() {
        LinearLayout layout=new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24),dp(16),dp(24),dp(20)); layout.setBackgroundColor(panel); return layout;
    }
    public TextView label(String value,int size) {
        TextView view=new TextView(activity); view.setText(value); view.setTextSize(size); view.setTextColor(text);
        view.setPadding(0,dp(10),0,dp(10)); return view;
    }
    public EditText input(String hint,int type) {
        EditText input=new EditText(activity); input.setHint(hint); input.setTextColor(text); input.setHintTextColor(muted);
        input.setTextSize(16); input.setSingleLine(true); input.setInputType(type);
        input.setPadding(dp(16),dp(15),dp(16),dp(15)); input.setMinHeight(dp(54));
        StateListDrawable background=new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused},box(bg,accent)); background.addState(new int[]{},box(bg,border));
        input.setBackground(background); input.setSaveEnabled(false);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.setMargins(0,dp(5),0,dp(7)); input.setLayoutParams(params);
        if (android.os.Build.VERSION.SDK_INT>=26) input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        return input;
    }
    public Button button(String value,Runnable action) {
        Button view=new Button(activity); view.setAllCaps(false); view.setText(value); view.setTextSize(15);
        view.setTypeface(Typeface.DEFAULT,Typeface.BOLD); view.setTextColor(onAccent); view.setBackground(box(accent,Color.TRANSPARENT));
        view.setMinHeight(dp(52)); view.setPadding(dp(14),dp(12),dp(14),dp(12));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.setMargins(0,dp(6),0,dp(6)); view.setLayoutParams(params);
        view.setOnClickListener(v->action.run()); return view;
    }
    public AlertDialog.Builder dialog() {
        RumoStyle palette=this;
        return new AlertDialog.Builder(new ContextThemeWrapper(activity,light?
                android.R.style.Theme_Material_Light_Dialog_Alert:android.R.style.Theme_Material_Dialog_Alert)) {
            @Override public AlertDialog show() { AlertDialog dialog=super.show(); palette.style(dialog); return dialog; }
        };
    }
    public void style(AlertDialog dialog) {
        Window window=dialog.getWindow();
        if(window!=null) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
            window.setBackgroundDrawable(box(panel,border)); colorTree(window.getDecorView());
        }
        for(int id:new int[]{-1,-2,-3}) { Button button=dialog.getButton(id); if(button!=null){button.setTextColor(accent);button.setAllCaps(false);} }
    }
    private void colorTree(View view) {
        if(view instanceof EditText) return;
        if(view instanceof android.widget.CompoundButton) { ((android.widget.CompoundButton)view).setTextColor(text); ((android.widget.CompoundButton)view).setButtonTintList(ColorStateList.valueOf(accent)); return; }
        if(view instanceof Button) { ((Button)view).setTextColor(onAccent); return; }
        if(view instanceof TextView) ((TextView)view).setTextColor(text);
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)colorTree(((ViewGroup)view).getChildAt(i));
    }
}
