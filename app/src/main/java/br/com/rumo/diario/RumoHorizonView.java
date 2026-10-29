package br.com.rumo.diario;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Decorative horizon, drawn locally in the current theme. No personal data or animation. */
final class RumoHorizonView extends View {
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final RectF arch=new RectF();
    private final int accent;
    RumoHorizonView(RumoStyle style){
        super(style.activity);accent=style.accent;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();
        float scale=Math.min(getWidth()/320f,getHeight()/140f);
        canvas.translate((getWidth()-320*scale)/2,(getHeight()-140*scale)/2);canvas.scale(scale,scale);
        ink.setColor(accent);ink.setStrokeWidth(1.1f);ink.setStyle(Paint.Style.STROKE);ink.setAlpha(35);
        for(int i=0;i<3;i++){
            float inset=i*13;arch.set(93+inset,5+inset,227-inset,139-inset);
            path.reset();path.moveTo(93+inset,113);path.lineTo(93+inset,72);path.arcTo(arch,180,180);path.lineTo(227-inset,113);canvas.drawPath(path,ink);
        }
        ink.setStyle(Paint.Style.FILL);ink.setAlpha(30);canvas.drawCircle(160,65,21,ink);
        ink.setAlpha(230);canvas.drawCircle(160,65,10,ink);
        ink.setStyle(Paint.Style.STROKE);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeWidth(1.6f);
        path.reset();path.moveTo(129,137);path.cubicTo(109,114,201,120,181,99);path.cubicTo(169,88,151,97,160,82);canvas.drawPath(path,ink);
        ink.setAlpha(40);ink.setStrokeWidth(1);canvas.drawLine(42,113,278,113,ink);
        canvas.restore();
    }
}
