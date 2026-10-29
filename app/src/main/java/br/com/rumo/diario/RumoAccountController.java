package br.com.rumo.diario;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.CancellationSignal;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.FragmentActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Native account UI. Passwords/tokens never enter the journal JavaScript bridge. */
public final class RumoAccountController {
    private final FragmentActivity activity;
    private final RumoVault vault;
    private final Runnable onAccountChanged;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private FirebaseAuth auth;
    private AlertDialog dialog, confirmation;
    private TextView status;
    private EditText email,password;
    private CheckBox adult;
    private LinearLayout fields;
    private CancellationSignal credentialRequest;
    private boolean busy,destroyed;

    public RumoAccountController(FragmentActivity activity,RumoVault vault,Runnable onAccountChanged) {this.activity=activity;this.vault=vault;this.onAccountChanged=onAccountChanged;}
    FirebaseAuth getAuth(){try{return initialize()?auth:null;}catch(Exception unavailable){return null;}}
    private void changed(){onAccountChanged.run();}
    public static boolean configured(android.content.Context context) {
        return context.getResources().getBoolean(R.bool.rumo_account_enabled)
            && !context.getString(R.string.rumo_firebase_api_key).trim().isEmpty()
            && !context.getString(R.string.rumo_firebase_project_id).trim().isEmpty()
            && !context.getString(R.string.rumo_firebase_app_id).trim().isEmpty();
    }
    private RumoStyle style(){return new RumoStyle(activity,vault);}
    private boolean visible(){return !destroyed&&dialog!=null&&dialog.isShowing()&&!activity.isFinishing();}
    private void message(String value){if(visible()&&status!=null)status.setText(value);}
    private void busy(boolean value){busy=value;if(fields!=null)setEnabled(fields,!value);}
    private void setEnabled(android.view.View view,boolean enabled){
        view.setEnabled(enabled);
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++)setEnabled(((android.view.ViewGroup)view).getChildAt(i),enabled);
    }
    private boolean initialize(){
        if(destroyed)return false;
        if(!configured(activity))return false;
        if(auth!=null)return true;
        FirebaseApp app;
        try {app=FirebaseApp.getInstance("RumoConta");}
        catch(IllegalStateException absent){
            app=FirebaseApp.initializeApp(activity,new FirebaseOptions.Builder()
                .setApiKey(activity.getString(R.string.rumo_firebase_api_key).trim())
                .setProjectId(activity.getString(R.string.rumo_firebase_project_id).trim())
                .setApplicationId(activity.getString(R.string.rumo_firebase_app_id).trim()).build(),"RumoConta");
            app.setDataCollectionDefaultEnabled(false);
        }
        auth=FirebaseAuth.getInstance(app);auth.setLanguageCode("pt-BR");return true;
    }
    public void show(){
        dismiss();
        if(!configured(activity)){
            dialog=style().dialog().setTitle("Sua conta no Rumo")
                .setMessage("O acesso com conta ainda não foi liberado nesta versão de testes. Seu diário continua funcionando neste aparelho. A conta de acesso e o PIN local são proteções diferentes.")
                .setPositiveButton("Entendi",null).show();return;
        }
        try{initialize();}catch(Exception error){dialog=style().dialog().setTitle("Conta indisponível").setMessage("Não foi possível abrir o acesso com conta. Seu diário foi preservado.").setPositiveButton("Fechar",null).show();return;}
        RumoStyle palette=style();fields=palette.column();status=palette.label("",14);
        FirebaseUser user=auth.getCurrentUser();
        if(user==null){
            fields.addView(palette.label("Seu próximo passo começa aqui. Entre na conta usada na compra ou crie a sua.",15));
            email=palette.input("Seu e-mail",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            password=palette.input("Senha da conta",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            fields.addView(palette.label("E-mail",13));fields.addView(email);fields.addView(palette.label("Senha",13));fields.addView(password);
            CheckBox reveal=new CheckBox(activity);reveal.setText("Mostrar senha");reveal.setTextColor(palette.text);reveal.setButtonTintList(android.content.res.ColorStateList.valueOf(palette.accent));
            reveal.setOnCheckedChangeListener((view,checked)->{password.setInputType(InputType.TYPE_CLASS_TEXT|(checked?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));password.setSelection(password.length());});fields.addView(reveal);
            adult=new CheckBox(activity);adult.setText("Tenho 18 anos ou mais");adult.setTextColor(palette.text);adult.setButtonTintList(android.content.res.ColorStateList.valueOf(palette.accent));fields.addView(adult);
            fields.addView(palette.button("Entrar com e-mail",()->emailSignIn(false)));
            fields.addView(palette.secondaryButton("Criar conta",()->emailSignIn(true)));
            if(!activity.getString(R.string.rumo_google_web_client_id).trim().isEmpty())fields.addView(palette.secondaryButton("Continuar com Google",this::google));
            fields.addView(palette.secondaryButton("Esqueci a senha da conta",this::reset));
            fields.addView(palette.label("A recuperação chega por um link de uso único no e-mail. Ela não redefine seu PIN nem a senha do backup.",13));
        }else{
            fields.addView(palette.label(user.getEmail()==null?"Conta conectada":user.getEmail(),18));
            fields.addView(palette.label("Entrar na conta não envia nem sincroniza seus check-ins. O PIN continua protegendo a abertura deste diário.",14));
            if(!user.isEmailVerified())fields.addView(palette.button("Enviar verificação do e-mail",()->{
                if(busy)return;busy(true);user.sendEmailVerification().addOnCompleteListener(task->{busy(false);message(task.isSuccessful()?"Confira seu e-mail para verificar a conta.":"Não foi possível enviar agora. Aguarde e tente novamente.");});
            }));
            fields.addView(palette.button("Conferir meu acesso",this::access));
            fields.addView(palette.secondaryButton("Sair da conta",this::signOut));
            fields.addView(palette.secondaryButton("Excluir conta de acesso",()->{ confirmation=style().dialog().setTitle("Excluir conta de acesso?")
                .setMessage("Seu diário local será mantido. A exclusão encerra o login e pode impedir a recuperação da licença vinculada a esta conta. Não é um pedido de reembolso. Registros de compra podem ser mantidos para obrigações legais. Se necessário, exporte um backup antes.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Excluir conta",(d,w)->{
                    if(busy)return;busy(true);user.delete().addOnCompleteListener(task->{busy(false);if(task.isSuccessful()){show();changed();}else message("Entre novamente na conta antes de solicitar a exclusão.");});
                }).show(); }));
        }
        fields.addView(status);ScrollView scroll=new ScrollView(activity);scroll.addView(fields);
        dialog=palette.dialog().setTitle("Sua conta no Rumo").setView(scroll).setNegativeButton("Fechar",null).show();
    }
    private boolean adultConfirmed(){if(adult!=null&&!adult.isChecked()){message("Confirme que você tem 18 anos ou mais para continuar.");return false;}return true;}
    private void emailSignIn(boolean create){
        if(busy||!visible()||!adultConfirmed())return;
        String address=email.getText().toString().trim(),secret=password.getText().toString();
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.setError("Confira seu e-mail.");return;}
        if(secret.isEmpty()||(create&&secret.length()<12)){password.setError(create?"Use pelo menos 12 caracteres.":"Digite sua senha.");return;}
        busy(true);password.setText("");message("Conectando…");
        com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> task=create?auth.createUserWithEmailAndPassword(address,secret):auth.signInWithEmailAndPassword(address,secret);
        task.addOnCompleteListener(result->{busy(false);if(result.isSuccessful()){
            if(create&&auth.getCurrentUser()!=null)auth.getCurrentUser().sendEmailVerification();
            if(visible())show();changed();
        }else message(create?"Não foi possível criar a conta. Confira os dados ou use a recuperação de senha.":"Não foi possível entrar. Confira os dados e sua conexão.");});
    }
    private void reset(){
        if(busy||!visible())return;String address=email.getText().toString().trim();
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){email.setError("Informe seu e-mail acima.");return;}
        busy(true);auth.sendPasswordResetEmail(address).addOnCompleteListener(task->{busy(false);message("Se esse e-mail puder receber recuperação, enviaremos as instruções. Confira também o spam e aguarde antes de tentar novamente.");});
    }
    private void google(){
        if(busy||!adultConfirmed())return;busy(true);message("Escolha sua conta Google…");credentialRequest=new CancellationSignal();
        GetGoogleIdOption option=new GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false)
            .setServerClientId(activity.getString(R.string.rumo_google_web_client_id).trim()).build();
        CredentialManager.create(activity).getCredentialAsync(activity,new GetCredentialRequest.Builder().addCredentialOption(option).build(),
            credentialRequest,ContextCompat.getMainExecutor(activity),new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>(){
                @Override public void onResult(GetCredentialResponse result){
                    try{
                        Credential credential=result.getCredential();
                        if(!(credential instanceof CustomCredential)||!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType()))throw new IllegalArgumentException();
                        String token=GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
                        auth.signInWithCredential(GoogleAuthProvider.getCredential(token,null)).addOnCompleteListener(task->{busy(false);if(task.isSuccessful()){if(visible())show();changed();}else message("Não foi possível entrar com Google. Tente novamente.");});
                    }catch(Exception error){busy(false);message("Não foi possível confirmar a conta Google.");}
                }
                @Override public void onError(GetCredentialException error){busy(false);message("Entrada com Google cancelada ou indisponível.");}
            });
    }
    private String origin(){return RumoAccountApi.origin(activity);}
    private void access(){
        FirebaseUser user=auth.getCurrentUser();if(user==null||busy)return;if(origin().isEmpty()){message("A consulta de compra ainda não foi liberada neste teste.");return;}
        busy(true);message("Conferindo seu acesso…");
        user.reload().addOnCompleteListener(reload->{
            if(!reload.isSuccessful()){busy(false);message("Conecte-se à internet para conferir o acesso.");return;}
            user.getIdToken(true).addOnCompleteListener(task->{
                if(destroyed)return;
                if(!task.isSuccessful()||task.getResult()==null){busy(false);message("Entre novamente na conta.");return;}
                String token=task.getResult().getToken(),base=origin();
                worker.execute(()->{
                    String result="Não foi possível consultar o acesso. Tente novamente.";HttpURLConnection connection=null;
                    try{
                        connection=(HttpURLConnection)new URL(base+"/api/access").openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
                        connection.setRequestProperty("Authorization","Bearer "+token);connection.setRequestProperty("Accept","application/json");
                        int code=connection.getResponseCode();
                        if(code==200){byte[] bytes;try(java.io.InputStream input=connection.getInputStream()){java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;while((n=input.read(buffer))!=-1){if(output.size()+n>8192)throw new java.io.IOException("Response too large");output.write(buffer,0,n);}bytes=output.toByteArray();}JSONObject data=new JSONObject(new String(bytes,StandardCharsets.UTF_8));result=data.optBoolean("active",false)?"Seu acesso Android está confirmado. O diário continua salvo neste aparelho.":"Nenhuma compra aprovada foi localizada para esta conta. Use Restaurar compra na tela de acesso.";}
                        else if(code==403)result="Verifique seu e-mail antes de consultar a compra.";
                    }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
                    final String outcome=result;activity.runOnUiThread(()->{busy(false);message(outcome);});
                });
            });
        });
    }
    private void signOut(){
        if(busy)return;auth.signOut();changed();
        CredentialManager.create(activity).clearCredentialStateAsync(new ClearCredentialStateRequest(),null,ContextCompat.getMainExecutor(activity),new CredentialManagerCallback<Void,ClearCredentialException>(){
            @Override public void onResult(Void result){}
            @Override public void onError(ClearCredentialException error){}
        });show();
    }
    public void dismiss(){if(confirmation!=null){confirmation.dismiss();confirmation=null;}if(dialog!=null){dialog.dismiss();dialog=null;}password=null;email=null;adult=null;status=null;fields=null;}
    public void destroy(){destroyed=true;dismiss();if(credentialRequest!=null)credentialRequest.cancel();worker.shutdownNow();}
}
