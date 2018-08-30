package com.htj.plugin.kakao;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
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

import com.kakao.auth.AuthType;
import com.kakao.auth.KakaoSDK;
import com.kakao.auth.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * copy from KakaoTalk LoginButton
 */
public class KakaoLoginSelectDialog {
  private Context context;
  private KOAuthSelectCallback callback;

  public KakaoLoginSelectDialog(Context context, KOAuthSelectCallback callback) {
    this.context = context;
    this.callback = callback;
  }

  public void open() {
    final List<AuthType> authTypes = getAuthTypes();
    if (authTypes.size() == 1) {
      this.callback.onSelect(authTypes.get(0));
    } else {
      final KakaoLoginSelectDialog.Item[] authItems = createAuthItemArray(authTypes);
      ListAdapter adapter = createLoginAdapter(authItems, context);
      final Dialog dialog = createLoginDialog(authItems, adapter, context);
      dialog.show();
    }
  }

  private List<AuthType> getAuthTypes() {
    final List<AuthType> availableAuthTypes = new ArrayList<AuthType>();

    if (Session.getCurrentSession().getAuthCodeManager().isTalkLoginAvailable()) {
      availableAuthTypes.add(AuthType.KAKAO_TALK);
      availableAuthTypes.add(AuthType.KAKAO_TALK_ONLY);
    }
    if (Session.getCurrentSession().getAuthCodeManager().isStoryLoginAvailable()) {
      availableAuthTypes.add(AuthType.KAKAO_STORY);
    }
    availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);

    AuthType[] authTypes = KakaoSDK.getAdapter().getSessionConfig().getAuthTypes();
    if (authTypes == null || authTypes.length == 0 || (authTypes.length == 1 && authTypes[0] == AuthType.KAKAO_LOGIN_ALL)) {
      authTypes = AuthType.values();
    }
    availableAuthTypes.retainAll(Arrays.asList(authTypes));

    if (availableAuthTypes.contains(AuthType.KAKAO_TALK) && availableAuthTypes.contains(AuthType.KAKAO_TALK_ONLY)) {
      availableAuthTypes.remove(AuthType.KAKAO_TALK_ONLY);
    }
    // 개발자가 설정한 것과 available 한 타입이 없다면 직접계정 입력이 뜨도록 한다.
    if(availableAuthTypes.size() == 0){
      availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);
    }
    return availableAuthTypes;
  }
  /**
   * 가능한 AuhType들이 담겨 있는 리스트를 인자로 받아 로그인 어댑터의 data source로 사용될 Item array를 반환한다.
   * @param authTypes 가능한 AuthType들을 담고 있는 리스트
   * @return 실제로 로그인 방법 리스트에 사용될 Item array
   */
  private KakaoLoginSelectDialog.Item[] createAuthItemArray(final List<AuthType> authTypes) {
    final List<KakaoLoginSelectDialog.Item> itemList = new ArrayList<KakaoLoginSelectDialog.Item>();
    if(authTypes.contains(AuthType.KAKAO_TALK)) {
      itemList.add(new KakaoLoginSelectDialog.Item(com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account, com.kakao.usermgmt.R.drawable.talk, com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account_tts, AuthType.KAKAO_TALK));
    }
    if (authTypes.contains(AuthType.KAKAO_TALK_ONLY)) {
      itemList.add(new KakaoLoginSelectDialog.Item(com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account, com.kakao.usermgmt.R.drawable.talk, com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account_tts, AuthType.KAKAO_TALK_ONLY));
    }
    if(authTypes.contains(AuthType.KAKAO_STORY)) {
      itemList.add(new KakaoLoginSelectDialog.Item(com.kakao.usermgmt.R.string.com_kakao_kakaostory_account, com.kakao.usermgmt.R.drawable.story, com.kakao.usermgmt.R.string.com_kakao_kakaostory_account_tts, AuthType.KAKAO_STORY));
    }
    if(authTypes.contains(AuthType.KAKAO_ACCOUNT)){
      itemList.add(new KakaoLoginSelectDialog.Item(com.kakao.usermgmt.R.string.com_kakao_other_kakaoaccount, com.kakao.usermgmt.R.drawable.account, com.kakao.usermgmt.R.string.com_kakao_other_kakaoaccount_tts, AuthType.KAKAO_ACCOUNT));
    }

    return itemList.toArray(new KakaoLoginSelectDialog.Item[0]);
  }

  @SuppressWarnings("deprecation")
  private ListAdapter createLoginAdapter(final KakaoLoginSelectDialog.Item[] authItems, final Context context) {
        /*
          가능한 auth type들을 유저에게 보여주기 위한 준비.
         */
    return new ArrayAdapter<KakaoLoginSelectDialog.Item>(
      context,
      android.R.layout.select_dialog_item,
      android.R.id.text1, authItems){
      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
          LayoutInflater inflater = (LayoutInflater) getContext()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          convertView = inflater.inflate(com.kakao.usermgmt.R.layout.layout_login_item, parent, false);
        }
        ImageView imageView = convertView.findViewById(com.kakao.usermgmt.R.id.login_method_icon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          imageView.setImageDrawable(context.getResources().getDrawable(authItems[position].icon, getContext().getTheme()));
        } else {
          imageView.setImageDrawable(context.getResources().getDrawable(authItems[position].icon));
        }
        TextView textView = convertView.findViewById(com.kakao.usermgmt.R.id.login_method_text);
        textView.setText(authItems[position].textId);
        return convertView;
      }
    };
  }

  /**
   * 실제로 유저에게 보여질 dialog 객체를 생성한다.
   * @param authItems 가능한 AuthType들의 정보를 담고 있는 Item array
   * @param adapter Dialog의 list view에 쓰일 adapter
   * @return 로그인 방법들을 팝업으로 보여줄 dialog
   */
  private Dialog createLoginDialog(final KakaoLoginSelectDialog.Item[] authItems, final ListAdapter adapter, final Context context) {
    final Dialog dialog = new Dialog(context, com.kakao.usermgmt.R.style.LoginDialog);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(com.kakao.usermgmt.R.layout.layout_login_dialog);
    if (dialog.getWindow() != null) {
      dialog.getWindow().setGravity(Gravity.CENTER);
    }

//        TextView textView = (TextView) dialog.findViewById(R.id.login_title_text);
//        Typeface customFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/KakaoOTFRegular.otf");
//        if (customFont != null) {
//            textView.setTypeface(customFont);
//        }

    ListView listView = dialog.findViewById(com.kakao.usermgmt.R.id.login_list_view);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final AuthType authType = authItems[position].authType;
        if (authType != null) {
          callback.onSelect(authType);
        } else {
          callback.onCancel();
        }
        dialog.dismiss();
      }
    });

    Button closeButton = dialog.findViewById(com.kakao.usermgmt.R.id.login_close_button);
    closeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.onCancel();
        dialog.dismiss();
      }
    });
    return dialog;
  }


  /**
   * 각 로그인 방법들의 text, icon, 실제 AuthTYpe들을 담고 있는 container class.
   */
  private static class Item {
    final int textId;
    public final int icon;
    final int contentDescId;
    final AuthType authType;
    Item(final int textId, final Integer icon, final int contentDescId, final AuthType authType) {
      this.textId = textId;
      this.icon = icon;
      this.contentDescId = contentDescId;
      this.authType = authType;
    }
  }
}

