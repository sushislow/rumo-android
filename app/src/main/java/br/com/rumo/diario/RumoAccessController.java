package br.com.rumo.diario;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import com.android.billingclient.api.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Commercial gate before any diary HTML or JavaScript bridge becomes available. */
final class RumoAccessController {
    private final FragmentActivity activity;
    private final RumoVault vault;
    private final FrameLayout container;
    private final RumoAccountController accounts;
    private final RumoLicenseCache cache;
    private final Runnable afterAccess;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private ScrollView screen;
    private TextView status;
    private BillingClient billing;
    private android.app.AlertDialog purchaseDialog;
    private boolean busy,destroyed,connecting,storeReady;
    private volatile boolean allowed;
    private long epoch,lastRestoreAt;
    private String accountId="";
    private final List<Runnable> connectionActions=new ArrayList<>();
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable expiryCheck=new Runnable(){@Override public void run(){
        if(destroyed||!BuildConfig.REQUIRE_LICENSE)return;FirebaseUser value=user();
        if(allowed&&(value==null||!cache.valid(value.getUid()))){allowed=false;afterAccess.run();}
        handler.postDelayed(this,60000);
    }};
    RumoAccessController(FragmentActivity activity,RumoVault vault,FrameLayout container,RumoAccountController accounts,Runnable afterAccess){
        this.activity=activity;this.vault=vault;this.container=container;this.accounts=accounts;this.afterAccess=afterAccess;cache=new RumoLicenseCache(activity);
    }
    private FirebaseUser user(){FirebaseAuth auth=accounts.getAuth();return auth==null?null:auth.getCurrentUser();}
    boolean isAllowed(){return !BuildConfig.REQUIRE_LICENSE||allowed;}
    private boolean current(long request,String uid){if(destroyed||activity.isDestroyed())return false;FirebaseUser value=user();return epoch==request&&value!=null&&uid.equals(value.getUid());}
    private void message(String text){if(status!=null)status.setText(text);}
    boolean enter(){
        if(!BuildConfig.REQUIRE_LICENSE){allowed=true;return true;}
        FirebaseUser value=user();
        if(value!=null&&cache.valid(value.getUid())){
            allowed=true;removeScreen();if(cache.shouldRefresh()&&!busy)refresh(true);return true;
        }
        allowed=false;showScreen("Entre na sua conta para ativar o Rumo neste aparelho.");
        if(value!=null&&!busy)refresh(false);return false;
    }
    private void removeScreen(){if(screen!=null){container.removeView(screen);screen=null;status=null;}}
    private void showScreen(String text){
        removeScreen();RumoStyle style=new RumoStyle(activity,vault);
        LinearLayout body=style.column();body.setBackgroundColor(style.bg);body.setPadding(style.dp(26),style.dp(24),style.dp(26),style.dp(28));
        TextView brand=style.label("rumo.",25);brand.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);brand.setLetterSpacing(-.05f);body.addView(brand);
        body.addView(new RumoHorizonView(style),new LinearLayout.LayoutParams(-1,style.dp(130)));
        TextView overline=style.label("UM DIA DE CADA VEZ",11);overline.setTextColor(style.accent);overline.setLetterSpacing(.16f);body.addView(overline);
        TextView title=style.label("Seu próximo passo\ncomeça aqui.",33);title.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));title.setLetterSpacing(-.035f);title.setLineSpacing(0,1.05f);body.addView(title);
        TextView intro=style.label("Um espaço para entender seus hábitos e construir novas escolhas. No seu ritmo.",16);intro.setTextColor(style.muted);body.addView(intro);
        FirebaseUser value=user();
        LinearLayout card=style.column();card.setPadding(style.dp(18),style.dp(12),style.dp(18),style.dp(16));card.setBackground(style.box(style.panel,style.border));
        LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);cardParams.setMargins(0,style.dp(20),0,style.dp(16));card.setLayoutParams(cardParams);
        TextView account=style.label(value==null?"Seu espaço começa com uma conta":value.getEmail()==null?"Sua conta está conectada":value.getEmail(),15);account.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);card.addView(account);
        status=style.label(text,14);status.setTextColor(style.muted);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);card.addView(status);
        card.addView(style.button(value==null?"Entrar ou criar minha conta":"Atualizar meu acesso",value==null?accounts::show:()->refresh(false)));
        card.addView(style.secondaryButton(value==null?"Já comprei · conferir acesso":"Minha conta",value==null?()->refresh(false):accounts::show));
        body.addView(card);
        TextView purchase=style.label("Instalação gratuita. O acesso ao diário é liberado com uma compra única.",13);purchase.setTextColor(style.muted);body.addView(purchase);
        boolean purchaseReady=configured()&&activity.getResources().getBoolean(R.bool.rumo_play_billing_enabled);
        android.widget.Button buyButton=style.secondaryButton(purchaseReady?"Comprar pelo Google Play":"Compras em preparação",this::buy);buyButton.setEnabled(purchaseReady);if(!purchaseReady)buyButton.setAlpha(.55f);body.addView(buyButton);
        android.widget.Button restoreButton=style.secondaryButton("Restaurar compra do Google Play",this::restore);restoreButton.setEnabled(purchaseReady);if(!purchaseReady)restoreButton.setAlpha(.55f);body.addView(restoreButton);
        TextView privacy=style.label("Seu diário fica neste aparelho. O PIN é separado da senha da conta. Depois de ativar, conecte-se ao menos a cada 7 dias para conferir o acesso.",12);privacy.setTextColor(style.muted);privacy.setLineSpacing(style.dp(3),1);body.addView(privacy);
        FrameLayout centered=new FrameLayout(activity){@Override protected void onMeasure(int width,int height){super.onMeasure(MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(width),style.dp(560)),MeasureSpec.AT_MOST),height);}};
        centered.addView(body,new FrameLayout.LayoutParams(-1,-2));
        LinearLayout outer=new LinearLayout(activity);outer.setOrientation(LinearLayout.VERTICAL);outer.setGravity(Gravity.CENTER_HORIZONTAL);outer.addView(centered,new LinearLayout.LayoutParams(-1,-2));
        screen=new ScrollView(activity);screen.setBackgroundColor(style.bg);screen.setFillViewport(true);screen.addView(outer);container.addView(screen,new FrameLayout.LayoutParams(-1,-1));container.bringChildToFront(screen);
    }
    private boolean configured(){return RumoAccountController.configured(activity)&&!RumoAccountApi.origin(activity).isEmpty()&&!activity.getString(R.string.rumo_license_public_key).trim().isEmpty();}
    void accountChanged(){epoch++;busy=false;allowed=false;storeReady=false;accountId="";cache.clear();if(BuildConfig.REQUIRE_LICENSE)afterAccess.run();}
    void refresh(boolean silent){
        if(busy||destroyed)return;
        if(!configured()){if(!silent)message("O acesso comercial ainda está em configuração. Seu diário foi preservado.");return;}
        FirebaseUser value=user();if(value==null){if(!silent)accounts.show();return;}
        final String uid=value.getUid();final long request=epoch;busy=true;if(!silent)message("Conferindo seu acesso…");
        value.reload().addOnCompleteListener(reload->{
            if(!current(request,uid))return;
            if(!reload.isSuccessful()){busy=false;if(!silent)message("Conecte-se à internet e tente novamente. Seus registros estão preservados.");return;}
            if(!value.isEmailVerified()){busy=false;message("Verifique seu e-mail em Minha conta e atualize o acesso.");return;}
            value.getIdToken(true).addOnCompleteListener(result->{
                if(!current(request,uid))return;
                if(!result.isSuccessful()||result.getResult()==null){busy=false;message("Entre novamente na sua conta.");return;}
                final String token=result.getResult().getToken();
                worker.execute(()->{
                    RumoAccountApi.Reply access=null,lease=null;
                    try{access=RumoAccountApi.call(RumoAccountApi.origin(activity),"/api/access",token,null);if(access.status==200&&access.data.optBoolean("active"))lease=RumoAccountApi.call(RumoAccountApi.origin(activity),"/api/license",token,null);}catch(Exception ignored){}
                    final RumoAccountApi.Reply state=access,proof=lease;
                    activity.runOnUiThread(()->{
                        if(!current(request,uid))return;busy=false;
                        if(state==null||state.status>=500){if(!silent)message("Não foi possível confirmar agora. Confira sua conexão e tente novamente.");return;}
                        if(state.status==401||state.status==403){cache.clear();allowed=false;showScreen("Verifique o e-mail ou entre novamente na conta para conferir o acesso.");return;}
                        if(state.status!=200){if(!silent)message("A consulta está indisponível. Tente novamente.");return;}
                        accountId=state.data.optString("playAccountId","");storeReady=state.data.optBoolean("playBillingReady",false);
                        if(state.data.optBoolean("active",false)){
                            if(proof!=null&&proof.status==200&&cache.save(uid,proof.data.optString("license",""))){allowed=true;removeScreen();afterAccess.run();}
                            else if(!cache.valid(uid)){allowed=false;showScreen("Sua compra foi encontrada. A ativação ainda precisa ser concluída. Atualize o acesso em instantes.");}
                            return;
                        }
                        cache.clear();allowed=false;showScreen(state.data.optBoolean("pending",false)?"Seu pagamento está em confirmação. Aguarde e atualize o acesso.":"Esta conta ainda não tem uma compra aprovada. Você pode comprar uma vez ou restaurar uma compra do Google Play.");
                    });
                });
            });
        });
    }
    private boolean canPurchase(){
        if(!configured()||!activity.getResources().getBoolean(R.bool.rumo_play_billing_enabled)){message("As compras pelo Google Play ainda estão em preparação.");return false;}
        FirebaseUser value=user();if(value==null){accounts.show();return false;}
        if(!value.isEmailVerified()){message("Verifique o e-mail da sua conta antes de comprar.");return false;}
        if(!storeReady||accountId.isEmpty()){message("Primeiro, toque em Atualizar acesso para conferir sua conta.");refresh(false);return false;}
        return true;
    }
    private void connect(Runnable ready){
        if(destroyed)return;
        if(billing!=null&&billing.isReady()){ready.run();return;}
        connectionActions.add(ready);if(connecting)return;connecting=true;
        if(billing==null)billing=BillingClient.newBuilder(activity).setListener((result,purchases)->{
            if(destroyed)return;
            if(result.getResponseCode()==BillingClient.BillingResponseCode.OK&&purchases!=null)process(purchases);
            else if(result.getResponseCode()==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)restore();
            else if(result.getResponseCode()==BillingClient.BillingResponseCode.USER_CANCELED)message("Compra cancelada. Você pode tentar novamente quando quiser.");
            else message("O Google Play não conseguiu concluir agora. Tente novamente.");
        }).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).enableAutoServiceReconnection().build();
        billing.startConnection(new BillingClientStateListener(){
            @Override public void onBillingSetupFinished(BillingResult result){
                connecting=false;List<Runnable> actions=new ArrayList<>(connectionActions);connectionActions.clear();
                if(destroyed)return;if(result.getResponseCode()==BillingClient.BillingResponseCode.OK){for(Runnable action:actions)action.run();}else {busy=false;message("O Google Play está indisponível. Confira sua conta da loja e a instalação do aplicativo.");}
            }
            @Override public void onBillingServiceDisconnected(){connecting=false;busy=false;connectionActions.clear();}
        });
    }
    private void buy(){
        if(busy||!canPurchase())return;final FirebaseUser buyer=user();final String uid=buyer.getUid();final long request=epoch;
        busy=true;message("Buscando o preço no Google Play…");
        connect(()->billing.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder().setProductId(activity.getString(R.string.rumo_play_product_id)).setProductType(BillingClient.ProductType.INAPP).build())).build(),(result,details)->{
            if(!current(request,uid))return;busy=false;
            if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK||details.getProductDetailsList().isEmpty()){message("A compra ainda não está disponível nesta versão da loja.");return;}
            ProductDetails product=details.getProductDetailsList().get(0);List<ProductDetails.OneTimePurchaseOfferDetails> offers=product.getOneTimePurchaseOfferDetailsList();
            if(offers==null||offers.isEmpty()){message("O produto ainda está em configuração na loja.");return;}
            ProductDetails.OneTimePurchaseOfferDetails offer=offers.get(0);
            RumoStyle style=new RumoStyle(activity,vault);
            if(!activity.getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))return;
            purchaseDialog=style.dialog().setTitle("Liberar o Rumo")
                .setMessage("Compra única de "+offer.getFormattedPrice()+". Sem renovação automática. A licença ficará vinculada à conta do Rumo que está conectada.")
                .setNegativeButton("Agora não",null).setPositiveButton("Continuar",(dialog,which)->{
                    if(!current(request,uid))return;
                    BillingFlowParams.ProductDetailsParams item=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.getOfferToken()).build();
                    BillingResult launched=billing.launchBillingFlow(activity,BillingFlowParams.newBuilder().setProductDetailsParamsList(Collections.singletonList(item)).setObfuscatedAccountId(accountId).build());
                    if(launched.getResponseCode()==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)restore();else if(launched.getResponseCode()!=BillingClient.BillingResponseCode.OK)message("Não foi possível abrir o pagamento do Google Play.");
                }).show();
        }));
    }
    void restore(){
        if(busy||!canPurchase())return;final FirebaseUser buyer=user();final String uid=buyer.getUid();final long request=epoch;
        busy=true;lastRestoreAt=System.currentTimeMillis();message("Buscando suas compras no Google Play…");
        connect(()->billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),(result,purchases)->{
            if(!current(request,uid))return;busy=false;
            if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK){message("Não foi possível buscar suas compras. Tente novamente.");return;}
            process(purchases);
        }));
    }
    private void process(List<Purchase> purchases){
        if(busy||destroyed)return;FirebaseUser buyer=user();if(buyer==null)return;
        Purchase selected=null;for(Purchase purchase:purchases)if(purchase.getProducts().contains(activity.getString(R.string.rumo_play_product_id))){selected=purchase;break;}
        if(selected==null){message("Nenhuma compra deste aplicativo foi encontrada na conta da Play Store. Se comprou pelo site, use Atualizar acesso.");return;}
        if(selected.getPurchaseState()==Purchase.PurchaseState.PENDING){message("Seu pagamento está pendente. O acesso será liberado após a confirmação.");return;}
        if(selected.getPurchaseState()!=Purchase.PurchaseState.PURCHASED)return;
        final String uid=buyer.getUid(),purchaseToken=selected.getPurchaseToken();final long request=epoch;busy=true;message("Confirmando sua compra…");
        buyer.getIdToken(true).addOnCompleteListener(result->{
            if(!current(request,uid))return;
            if(!result.isSuccessful()||result.getResult()==null){busy=false;message("Entre novamente e use Restaurar compra.");return;}
            final String token=result.getResult().getToken();
            worker.execute(()->{
                RumoAccountApi.Reply reply=null;try{reply=RumoAccountApi.call(RumoAccountApi.origin(activity),"/api/play/verify",token,new JSONObject().put("purchaseToken",purchaseToken));}catch(Exception ignored){}
                final RumoAccountApi.Reply value=reply;
                activity.runOnUiThread(()->{
                    if(!current(request,uid))return;busy=false;
                    if(value!=null&&value.status==200){refresh(false);}else message(value!=null&&value.status==409?"Esta compra já está vinculada a outra conta do Rumo. Entre na conta usada na compra.":"Não foi possível confirmar agora. Conecte-se e use Restaurar compra. O diário foi preservado.");
                });
            });
        });
    }
    void onResume(){if(!BuildConfig.REQUIRE_LICENSE)return;handler.removeCallbacks(expiryCheck);handler.postDelayed(expiryCheck,60000);if(allowed){FirebaseUser value=user();if(value==null||!cache.valid(value.getUid())){allowed=false;afterAccess.run();}else if(cache.shouldRefresh())refresh(true);}else if(storeReady&&!busy&&System.currentTimeMillis()-lastRestoreAt>30000)restore();}
    void onStop(){handler.removeCallbacks(expiryCheck);if(purchaseDialog!=null){purchaseDialog.dismiss();purchaseDialog=null;}}
    void destroy(){destroyed=true;epoch++;onStop();connectionActions.clear();if(billing!=null)billing.endConnection();worker.shutdownNow();removeScreen();}
}
