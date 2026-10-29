package br.com.rumo.diario;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Native-only HTTPS transport; never accepts diary fields. */
final class RumoAccountApi {
    static final class Reply {
        final int status;final JSONObject data;
        Reply(int status,JSONObject data){this.status=status;this.data=data;}
    }
    static String origin(android.content.Context context){
        String value=context.getString(R.string.rumo_account_origin).trim();
        try{URL url=new URL(value);if(!"https".equals(url.getProtocol())||url.getUserInfo()!=null||url.getQuery()!=null||url.getRef()!=null||!url.getPath().isEmpty())return "";return value;}catch(Exception invalid){return "";}
    }
    static Reply call(String origin,String path,String token,JSONObject body)throws Exception{
        if(origin.isEmpty()||!path.matches("/api/(access|license|play/verify)"))throw new IllegalArgumentException();
        HttpURLConnection connection=(HttpURLConnection)new URL(origin+path).openConnection();
        try{
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
            connection.setRequestProperty("Authorization","Bearer "+token);connection.setRequestProperty("Accept","application/json");
            if(body!=null){connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(bytes.length);try(java.io.OutputStream out=connection.getOutputStream()){out.write(bytes);}}
            int status=connection.getResponseCode();java.io.InputStream stream=status<400?connection.getInputStream():connection.getErrorStream();
            if(stream==null)return new Reply(status,new JSONObject());
            try(java.io.InputStream input=stream;java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){
                byte[] buffer=new byte[2048];int n;while((n=input.read(buffer))!=-1){if(output.size()+n>16000)throw new java.io.IOException("Resposta grande demais");output.write(buffer,0,n);}
                return new Reply(status,new JSONObject(new String(output.toByteArray(),StandardCharsets.UTF_8)));
            }
        }finally{connection.disconnect();}
    }
}
