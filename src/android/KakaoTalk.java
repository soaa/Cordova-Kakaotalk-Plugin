package com.htj.plugin.kakao;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.kakao.auth.ApiResponseCallback;
import com.kakao.auth.ApprovalType;
import com.kakao.auth.AuthType;
import com.kakao.auth.IApplicationConfig;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.ISessionConfig;
import com.kakao.auth.KakaoAdapter;
import com.kakao.auth.KakaoSDK;
import com.kakao.auth.Session;
import com.kakao.kakaolink.v2.KakaoLinkResponse;
import com.kakao.kakaolink.v2.KakaoLinkService;
import com.kakao.message.template.ButtonObject;
import com.kakao.message.template.ContentObject;
import com.kakao.message.template.FeedTemplate;
import com.kakao.message.template.LinkObject;
import com.kakao.message.template.TemplateParams;
import com.kakao.network.ErrorResult;
import com.kakao.network.callback.ResponseCallback;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.KakaoParameterException;
import com.kakao.util.exception.KakaoException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class KakaoTalk extends CordovaPlugin {

    private static final String LOG_TAG = "KakaoTalk";

    /**
     * Initialize cordova plugin kakaotalk
     *
     * @param cordova
     * @param webView
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.v(LOG_TAG, "kakao : initialize");
        super.initialize(cordova, webView);
    }

    /**
     * Execute plugin
     *
     * @param action
     * @param options
     * @param callbackContext
     */
    public boolean execute(final String action, final JSONArray options, final CallbackContext callbackContext) throws JSONException {
        Log.v(LOG_TAG, "kakao : execute " + action);

        if (KakaoSDK.getAdapter() == null) {
            try {
                Log.v(LOG_TAG, "kakao exec " + action);
                KakaoSDK.init(new KakaoSDKAdapter(cordova.getActivity().getApplication()));
            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
            }
        }

        cordova.setActivityResultCallback(this);

        if (action.equals("login")) {
            Session.getCurrentSession().addCallback(new SessionCallback(callbackContext));
            this.login(callbackContext);
            //requestMe(callbackContext);
            return true;
        } else if (action.equals("logout")) {
            this.logout(callbackContext);
            return true;
        } else if (action.equals("share")) {
            return false;
        } else if (action.equals("link")) {
            // Kakao Link v2
            try {
                link(options, callbackContext);
                return true;
            } catch (Exception ex) {
                callbackContext.error(ex.getMessage());
                Log.e(LOG_TAG, ex.getMessage(), ex);
                return false;
            }
        } else if (action.equals("canShareViaStory")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Uri kakaoLinkTestUri = Uri.parse("storylink://posting");
                        Intent intent = new Intent(Intent.ACTION_SEND, kakaoLinkTestUri);
                        List<ResolveInfo> list = cordova.getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                        callbackContext.success((list != null && !list.isEmpty()) ? 1 : 0);

                    } catch (Exception ex) {
                        Log.e(LOG_TAG, ex.getMessage(), ex);
                        callbackContext.error(ex.getMessage());
                    }
                }
            });

            return true;
        } else if (action.equals("shareViaStory")) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject params = options.getJSONObject(0);

                        if (params != null) {
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("text/plain");
                            i.putExtra(Intent.EXTRA_SUBJECT, params.getString("text"));
                            i.putExtra(Intent.EXTRA_TEXT, params.getString("url"));

                            i.setPackage("com.kakao.story");
                            cordova.getActivity().startActivity(i);

                            callbackContext.success("success");
                        } else {
                            callbackContext.error("empty params");
                        }
                    } catch (Exception ex) {
                        callbackContext.error(ex.getMessage());
                    }
                }
            });

            return true;
        }
        return false;
    }

    /**
     * KakaoLink v2 지원용 method
     *
     * @param options         - kakaolink javascript sdk 에 정의된 payload
     * @param callbackContext
     * @throws JSONException
     */
    private void link(JSONArray options, final CallbackContext callbackContext) throws JSONException, KakaoParameterException {
        JSONObject parameters = options.getJSONObject(0);
        JSONObject content = parameters.getJSONObject("content");

        ContentObject.Builder contentObjectBuilder = ContentObject.newBuilder(
                content.getString("title"),
                content.getString("imageUrl"),
                parseLinkObject(content.getJSONObject("link"))
        ).setDescrption(content.optString("description", null));

        if (content.has("imageWidth"))
            contentObjectBuilder.setImageWidth(content.getInt("imageWidth"));
        if (content.has("imageHeight"))
            contentObjectBuilder.setImageHeight(content.getInt("imageHeight"));

        final ContentObject contentObject = contentObjectBuilder.build();
        final TemplateParams params;

        // TODO : 일단 Feed Template 만 지원
        if ("feed".equals(parameters.optString("objectType"))) {
            FeedTemplate.Builder paramsBuilder = FeedTemplate.newBuilder(contentObject);

            // button 설정 부분은 javascript sdk 설명대로 구현한다.
            // buttons 가 buttonTitle 에 우선
            if (parameters.has("buttons")) {
                JSONArray buttons = parameters.getJSONArray("buttons");
                for (int i = 0; i < buttons.length(); i++) {
                    JSONObject button = buttons.getJSONObject(i);
                    paramsBuilder.addButton(new ButtonObject(button.getString("title"), parseLinkObject(button.getJSONObject("link"))));
                }
            } else if (parameters.has("buttonTitle")) {
                // 기본 버튼
                paramsBuilder.addButton(new ButtonObject(parameters.getString("buttonTitle"), contentObject.getLink()));
            }

            params = paramsBuilder.build();

        } else {
            throw new KakaoParameterException("Unsupported object type : " + parameters.optString("objectType"));
        }

        final Activity activity = cordova.getActivity();
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                KakaoLinkService.getInstance().sendDefault(activity, params, new ResponseCallback<KakaoLinkResponse>() {
                    @Override
                    public void onFailure(ErrorResult errorResult) {
                        callbackContext.error(errorResult.getErrorMessage());
                    }

                    @Override
                    public void onSuccess(KakaoLinkResponse result) {
                        callbackContext.success(result.getTemplateMsg());
                    }
                });
            }
        });
    }

    private LinkObject parseLinkObject(JSONObject jobj) {
        return LinkObject.newBuilder()
                .setWebUrl(jobj.optString("webUrl", null))
                .setMobileWebUrl(jobj.optString("mobileWebUrl", jobj.optString("webUrl", null)))
                .setAndroidExecutionParams(jobj.optString("androidExecParams", null))
                .setIosExecutionParams(jobj.optString("iosExecParams", null))
                .build();
    }

    /**
     * Log in
     */
    private void login(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final KakaoLoginSelectDialog dialog = new KakaoLoginSelectDialog((Context) cordova.getActivity(), new KOAuthSelectCallback() {
                    @Override
                    public void onSelect(final AuthType authType) {
                        Session.getCurrentSession().open(authType, cordova.getActivity());
                    }

                    @Override
                    public void onCancel() {
                        callbackContext.error("selection canceled");
                    }
                });

                dialog.open();
            }
        });
    }



    /**
     * Log out
     *
     * @param callbackContext
     */
    private void logout(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
                    @Override
                    public void onCompleteLogout() {
                        Log.v(LOG_TAG, "kakao : onCompleteLogout");
                        callbackContext.success();
                    }
                });
            }
        });
    }

    /**
     * On activity result
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(LOG_TAG, "kakao : onActivityResult : " + requestCode + ", code: " + resultCode);
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, intent)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    /**
     * Result
     *
     * @param userProfile
     */
    private JSONObject handleResult(MeV2Response userProfile) {
        Log.v(LOG_TAG, "kakao : handleResult");
        JSONObject response = new JSONObject();
        try {
            response.put("id", userProfile.getId());
            response.put("nickname", userProfile.getNickname());
            response.put("profile_image", userProfile.getProfileImagePath());
            response.put("access_token", Session.getCurrentSession().getAccessToken());
            response.put("refresh_token", Session.getCurrentSession().getRefreshToken());
        } catch (JSONException e) {
            Log.v(LOG_TAG, "kakao : handleResult error - " + e.toString());
        }
        return response;
    }


    /**
     * Class SessonCallback
     */
    private class SessionCallback implements ISessionCallback {

        private CallbackContext callbackContext;

        public SessionCallback(final CallbackContext callbackContext) {
            this.callbackContext = callbackContext;
        }

        @Override
        public void onSessionOpened() {
            Log.v(LOG_TAG, "kakao : SessionCallback.onSessionOpened");
            UserManagement.getInstance().me(new MeV2ResponseCallback() {
                @Override
                public void onFailure(ErrorResult errorResult) {
                    callbackContext.error("kakao : SessionCallback.onSessionOpened.requestMe.onFailure - " + errorResult);
                }

                @Override
                public void onSuccess(MeV2Response result) {
                    callbackContext.success(handleResult(result));
                }

                @Override
                public void onSessionClosed(ErrorResult errorResult) {
                    Log.v(LOG_TAG, "kakao : SessionCallback.onSessionOpened.requestMe.onSessionClosed - " + errorResult);
                    Session.getCurrentSession().checkAndImplicitOpen();
                }
            });

            Session.getCurrentSession().removeCallback(this);
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if (exception != null) {
                Log.v(LOG_TAG, "kakao : onSessionOpenFailed" + exception.toString());
            }
            callbackContext.error(exception.getMessage());

            Session.getCurrentSession().removeCallback(this);
        }
    }

    /**
     * Class KakaoSDKAdapter
     */
    private static class KakaoSDKAdapter extends KakaoAdapter {
        private Context context;

        public KakaoSDKAdapter(Context context) {
            this.context = context;
        }

        @Override
        public ISessionConfig getSessionConfig() {
            return new ISessionConfig() {
                @Override
                public AuthType[] getAuthTypes() {
                    return new AuthType[]{AuthType.KAKAO_LOGIN_ALL};
                }

                @Override
                public boolean isUsingWebviewTimer() {
                    return false;
                }

                @Override
                public boolean isSecureMode() {
                    return false;
                }

                @Override
                public ApprovalType getApprovalType() {
                    return ApprovalType.INDIVIDUAL;
                }

                @Override
                public boolean isSaveFormData() {
                    return true;
                }
            };
        }

        @Override
        public IApplicationConfig getApplicationConfig() {
            return new IApplicationConfig() {
                @Override
                public Context getApplicationContext() {
                    return context;
                }
            };
        }
    }

}
