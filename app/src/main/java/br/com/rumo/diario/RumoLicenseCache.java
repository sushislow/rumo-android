package br.com.rumo.diario;

import android.content.Context;
import android.content.SharedPreferences;

/** Private cache of a signed authorization, independent of the encrypted diary. */
final class RumoLicenseCache {
    private final Context context;
    private final SharedPreferences preferences;
    RumoLicenseCache(Context context){this.context=context;preferences=context.getSharedPreferences("rumo_license_v1",Context.MODE_PRIVATE);}
    boolean valid(String uid){
        long now=System.currentTimeMillis(),previous=preferences.getLong("lastWall",0);
        if(now+300000<previous)return false;
        boolean allowed=LicenseVerifier.valid(preferences.getString("token",""),context.getString(R.string.rumo_license_public_key),
            context.getString(R.string.rumo_account_origin).trim(),uid,context.getPackageName(),context.getString(R.string.rumo_purchase_mode),now/1000);
        if(allowed)preferences.edit().putLong("lastWall",Math.max(previous,now)).apply();return allowed;
    }
    boolean save(String uid,String token){
        if(!LicenseVerifier.valid(token,context.getString(R.string.rumo_license_public_key),context.getString(R.string.rumo_account_origin).trim(),uid,context.getPackageName(),context.getString(R.string.rumo_purchase_mode),System.currentTimeMillis()/1000))return false;
        return preferences.edit().putString("token",token).putLong("lastWall",System.currentTimeMillis()).putLong("checkedAt",System.currentTimeMillis()).commit();
    }
    boolean shouldRefresh(){return System.currentTimeMillis()-preferences.getLong("checkedAt",0)>24L*3600000;}
    void clear(){preferences.edit().remove("token").remove("checkedAt").apply();}
}
