package com.alexismiele.epifeu;

public class EpiFeu {
    private String mIp;
    private String mCookie;
    private String mName;
    private String mPhoto;

    public EpiFeu() {
        String intraCookie = android.webkit.CookieManager.getInstance().getCookie("https://intra.epitech.eu/");
    }


}
